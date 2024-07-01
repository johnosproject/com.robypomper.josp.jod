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

import com.robypomper.josp.jod.executor.AbsJODExecutor;
import com.robypomper.josp.jod.executor.Substitutions;
import com.robypomper.josp.jod.structure.JODComponent;
import com.robypomper.josp.jod.structure.JODState;
import com.robypomper.josp.jod.structure.pillars.JODBooleanAction;
import com.robypomper.josp.jod.structure.pillars.JODRangeAction;
import com.robypomper.josp.protocol.JOSPProtocol;
import org.freedesktop.dbus.exceptions.DBusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Vector;


/**
 * JOD Executor DBus.
 */
public class ExecutorDBus extends AbsJODExecutor implements JODBooleanAction.JOSPBoolean.Executor, JODRangeAction.JOSPRange.Executor {

    // Class constants

    private static final String PROP_DBUS_NAME = "dbus_name";
    private static final String PROP_DBUS_OBJ_PATH = "dbus_obj_path";
    private static final String DEF_PROP_DBUS_OBJ_PATH = "/";
    private static final String PROP_DBUS_IFACE = "dbus_iface";
    private static final String PROP_DBUS_METHOD = "dbus_method";
    private static final String PROP_DBUS_METHOD_PARAMS = "dbus_method_params";
    private static final String DEF_PROP_DBUS_METHOD_PARAMS = "";

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(ExecutorDBus.class);
    protected final DBusInstance dbus_instance;
    protected final String dbus_name;
    protected final String dbus_obj_path;
    protected final String dbus_iface;
    protected final String dbus_method;
    protected String dbus_method_params;


    // Constructor

    /**
     * {@inheritDoc}
     *
     * @param name
     * @param proto
     * @param component
     */
    public ExecutorDBus(String name, String proto, String configsStr, JODComponent component) throws MissingPropertyException, FactoryException {
        super(name, proto, component);
        log.trace(String.format("ExecutorDBus for component '%s' init with config string '%s://%s'", getName(), proto, configsStr));

        try {
            dbus_instance = DBusInstance.getInstance();
        } catch (DBusException e) {
            throw new FactoryException(String.format("DBus not available, disabled listener '%s'.", name), e);
        }

        Map<String, String> configs = splitConfigsStrings(configsStr);
        dbus_name = parseConfigString(configs, PROP_DBUS_NAME);
        dbus_obj_path = parseConfigString(configs, PROP_DBUS_OBJ_PATH, DEF_PROP_DBUS_OBJ_PATH);
        dbus_iface = parseConfigString(configs, PROP_DBUS_IFACE);
        dbus_method = parseConfigString(configs, PROP_DBUS_METHOD);
        dbus_method_params = parseConfigString(configs, PROP_DBUS_METHOD_PARAMS, DEF_PROP_DBUS_METHOD_PARAMS);
        log.trace(String.format("ExecutorDBus for component '%s' listen for changes on '%s/%s' bus/property.", getName(), dbus_name, dbus_method));
    }


    // Mngm

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exec(JOSPProtocol.ActionCmd commandAction, JODBooleanAction.JOSPBoolean cmdAction) {
        log.debug(String.format("Executing '%s' executor", getName()));
        if (!isEnabled()) {
            log.warn(String.format("Error on exec '%s' executor because disabled", getName()));
            return false;
        }

        // Do something...
        String method_args_as_str = new Substitutions(dbus_method_params)
                //.substituteObject(jod.getObjectInfo())
                //.substituteObjectConfigs(jod.getObjectInfo())
                .substituteComponent(getComponent())
                .substituteState((JODState) getComponent())
                .substituteAction(commandAction)
                .toString();
        Object[] method_args = argsStr2ArgsObj(method_args_as_str);
        Object res = dbus_instance.invokeRemoteMethodCall(dbus_name, dbus_obj_path, dbus_iface,
                dbus_method, method_args, null);

        Vector<String> method_args_as_str_generated = new Vector<>();
        for (Object o : method_args)
            method_args_as_str_generated.add(String.format("%s:%s", o.toString(), o.getClass().getSimpleName()));
        log.debug(String.format("Executor '%s' executed method '%s(%s)' => '%s'",
                getName(), dbus_method, String.join(",", method_args_as_str_generated), res));

        ((JODState) getComponent()).forceCheckState();

        return true;
    }

    private Object[] argsStr2ArgsObj(String methodArgsAsStr) {
        String[] argsStr = methodArgsAsStr.split(",");
        Object[] argsObj = new Object[argsStr.length];
        for (int i = 0; i < argsStr.length; i++) {
            String argFull = argsStr[i];
            try {
                String[] argStrSplit = argFull.split(":");
                String argValue = argStrSplit[0].trim();
                String argType = argStrSplit[1].trim();
                argsObj[i] = generateArgObj(argValue, argType);
            } catch (Exception ignore) {
                log.warn(String.format("ExecutorDBus '%s' error on converting method parameter '%s'. Use his String as a value.", getName(), argFull));
                argsObj[i] = argFull;
            }
        }
        return argsObj;
    }

    private Object generateArgObj(String argValue, String argType) {
        String[] packages = new String[]{
                "",
                "java.lang"
        };

        Class<?> c = null;
        int i = 0;
        while (i < packages.length && c == null) {
            String fullName = (!packages[i].isEmpty() ? packages[i] + "." : "") + argType;
            try {
                c = Class.forName(fullName);
            } catch (ClassNotFoundException ignored) {}
            i++;
        }
        if (c==null) {
            log.warn(String.format("ExecutorDBus '%s' error on converting method parameter, no type '%s' found for '%s' value. Use his value as String.", getName(), argType, argValue));
            return argValue;
        }

        try {
            Constructor<?> cons = c.getConstructor(String.class);
            return c.cast(cons.newInstance(argValue));
        } catch (NoSuchMethodException e) {
            log.warn(String.format("ExecutorDBus '%s' error on converting method parameter, no '%s(String)' constructor found for '%s' value. Use his value as String.", getName(), argType, argValue));
            return argValue;
        } catch (InvocationTargetException|InstantiationException|IllegalAccessException e) {
            log.warn(String.format("ExecutorDBus '%s' error on converting method parameter, error on '%s(String)' constructor found for '%s' value. Use his value as String.", getName(), argType, argValue));
            return argValue;
        }
    }


    @Override
    public boolean exec(JOSPProtocol.ActionCmd commandAction, JODRangeAction.JOSPRange cmdAction) {
        String method_args_as_str = new Substitutions(dbus_method_params)
                //.substituteObject(jod.getObjectInfo())
                //.substituteObjectConfigs(jod.getObjectInfo())
                .substituteComponent(getComponent())
                .substituteState((JODState) getComponent())
                .substituteAction(commandAction)
                .toString();
        Object[] method_args = new Object[0];  // <= method_args_as_str
        Object res = dbus_instance.invokeRemoteMethodCall(dbus_name, dbus_obj_path, dbus_iface,
                dbus_method, method_args, null);


        log.debug(String.format("Executor '%s' executed method '%s(%s)' => '%s'",
                getName(), dbus_method, method_args_as_str, res));

        ((JODState) getComponent()).forceCheckState();

        return true;
    }

}
