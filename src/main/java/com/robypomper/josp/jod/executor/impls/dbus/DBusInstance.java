package com.robypomper.josp.jod.executor.impls.dbus;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.Variant;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * TODO remove System.out.println statement and replace with CustomException
 */
public class DBusInstance extends AbstractPropertiesChangedHandler implements AutoCloseable {

    public interface ListenerChanged {
        void onPropUpdated(String objPath, String iface, String propKey, Variant<?> proValue);
    }

    public interface ListenerRemoved {
        void onPropRemoved(String objPath, String iface, String propKey);
    }


    // Class vars
    private static DBusInstance instance = null;

    private final DBusConnection sessionConnection;

    private final AutoCloseable props_token;

    // obj_path > property > listeners
    private final Map<String, Map<String, Vector<ListenerChanged>>> listeners_changed = new HashMap<>();

    // obj_path > property > listeners
    private final Map<String, Map<String, Vector<ListenerRemoved>>> listeners_removed = new HashMap<>();

    //

    public static DBusInstance getInstance() throws DBusException {
        if (instance == null)
            instance = new DBusInstance();
        return instance;
    }

    // Constructor

    private DBusInstance() throws DBusException {
        sessionConnection = DBusConnectionBuilder.forSessionBus().build();
        props_token = sessionConnection.addSigHandler(Properties.PropertiesChanged.class, this);
    }


    // Autocloseable

    @Override
    public void close() throws Exception {
        props_token.close();
        sessionConnection.close();
    }


    // AbstractPropertiesChangedHandler

    @Override
    public void handle(Properties.PropertiesChanged propChanges) {
        String objPath = propChanges.getPath();
        String iface = propChanges.getInterfaceName();
        String obj_code = objPath + "::" + iface;

        if (listeners_changed.containsKey(obj_code)) {
            Map<String, Vector<ListenerChanged>> obj_listeners_changed = listeners_changed.get(obj_code);

            for (Map.Entry<String, Variant<?>> prop : propChanges.getPropertiesChanged().entrySet())
                if (obj_listeners_changed.containsKey(prop.getKey()))
                    for (ListenerChanged l : obj_listeners_changed.get(prop.getKey()))
                        l.onPropUpdated(objPath, iface, prop.getKey(), prop.getValue());
        }

        for (String p : propChanges.getPropertiesRemoved()) {
            Map<String, Vector<ListenerRemoved>> obj_listeners_removed = listeners_removed.get(obj_code);

            if (listeners_removed.containsKey(p))
                for (ListenerRemoved l : obj_listeners_removed.get(p))
                    l.onPropRemoved(objPath, iface, p);
        }
    }


    // Listener registers

    public void registerListenerChanged(String objPath, String iface, String property, ListenerChanged l) {
        String obj_code = objPath + "::" + iface;
        if (!listeners_changed.containsKey(obj_code))
            listeners_changed.put(obj_code, new HashMap<>());
        Map<String, Vector<ListenerChanged>> listeners_obj = listeners_changed.get(obj_code);

        if (!listeners_obj.containsKey(property))
            listeners_obj.put(property, new Vector<>());
        Vector<ListenerChanged> listeners_property = listeners_obj.get(property);

        listeners_property.add(l);
    }

    public void registerListenerRemoved(String objPath, String iface, String property, ListenerRemoved l) {
        String obj_code = objPath + "::" + iface;
        if (!listeners_removed.containsKey(obj_code))
            listeners_removed.put(obj_code, new HashMap<>());
        Map<String, Vector<ListenerRemoved>> listeners_obj = listeners_removed.get(obj_code);

        if (!listeners_obj.containsKey(property))
            listeners_obj.put(property, new Vector<>());
        Vector<ListenerRemoved> listeners_property = listeners_obj.get(property);

        listeners_property.add(l);
    }

    public void deregisterListenerChanged(String objPath, String iface, String property, ListenerChanged l) {
        String obj_code = objPath + "::" + iface;
        if (listeners_changed.containsKey(obj_code))
            if (listeners_changed.get(obj_code).containsKey(property))
                listeners_changed.get(obj_code).get(property).remove(l);
    }

    public void deregisterListenerRemoved(String objPath, String iface, String property, ListenerRemoved l) {
        String obj_code = objPath + "::" + iface;
        if (listeners_removed.containsKey(obj_code))
            if (listeners_removed.get(obj_code).containsKey(property))
                listeners_removed.get(obj_code).get(property).remove(l);
    }


    // Remote methods

    public Object invokeRemoteMethodCall(
            String dbus_name, String dbus_obj_path, String dbus_iface,
            String method_name, Object[] method_args, Class<?> method_return_type) {

        boolean noResponse = method_return_type == null || method_return_type == Void.class;

        MethodCall call;
        try {
            call = generateRemoteMethodCall(
                    dbus_name, dbus_obj_path, dbus_iface,
                    method_name, method_args);
        } catch (Throwable t) {
            System.err.printf("Can't prepare '%s' method call: %s%n", method_name, t);
            return null;
        }

        try {
            sessionConnection.sendMessage(call);
        } catch (Throwable t) {
            System.err.printf("Can't send '%s' method call: %s%n", method_name, t);
            return null;
        }

        Object res = null;
        if (noResponse) {
            //System.out.printf("Executed method '%s' with NO result%n", method_name);
            return null;
        }

        Message reply = call.getReply();
        if (null == reply) {
            System.err.printf("Timeout reached waiting for '%s' method response%n", method_name);
            return null;
        }
        if (reply instanceof Error) {
            DBusExecutionException e = ((Error) reply).getException();
            System.err.printf("Error waiting for '%s' method response: %s%n", method_name, e);
            return null;
        }
        try {
            res = convertRemoteMethodCallReturnValue(reply.getParameters(), method_return_type);
        } catch (DBusException e) {
            System.err.printf("Error parsing '%s' method response: %s%n", method_name, e);
        }

        //System.out.printf("Executed method '%s' with result '%s'%n", method_name, res);
        return res;
    }

    private MethodCall generateRemoteMethodCall(
            String dbus_name, String dbus_obj_path, String dbus_iface,
            String method_name, Object[] method_args) throws DBusException {

        Class<?>[] method_params_types = new Class[method_args.length];
        for (int i = 0; i < method_args.length; i++)
            method_params_types[i] = method_args[i].getClass();
        String sig = null;
        if (method_params_types.length > 0) {
            try {
                sig = Marshalling.getDBusType(method_params_types);
                method_args = Marshalling.convertParameters(method_args, method_params_types, sessionConnection);
            } catch (DBusException _ex) {
                throw new DBusExecutionException("Failed to construct D-Bus type: " + _ex.getMessage());
            }
        }
        return new MethodCall(dbus_name, dbus_obj_path, dbus_iface, method_name,
                Message.Flags.NO_AUTO_START, sig, method_args);
    }

    public Object convertRemoteMethodCallReturnValue(Object[] _rp, Class<?> retType) throws DBusException {
        Object[] rp = _rp;
        if (rp == null) {
            if (null == retType || Void.TYPE.equals(retType)) {
                return null;
            } else {
                throw new DBusException("Wrong return type (got void, expected a value)");
            }
        } else {
            try {
                rp = Marshalling.deSerializeParameters(rp, new Type[]{
                        retType
                }, sessionConnection);
            } catch (Exception _ex) {
                throw new DBusException(String.format("Wrong return type (failed to de-serialize correct types: %s )", _ex.getMessage()));
            }
        }

        switch (rp.length) {
            case 0:
                if (null == retType || Void.TYPE.equals(retType)) {
                    return null;
                } else {
                    throw new DBusException("Wrong return type (got void, expected a value)");
                }
            case 1:
                return rp[0];
            default:

                // check we are meant to return multiple values
                if (!Tuple.class.isAssignableFrom(retType)) {
                    throw new DBusException("Wrong return type (not expecting Tuple)");
                }

                Constructor<?> cons = retType.getConstructors()[0];
                try {
                    return cons.newInstance(rp);
                } catch (Exception _ex) {
                    throw new DBusException(_ex.getMessage());
                }
        }
    }

}
