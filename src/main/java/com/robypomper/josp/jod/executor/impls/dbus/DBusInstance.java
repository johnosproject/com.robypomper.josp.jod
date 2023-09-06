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
        void onPropUpdated(String infName, String objPath, String propKey, Variant<?> proValue);
    }

    public interface ListenerRemoved {
        void onPropRemoved(String infName, String objPath, String propKey);
    }


    // Class vars
    private static DBusInstance instance = null;

    private final DBusConnection sessionConnection;

    private final AutoCloseable props_token;

    private final Map<String, Vector<ListenerChanged>> listeners_changed = new HashMap<>();

    private final Map<String, Vector<ListenerRemoved>> listeners_removed = new HashMap<>();


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
        String interfaceName = propChanges.getInterfaceName();
        String objPath = propChanges.getPath();

        for (Map.Entry<String, Variant<?>> entry : propChanges.getPropertiesChanged().entrySet())
            if (listeners_changed.containsKey(entry.getKey()))
                for (ListenerChanged l : listeners_changed.get(entry.getKey()))
                    l.onPropUpdated(interfaceName, objPath, entry.getKey(), entry.getValue());

        for (String p : propChanges.getPropertiesRemoved())
            if (listeners_removed.containsKey(p))
                for (ListenerRemoved l : listeners_removed.get(p))
                    l.onPropRemoved(interfaceName, objPath, p);
    }


    // Listener registers

    public void registerListenerChanged(String property, ListenerChanged l) {
        if (!listeners_changed.containsKey(property))
            listeners_changed.put(property, new Vector<>());
        listeners_changed.get(property).add(l);
    }

    public void registerListenerRemoved(String property, ListenerRemoved l) {
        if (!listeners_removed.containsKey(property))
            listeners_removed.put(property, new Vector<>());
        listeners_removed.get(property).add(l);
    }

    public void deregisterListenerChanged(String property, ListenerChanged l) {
        if (listeners_changed.containsKey(property))
            listeners_changed.get(property).remove(l);
    }

    public void deregisterListenerRemoved(String property, ListenerRemoved l) {
        if (listeners_removed.containsKey(property))
            listeners_removed.get(property).remove(l);
    }


}
