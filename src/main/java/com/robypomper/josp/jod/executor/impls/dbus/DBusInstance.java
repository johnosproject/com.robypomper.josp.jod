package com.robypomper.josp.jod.executor.impls.dbus;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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


}
