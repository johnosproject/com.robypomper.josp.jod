/*******************************************************************************
 * The John Object Daemon is the agent software to connect "objects"
 * to an IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2024 Roberto Pompermaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.robypomper.josp.jod.executor.impls.dbus;

import com.robypomper.josp.jod.executor.AbsJODListener;
import com.robypomper.josp.jod.executor.impls.http.EvaluatorInternal;
import com.robypomper.josp.jod.executor.impls.http.FormatterInternal;
import com.robypomper.josp.jod.structure.JODComponent;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;


/**
 * JOD Puller test.
 */
public class ListenerDBus extends AbsJODListener implements DBusInstance.ListenerChanged {

    // Class constants

    private static final String PROP_DBUS_NAME = "dbus_name";
    private static final String PROP_DBUS_OBJ_PATH = "dbus_obj_path";
    private static final String DEF_PROP_DBUS_OBJ_PATH = "/";
    private static final String PROP_DBUS_IFACE = "dbus_iface";
    private static final String PROP_DBUS_PROP = "dbus_prop";
    private static final String PROP_INIT_DATA = "init_data";
    private static final String DEF_PROP_INIT_DATA = "0";

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(ListenerDBus.class);
    private final FormatterInternal formatter;
    private final EvaluatorInternal evaluator;
    protected final DBusInstance dbus_instance;
    protected boolean isListening = false;
    protected final String dbus_name;
    protected final String dbus_obj_path;
    protected final String dbus_iface;
    protected final String dbus_prop;
    protected String value;


    // Constructor

    /**
     * {@inheritDoc}
     *
     * @param name
     * @param proto
     * @param component
     */
    public ListenerDBus(String name, String proto, String configsStr, JODComponent component) throws MissingPropertyException, FactoryException, ParsingPropertyException {
        super(name, proto, component);
        log.trace(String.format("ListenerDBus for component '%s' init with config string '%s://%s'", getName(), proto, configsStr));

        try {
            dbus_instance = DBusInstance.getInstance();
        } catch (DBusException e) {
            throw new FactoryException(String.format("DBus not available, disabled listener '%s'.", name), e);
        }

        formatter = new FormatterInternal(this, name, proto, configsStr, component);
        evaluator = new EvaluatorInternal(this, name, proto, configsStr, component);
        Map<String, String> configs = splitConfigsStrings(configsStr);
        dbus_name = parseConfigString(configs, PROP_DBUS_NAME);
        dbus_obj_path = parseConfigString(configs, PROP_DBUS_OBJ_PATH, DEF_PROP_DBUS_OBJ_PATH);
        dbus_iface = parseConfigString(configs, PROP_DBUS_IFACE);
        dbus_prop = parseConfigString(configs, PROP_DBUS_PROP);
        value = parseConfigString(configs, PROP_INIT_DATA, DEF_PROP_INIT_DATA);
        log.trace(String.format("ListenerDBus for component '%s' listen for changes on '%s/%s' bus/property.", getName(), dbus_name, dbus_prop));
    }


    // Getters

    @Override
    public boolean isEnabled() {
        return isListening;
    }


    // Mngm

    @Override
    public void listen() {
        log.debug(String.format("                                   Start '%s' listener", getName()));
        dbus_instance.registerListenerChanged(dbus_obj_path, dbus_iface, dbus_prop, this);
        isListening = true;
    }

    @Override
    public void halt() {
        log.info(String.format("Stop '%s' listener server", getName()));
        dbus_instance.deregisterListenerChanged(dbus_obj_path, dbus_iface, dbus_prop, this);
        isListening = false;
    }


    // DBus' property listener

    @Override
    public void onPropUpdated(String objPath, String iface, String propKey, Variant<?> propValue) {
        String newStatus = propValue.getValue().toString();

        String result;
        try {
            result = formatter.parse(newStatus);
        } catch (FormatterInternal.ParsingException | FormatterInternal.PathNotFoundException e) {
            log.warn(String.format("ListenerDBus for component '%s' of proto '%s' error on parsing received property '%s''s update '%s' because '%s'", getName(), getProto(), dbus_prop, newStatus, e.getMessage()), e);
            log.debug(String.format("ListenerDBus for component '%s' of proto '%s' error on parsing received property '%s''s update", getName(), getProto(), dbus_prop));
            return;
        }

        log.debug(String.format("ListenerDBus for component '%s' of proto '%s' read state '%s'", getName(), getProto(), result));

        if (value.compareTo(result) == 0){
            log.debug(String.format("ListenerDBus for component '%s' of proto '%s' get same result as last attempt, skip it", getName(), getProto()));
            return;
        }
        value = result;

        String resultEvaluated;
        try {
            resultEvaluated = evaluator.evaluate(result);
        } catch (EvaluatorInternal.EvaluationException e) {
            log.warn(String.format("ListenerDBus for component '%s' of proto '%s' error on evaluating property '%s''s value '%s' because '%s'", getName(), getProto(), dbus_prop, result, e.getMessage()), e);
            log.debug(String.format("ListenerDBus for component '%s' of proto '%s' error on evaluating value '%s'", getName(), getProto(), result));
            return;
        }

log.info(String.format("ListenerDBus for component '%s' of proto '%s' monitored '%s' property and get '%s' value from '%s' result", getName(), getProto(), dbus_prop, resultEvaluated, result));
        log.debug(String.format("ListenerDBus for component '%s' of proto '%s' monitored '%s' property and get '%s' value from '%s' result", getName(), getProto(), dbus_prop, resultEvaluated, result));

        if (!convertAndSetStatus(resultEvaluated))
            log.warn(String.format("ListenerDBus for component '%s' can't update his component because not supported (%s)", getName(), getComponent().getClass().getSimpleName()));
    }

}
