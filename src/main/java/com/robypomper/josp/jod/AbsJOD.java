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

package com.robypomper.josp.jod;

import com.robypomper.comm.exception.PeerConnectionException;
import com.robypomper.comm.exception.PeerDisconnectionException;
import com.robypomper.java.JavaAssertions;
import com.robypomper.java.JavaEnum;
import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.jod.comm.JODCommunication;
import com.robypomper.josp.jod.comm.JODLocalClientInfo;
import com.robypomper.josp.jod.events.Events;
import com.robypomper.josp.jod.events.JODEvents;
import com.robypomper.josp.jod.executor.JODExecutorMngr;
import com.robypomper.josp.jod.history.JODHistory;
import com.robypomper.josp.jod.objinfo.JODObjectInfo;
import com.robypomper.josp.jod.permissions.JODPermissions;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.states.JODState;
import com.robypomper.josp.states.StateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * Default {@link JOD} implementation, initialization excluded.
 * <p>
 * This class fully manage a JOD object (start,stop,status...) for all JOD systems.
 * But JOD systems initialization is delegate to his sub-classes. That allow to
 * initialize multiple JOD objects using different systems implementations.
 * Helping provide new JOD versions and flavours.
 * <p>
 * The JOD and {@link AbsJOD} hierarchy is design to allow sub-classes to initialize
 * JOD systems (structure, comm, executor...) and delegate JOD systems orchestration
 * to AbsJOD class. AbsJOD class manage JOD system using only their interfaces,
 * that make system implementation completely interoperable (at JOD level). So
 * AbsJOD sub-classes (like {@link JOD_002} can switch to different systems
 * implementations/versions keeping full compatibility with all others JOD
 * systems.
 * <p>
 * All AbsJOD sub-classes must implement the <code>instance(...)</code> method
 * and return a self instance. <code>instance(...)</code> method can be
 * implemented using {@link JOD.Settings} param or his sub-class.
 * Returned class from {@link FactoryJOD#getJODClass(String)} must implement a
 * <code>instance(...)</code> method with a param corresponding to class returned
 * by {@link FactoryJOD#getJODSettingsClass(String)}. Both method are called using
 * same String param corresponding to JOD version
 */
@SuppressWarnings("JavadocReference")
public abstract class AbsJOD implements JOD {

    // Private systems references

    private final JOD.Settings settings;
    private final JCPAPIsClientObj jcpClient;
    private final JODObjectInfo objInfo;
    private final JODStructure structure;
    private final JODCommunication comm;
    private final JODExecutorMngr executor;
    private final JODPermissions permissions;
    private final JODEvents events;
    private final JODHistory history;


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(AbsJOD.class);
    private final JavaEnum.Synchronizable<JODState> state = new JavaEnum.Synchronizable<>(JODState.STOP);


    // Constructor

    /**
     * Default constructor, set all private systems references.
     *
     * @param settings    instance containing current JOD configs.
     * @param jcpClient   instance of JCP client for objects.
     * @param objInfo     {@link JODObjectInfo} reference.
     * @param structure   {@link JODStructure} reference.
     * @param comm        {@link JODCommunication} reference.
     * @param executor    {@link JODExecutorMngr} reference.
     * @param permissions {@link JODPermissions} reference.
     * @param events      {@link JODEvents} reference.
     */
    protected AbsJOD(Settings settings,
                     JCPAPIsClientObj jcpClient,
                     JODObjectInfo objInfo,
                     JODStructure structure,
                     JODCommunication comm,
                     JODExecutorMngr executor,
                     JODPermissions permissions,
                     JODEvents events,
                     JODHistory history) {
        this.settings = settings;
        this.jcpClient = jcpClient;
        this.objInfo = objInfo;
        this.structure = structure;
        this.comm = comm;
        this.executor = executor;
        this.permissions = permissions;
        this.events = events;
        this.history = history;

        log.info(String.format("Initialized AbsJOD/%s instance for '%s' ('%s') object", this.getClass().getSimpleName(), objInfo.getObjName(), objInfo.getObjId()));
    }


    // JOD mngm

    /**
     * {@inheritDoc}
     */
    @Override
    public JODState getState() {
        return state.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Activate autoRefresh from {@link JODObjectInfo}, {@link JODPermissions}
     * and {@link JODStructure} systems. Then activate the firmware's
     * interfaces {@link com.robypomper.josp.jod.executor.JODPuller},
     * {@link com.robypomper.josp.jod.executor.JODListener} and
     * {@link com.robypomper.josp.jod.executor.JODExecutor}. Finally it start
     * local and cloud communication.
     *
     * @throws StateException throw if current JOD object is already running.
     */
    @Override
    public void startup() throws StateException {
        log.info(String.format("Start JOD instance for '%s' object", objInfo.getObjId()));
        log.trace(String.format("JOD state is %s", getState()));

        if (state.enumEquals(JODState.RUN))
            return; // Already done

        else if (state.enumEquals(JODState.STARTING))
            return; // Already in progress

        else if (state.enumEquals(JODState.RESTARTING))
            throw new StateException("Can't startup JOD daemon instance because is restarting, try again later");

        else if (state.enumEquals(JODState.STOP))
            startupInstance();

        else if (state.enumEquals(JODState.SHOUTING)) {
            throw new StateException("Can't startup JOD daemon instance because is shutting down, try again later");
        }

        log.trace(String.format("JOD state is %s", getState()));

    }

    /**
     * {@inheritDoc}
     * <p>
     * Stop local and cloud communication. Then deactivate the firmware's
     * interfaces {@link com.robypomper.josp.jod.executor.JODPuller},
     * {@link com.robypomper.josp.jod.executor.JODListener} and
     * {@link com.robypomper.josp.jod.executor.JODExecutor}. Finally deactivate
     * autoRefresh from {@link JODObjectInfo}, {@link JODPermissions}
     * and {@link JODStructure} systems
     *
     * @throws StateException throw if current JOD object is already stopped.
     */
    @Override
    public void shutdown() throws StateException {
        log.info(String.format("Shutting down JOD instance for '%s' object", objInfo.getObjId()));
        log.trace(String.format("JOD state is %s", getState()));

        if (state.enumEquals(JODState.RUN))
            try {
                shutdownInstance();
            } catch (Throwable e) {
                JavaAssertions.makeWarning_Failed(e, "Error on shutdown JOD instance");
            }

        else if (state.enumEquals(JODState.STARTING))
            throw new StateException("Can't shout down JOD daemon instance because is starting, try again later");

        else if (state.enumEquals(JODState.RESTARTING))
            throw new StateException("Can't shout down JOD daemon instance because is restarting, try again later");

        else if (state.enumEquals(JODState.STOP))
            return; // Already done

        else if (state.enumEquals(JODState.SHOUTING))
            return; // Already in progress

        log.trace(String.format("JOD state is %s", getState()));
    }

    /**
     * {@inheritDoc}
     *
     * @throws StateException thrown if errors occurs on JOD object stop and start.
     */
    @Override
    public boolean restart() throws StateException {
        log.info(String.format("Shutting down JOD instance for '%s' object", objInfo.getObjId()));
        log.trace(String.format("JOD state is %s", getState()));

        if (state.enumEquals(JODState.RUN))
            restartInstance();

        else if (state.enumEquals(JODState.STARTING))
            throw new StateException("Can't restart JOD daemon instance because is starting, try again later");

        else if (state.enumEquals(JODState.RESTARTING))
            return true; // Already in progress

        else if (state.enumEquals(JODState.STOP))
            restartInstance();

        else if (state.enumEquals(JODState.SHOUTING))
            throw new StateException("Can't restart JOD daemon instance because is shutting down, try again later");

        log.trace(String.format("JOD state is %s", getState()));
        return state.enumEquals(JODState.STARTING);
    }

    @Override
    public void printInstanceInfo() {
        //log.info(String.format("JOD Object '%s' started", objInfo.getObjId()));
        //log.info(String.format("    JOD Obj status           = %s", status()));
        //log.info(String.format("    JOSP JOD version         = %s", version()));
        //log.info(String.format("    JOSP protocol supported  = %s", Arrays.asList(versionsJOSPProtocol())));
        //log.info(String.format("    JCP APIs supported       = %s", Arrays.asList(versionsJCPAPIs())));
        //log.info(String.format("    Cloud comm.              = %s", comm.isCloudConnected()));
        //log.info(String.format("    Local comm.              = %s", comm.isLocalRunning()));
        //log.info(String.format("    JOD Obj id               = %s", objInfo.getObjId()));
        //log.info(String.format("    JOD Obj name             = %s", objInfo.getObjName()));
        //log.info(String.format("    JOD Obj owner id         = %s", objInfo.getOwnerId()));

        log.debug("                                   JOD Obj");
        log.debug("                                   +  -- IDs");
        log.debug("                                   +         JOD Obj");
        log.debug(String.format("                                   +             ID                = %s", objInfo.getObjId()));
        log.debug(String.format("                                   +             name              = %s", objInfo.getObjName()));
        log.debug(String.format("                                   +             brand             = %s", objInfo.getBrand()));
        log.debug(String.format("                                   +             model             = %s", objInfo.getModel()));
        log.debug("                                   +         Owner");
        log.debug(String.format("                                   +             ID                = %s", objInfo.getOwnerId()));
        log.debug("                                   +  -- Ver.s");
        log.debug(String.format("                                   +     JOD Srv state             = %s", getState()));
        log.debug(String.format("                                   +     JOD Srv version           = %s", version()));
        log.debug(String.format("                                   +     JOSP protocol supported   = %s", Arrays.asList(versionsJOSPProtocol())));
        log.debug(String.format("                                   +     JCP APIs supported        = %s", Arrays.asList(versionsJCPAPIs())));
        log.debug("                                   +  -- Comm.s");
        log.debug("                                   +         JCP APIs");
        log.debug(String.format("                                   +             State             = %s", comm.getCloudAPIs().getState()));
        log.debug(String.format("                                   +             Url               = %s", comm.getCloudAPIs().getAPIsUrl()));
        log.debug(String.format("                                   +             HostName          = %s", comm.getCloudAPIs().getAPIsHostname()));
        log.debug(String.format("                                   +             IsConnected       = %s", comm.getCloudAPIs().isConnected()));
        log.debug(String.format("                                   +             IsAuth            = %s", comm.getCloudAPIs().isUserAuthenticated()));
        log.debug(String.format("                                   +             LastConn          = %s", comm.getCloudAPIs().getLastConnection()));
        log.debug(String.format("                                   +             LastDiscon        = %s", comm.getCloudAPIs().getLastDisconnection()));
        log.debug("                                   +         Cloud Comm.");
        log.debug(String.format("                                   +             State (Client)    = %s", comm.getCloudConnection().getState()));
        InetAddress cloudAddr = comm.getCloudConnection().getConnectionInfo().getRemoteInfo().getAddr();
        Integer cloudPort = comm.getCloudConnection().getConnectionInfo().getRemoteInfo().getPort();
        log.debug(String.format("                                   +             HostName          = %s", (cloudAddr != null ? cloudAddr.getHostName() : "N/A")));
        log.debug(String.format("                                   +             IPAddr            = %s", (cloudAddr != null ? cloudAddr.getHostAddress() : "N/A")));
        log.debug(String.format("                                   +             Port              = %s", (cloudPort != null ? cloudPort : "N/A")));
        log.debug(String.format("                                   +             IsConnected       = %s", comm.getCloudConnection().getState().isConnected()));
        log.debug(String.format("                                   +             LastConn          = %s", comm.getCloudConnection().getConnectionInfo().getStats().getLastConnection()));
        log.debug(String.format("                                   +             LastDiscon        = %s", comm.getCloudConnection().getConnectionInfo().getStats().getLastDisconnection()));
        log.debug(String.format("                                   +             LastDisconReason  = %s", comm.getCloudConnection().getDisconnectionReason()));
        log.debug("                                   +         Local Comm.");
        if (comm.getLocalServer() != null) {
            log.debug(String.format("                                   +             State (Server)    = %s", comm.getLocalServer().getState()));
            log.debug(String.format("                                   +             ClientsCount      = %s", comm.getAllLocalClientsInfo().size()));
            //log.debug(String.format("                                   +             ClientsConn       = %d", comm.getLocalConnections().getConnectedCount()));
            //log.debug(String.format("                                   +             ClientsDisconn    = %d", comm.getLocalConnections().getDisconnectedCount()));
            InetAddress localAddr = comm.getLocalServer().getAddr();
            Integer localPort = comm.getLocalServer().getPort();
            log.debug(String.format("                                   +             HostName          = %s", (localAddr != null ? localAddr.getHostName() : "N/A")));
            log.debug(String.format("                                   +             IPAddr            = %s", (localAddr != null ? localAddr.getHostAddress() : "N/A")));
            log.debug(String.format("                                   +             Port              = %s", (localPort != null ? localPort : "N/A")));
            log.debug(String.format("                                   +             IsRunning         = %s", comm.getLocalServer().getState().isRunning()));
            //log.debug(String.format("                                   +             lastStart         = %s", comm.getLocalConnections().getLastStartup()));
            //log.debug(String.format("                                   +             lastStop          = %s", comm.getLocalConnections().getLastShutdown()));
            //log.debug(String.format("                                   +             lastConn          = %s", comm.getLocalConnections().getLastObjConnection()));
            //log.debug(String.format("                                   +             lastDiscon        = %s", comm.getLocalConnections().getLastObjDisconnection()));
        } else
            log.debug("                                   +             State (Server)    = N/A");
        List<JODLocalClientInfo> srvsList = getCommunication().getAllLocalClientsInfo();
        StringBuilder srvNames = new StringBuilder();
        for (JODLocalClientInfo ci : srvsList)
            srvNames.append(ci.getFullSrvId()).append(", ");
        log.debug("                                   +  -- Objs Mngr");
        log.debug(String.format("                                   +         Count                 = %s", srvsList.size()));
        log.debug(String.format("                                   +         List                  = %s", srvNames));
    }

    private void startupInstance() {
        assert state.enumEquals(JODState.STOP)
                || state.enumEquals(JODState.RESTARTING) :
                "Method startupInstance() can be called only from STOP or RESTARTING state";

        synchronized (state) {
            if (state.enumNotEquals(JODState.RESTARTING))
                state.set(JODState.STARTING);

            long start = new Date().getTime();

            Events.registerJODStart("Startup sub-systems");
            events.startCloudSync();
            objInfo.startAutoRefresh();
            permissions.startAutoRefresh();
            history.startCloudSync();
            structure.startAutoRefresh();
            executor.activateAll();

            try {
                boolean startLocal = ((JODSettings_002) settings).getLocalEnabled();
                log.info(String.format("JODCommunication local communication %s", startLocal ? "enabled" : "disabled"));
                if (startLocal) comm.startLocal();

            } catch (JODCommunication.LocalCommunicationException e) {
                log.warn(String.format("Error on starting local communication of '%s' object because %s", objInfo.getObjId(), e.getMessage()), e);
            }

            try {
                boolean startCloud = ((JODSettings_002) settings).getCloudEnabled();
                log.info(String.format("JCP GWs client %s", startCloud ? "enabled" : "disabled"));
                if (startCloud)
                    comm.getCloudConnection().connect();

            } catch (PeerConnectionException e) {
                if (!comm.getCloudAPIs().isConnected())
                    log.warn("Can't connect GWs client because JCP API not available, retry when JCP API become reachable");
                else
                    log.warn("JCP GWs client not connected, retry later", e);
            }

            long time = new Date().getTime() - start;
            Events.registerJODStart("Sub-systems started successfully", time);

            if (state.enumNotEquals(JODState.RESTARTING))
                state.set(JODState.RUN);
        }

        log.info(String.format("JOD Object '%s' started", objInfo.getObjId()));

        printInstanceInfo();
    }

    private void shutdownInstance() {
        assert state.enumEquals(JODState.RUN)
                || state.enumEquals(JODState.RESTARTING) :
                "Method shutdownInstance() can be called only from RUN or RESTARTING state";

        long start = new Date().getTime();

        synchronized (state) {
            if (state.enumNotEquals(JODState.RESTARTING))
                state.set(JODState.SHOUTING);

            Events.registerJODStop("Stopping sub-system");

            log.trace("JODCommunication stop server and disconnect from JCP");
            try {
                comm.stopLocal();

            } catch (JODCommunication.LocalCommunicationException e) {
                log.warn(String.format("Error on hiding local communication object's server '%s' because %s", objInfo.getObjId(), e.getMessage()), e);
            }

            try {
                comm.getCloudConnection().disconnect();

            } catch (PeerDisconnectionException e) {
                log.warn(String.format("Error on disconnecting cloud communication of '%s' object because %s", objInfo.getObjId(), e.getMessage()), e);
            }

            log.trace("JODExecutor disable all workers");
            executor.deactivateAll();

            log.trace("JODObjectInfo stopping");
            objInfo.stopAutoRefresh();

            log.trace("JODPermission stopping");
            permissions.stopAutoRefresh();

            log.trace("JODStructure stopping");
            structure.stopAutoRefresh();

            try {
                history.storeCache();
            } catch (IOException e) {
                log.warn("Can't flush status on file, continue shutdown JOD");
            }
            log.trace("JODHistory stopping");
            history.stopCloudSync();

            long time = new Date().getTime() - start;
            Events.registerJODStop("Sub-system stopped successfully", time);
            try {
                Events.storeCache();
            } catch (IOException e) {
                log.warn("Can't flush events on file, continue shutdown JOD");
            }
            log.trace("JODEvents stopping");
            events.stopCloudSync();

            if (state.enumNotEquals(JODState.RESTARTING))
                state.set(JODState.STOP);
        }

        log.info(String.format("JOD Object '%s' stopped", objInfo.getObjId()));
    }

    private void restartInstance() {
        assert state.enumEquals(JODState.RUN)
                || state.enumEquals(JODState.STOP) :
                "Method shutdownInstance() can be called only from RUN or STOP state";

        synchronized (state) {
            state.set(JODState.RESTARTING);

            log.trace("JOD shout down for restarting");
            if (state.enumEquals(JODState.RUN))
                shutdownInstance();

            log.trace("JOD startup for restarting");
            startupInstance();

            state.set(JODState.RUN);
        }

        log.info(String.format("JOD Object '%s' restarted", objInfo.getObjId()));
    }


    // JOD Systems

    @Override
    public JCPAPIsClientObj getJCPClient() {
        return jcpClient;
    }

    @Override
    public JODObjectInfo getObjectInfo() {
        return objInfo;
    }

    @Override
    public JODStructure getObjectStructure() {
        return structure;
    }

    @Override
    public JODCommunication getCommunication() {
        return comm;
    }

    @Override
    public JODExecutorMngr getExecutor() {
        return executor;
    }

    @Override
    public JODPermissions getPermission() {
        return permissions;
    }

    @Override
    public JODEvents getEvents() {
        return events;
    }

    @Override
    public JODHistory getHistory() {
        return history;
    }

}
