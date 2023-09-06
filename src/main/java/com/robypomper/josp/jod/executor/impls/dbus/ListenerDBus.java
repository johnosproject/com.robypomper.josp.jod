/*******************************************************************************
 * The John Object Daemon is the agent software to connect "objects"
 * to an IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2021 Roberto Pompermaier
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
import com.robypomper.josp.jod.structure.JODComponent;
import com.robypomper.log.Mrk_JOD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;


/**
 * JOD Puller test.
 */
public class ListenerDBus extends AbsJODListener implements DBusInstance.ListenerChanged {

    // Class constants

    private static final String PROP_DBUS_NAME = "dbus_name";
    private static final String PROP_DBUS_OBJ_PATH = "dbus_obj_path";
    private static final String DEF_PROP_DBUS_OBJ_PATH = "";
    private static final String PROP_DBUS_PROP = "dbus_prop";
    private static final String PROP_INIT_DATA = "init_data";
    private static final String DEF_PROP_INIT_DATA = "0";

    // Internal vars

    protected static final Logger log = LogManager.getLogger();
    protected final DBusInstance dbus_instance;
    protected boolean isListening = false;
    protected final String dbus_name;
    protected final String dbus_obj_path;
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
    public ListenerDBus(String name, String proto, String configsStr, JODComponent component) throws MissingPropertyException, FactoryException {
        super(name, proto, component);
        log.trace(Mrk_JOD.JOD_EXEC_IMPL, String.format("ListenerDBus for component '%s' init with config string '%s://%s'", getName(), proto, configsStr));

        try {
            dbus_instance = DBusInstance.getInstance();
        } catch (DBusException e) {
            throw new FactoryException(String.format("DBus not available, disabled listener '%s'.", name), e);
        }

        Map<String, String> configs = splitConfigsStrings(configsStr);
        dbus_name = parseConfigString(configs, PROP_DBUS_NAME);
        dbus_obj_path = parseConfigString(configs, PROP_DBUS_OBJ_PATH, DEF_PROP_DBUS_OBJ_PATH);
        dbus_prop = parseConfigString(configs, PROP_DBUS_PROP);
        value = parseConfigString(configs, PROP_INIT_DATA, DEF_PROP_INIT_DATA);
        log.trace(Mrk_JOD.JOD_EXEC_IMPL, String.format("ListenerDBus for component '%s' listen for changes on '%s/%s' bus/property.", getName(), dbus_name, dbus_prop));
    }


    // Getters

    @Override
    public boolean isEnabled() {
        return isListening;
    }


    // Mngm

    @Override
    public void listen() {
        log.info(Mrk_JOD.JOD_EXEC_SUB, String.format("Start '%s' listener", getName()));
        dbus_instance.registerListenerChanged(dbus_prop, this);
        isListening = true;
    }

    @Override
    public void halt() {
        log.info(Mrk_JOD.JOD_EXEC_SUB, String.format("Stop '%s' listener server", getName()));
        dbus_instance.deregisterListenerChanged(dbus_prop, this);
        isListening = false;
    }


    // DBus' property listener

    @Override
    public void onPropUpdated(String objPath, String iface, String propKey, Variant<?> propValue) {
        if (dbus_prop.compareToIgnoreCase(propKey) != 0)
            return;

        String newStatus = propValue.getValue().toString();
        if (!convertAndSetStatus(newStatus))
            log.warn(Mrk_JOD.JOD_EXEC_IMPL, String.format("ListenerFiles for component '%s' can't update his component because not supported (%s)", getName(), getComponent().getClass().getSimpleName()));
    }

}
