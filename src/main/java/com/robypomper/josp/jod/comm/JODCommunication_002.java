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

package com.robypomper.josp.jod.comm;
import com.robypomper.comm.exception.*;
import com.robypomper.comm.peer.Peer;
import com.robypomper.comm.peer.PeerConnectionListener;
import com.robypomper.discovery.DiscoverySystemFactory;
import com.robypomper.discovery.Publisher;
import com.robypomper.java.JavaJSONArrayToFile;
import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.clients.JCPClient2;
import com.robypomper.josp.jod.JODSettings_002;
import com.robypomper.josp.jod.events.Events;
import com.robypomper.josp.jod.events.JODEvents_002;
import com.robypomper.josp.jod.objinfo.JODObjectInfo_002;
import com.robypomper.josp.jod.permissions.JODPermissions;
import com.robypomper.josp.jod.permissions.JODPermissions_002;
import com.robypomper.josp.jod.structure.*;
import com.robypomper.josp.protocol.*;
import com.robypomper.josp.types.josp.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of the {@link JODCommunication} interface.
 */
public class JODCommunication_002 implements JODCommunication {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JODCommunication_002.class);
    // JOD
    private final JODSettings_002 locSettings;
    private final JODObjectInfo_002 objInfo;
    private final String instanceId;
    private final JODPermissions_002 permissions;
    private final JODEvents_002 events;
    private JODStructure_002 structure;
    // Comms
    private final JCPAPIsClientObj jcpClient;
    private final JODGwO2SClient gwClient;
    private JODLocalServer localServer;
    private Publisher localServerPublisher = null;


    // Constructor

    /**
     * Default constructor that initialize the local server and the O2S gateway
     * client.
     *
     * @param settings    the JOD settings.
     * @param objInfo     the object's info.
     * @param jcpClient   the jcp object client.
     * @param permissions the {@link JODPermissions} instance used to check
     *                    connected services's permissions to receive updates or
     * @param events      the object's events manager.
     */
    public JODCommunication_002(JODSettings_002 settings, JODObjectInfo_002 objInfo, JCPAPIsClientObj jcpClient, JODPermissions_002 permissions, JODEvents_002 events, String instanceId) throws LocalCommunicationException {
        this.locSettings = settings;
        this.instanceId = instanceId;
        this.objInfo = objInfo;
        this.permissions = permissions;
        this.jcpClient = jcpClient;
        jcpClient.addConnectionListener(jcpConnectionListener);
        this.events = events;

        // Init local server
        int localPort = locSettings.getLocalServerPort();
        log.trace(String.format("Local object's server use '%s' server id", objInfo.getObjId()));
        log.trace(String.format("Local object's server use '%d' port", localPort));
        localServer = new JODLocalServer(this, objInfo, permissions, locSettings);
        log.trace(String.format("Local object's server use '%d' port", localServer.getPort()));

        // Publish local object server, if server's port is already known
        if (locSettings.getLocalServerPort() != 0)
            initializeLocalServerPublisher(locSettings.getLocalServerPort());

        // Init cloud object client
        this.gwClient = new JODGwO2SClient(this, objInfo, jcpClient, instanceId);
        this.gwClient.addListener(gwClientListener);

        log.info(String.format("Initialized JODCommunication instance for '%s' ('%s') object", objInfo.getObjName(), objInfo.getObjId()));
    }

    private void initializeLocalServerPublisher(int localPort) throws LocalCommunicationException {
        String publisherImpl = locSettings.getLocalDiscovery();
        String publisherSrvName = objInfo.getObjName() + "-" + instanceId;
        try {
            log.debug(String.format("Creating publisher '%s' service for local object's server", publisherImpl));
            log.trace(String.format("Local object's server publisher use '%s' service name", publisherSrvName));
            localServerPublisher = DiscoverySystemFactory.createPublisher(publisherImpl, JOSPProtocol.DISCOVERY_TYPE, publisherSrvName, localPort, instanceId);
            log.debug(String.format("Publisher '%s' service created for local object's server", publisherImpl));

        } catch (Publisher.PublishException e) {
            log.warn(String.format("Error on creating publisher '%s' service for local object's server because %s", publisherImpl, e.getMessage()), e);
            throw new LocalCommunicationException(String.format("Error on creating publisher '%s' service for local object's server", publisherImpl), e);
        }
    }


    // To Service Msg

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendToServices(String msg, JOSPPerm.Type minReqPerm) {
        log.debug(String.format("JOD Communication send '%s' message to local services and cloud", msg.substring(0, msg.indexOf('\n'))));

        // Send via local communication
        if (isLocalRunning()) {
            for (JODLocalClientInfo locConn : getAllLocalClientsInfo()) {
                if (!locConn.isConnected() || !permissions.checkPermission(locConn.getSrvId(), locConn.getUsrId(), minReqPerm, JOSPPerm.Connection.OnlyLocal))
                    continue;

                try {
                    locConn.getClient().sendData(msg);

                } catch (PeerNotConnectedException | PeerStreamException e) {
                    log.warn(String.format("JOD Communication error on send data to JSL service '%s'", locConn.getClientId()), e);
                }
            }
        }

        // Send via cloud communication
        if (gwClient.getState().isConnected()) {
            try {
                gwClient.sendData(msg);

            } catch (PeerNotConnectedException | PeerStreamException e) {
                log.warn("JOD Communication error on send data to JCP GW", e);
            }
        }

        return isLocalRunning() || gwClient.getState().isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendToCloud(String msg) throws CloudNotConnected {
        log.debug(String.format("JOD Communication send '%s' message to cloud only", msg.substring(0, msg.indexOf('\n'))));

        try {
            gwClient.sendData(msg);
            return true;

        } catch (PeerNotConnectedException | PeerStreamException e) {
            throw new CloudNotConnected(gwClient, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendToSingleLocalService(JODLocalClientInfo locConn, String msg, JOSPPerm.Type minReqPerm) throws ServiceNotConnected {
        log.debug(String.format("JOD Communication send '%s' message to JSL service '%s' only", msg.substring(0, msg.indexOf('\n')), locConn.getFullSrvId()));

        if (!permissions.checkPermission(locConn.getSrvId(), locConn.getUsrId(), minReqPerm, JOSPPerm.Connection.OnlyLocal))
            return false;

        try {
            locConn.getClient().sendData(msg);
            return true;

        } catch (PeerNotConnectedException | PeerStreamException e) {
            throw new ServiceNotConnected(locConn, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendObjectUpdMsg(JODState component, JODStateUpdate update) {
        sendToServices(JOSPProtocol_ObjectToService.createObjectStateUpdMsg(objInfo.getObjId(), component.getPath().getString(), update), JOSPPerm.Type.State);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncObject() {
        objInfo.syncObjInfo();
        structure.syncObjStruct();
        permissions.syncObjPermissions();
    }


    // From Service Msg

    @Override
    public boolean processFromServiceMsg(String msg, JOSPPerm.Connection connType) {
        String srvId;
        String usrId;
        try {
            srvId = JOSPProtocol_ServiceToObject.getSrvId(msg);
            usrId = JOSPProtocol_ServiceToObject.getUsrId(msg);
            log.info(String.format("Received '%s' message from %s/%s (%s)", msg.substring(0, msg.indexOf('\n')), srvId, usrId, connType == JOSPPerm.Connection.OnlyLocal ? "local connection" : "cloud connection"));

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on parsing '%s' message because %s", msg.substring(0, msg.indexOf('\n')), e.getMessage()), e);
            return false;
        }

        try {
            // dispatch to processor
            boolean processedSuccessfully;
            if (JOSPProtocol_ServiceToObject.isObjectSetNameMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.CoOwner, connType) && processObjectSetNameMsg(msg);
            else if (JOSPProtocol_ServiceToObject.isObjectSetOwnerIdMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.CoOwner, connType) && processObjectSetOwnerIdMsg(msg);
            else if (JOSPProtocol_ServiceToObject.isObjectAddPermMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.CoOwner, connType) && processObjectAddPermMsg(msg);
            else if (JOSPProtocol_ServiceToObject.isObjectUpdPermMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.CoOwner, connType) && processObjectUpdPermMsg(msg);
            else if (JOSPProtocol_ServiceToObject.isObjectRemPermMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.CoOwner, connType) && processObjectRemPermMsg(msg);

            else if (JOSPProtocol_ServiceToObject.isObjectActionCmdMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPPerm.Type.Actions, connType) && processObjectCmdMsg(msg, srvId, usrId, connType);

            else if (JOSPProtocol_ServiceToObject.isHistoryReqMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPProtocol_ServiceToObject.HISTORY_MSG_REQ_MIN_PERM, connType) && processHistoryReqMsg(msg, srvId, usrId, connType);
            else if (JOSPProtocol_ServiceToObject.isEventsReqMsg(msg))
                processedSuccessfully = permissions.checkPermission(srvId, usrId, JOSPProtocol_ServiceToObject.EVENTS_MSG_REQ_MIN_PERM, connType) && processEventsReqMsg(msg, srvId, usrId, connType);

            else
                throw new Throwable("Unknown message type");

            if (!processedSuccessfully)
                throw new Throwable("Error processing message");

            log.info(String.format("Message '%s' from %s/%s processed successfully", msg.substring(0, msg.indexOf('\n')), srvId, usrId));
            return true;

        } catch (Throwable t) {
            log.warn(String.format("Error on processing '%s' message from %s/%s because %s", msg.substring(0, msg.indexOf('\n')), srvId, usrId, t.getMessage()), t);
            return false;
        }
    }

    private boolean processObjectCmdMsg(String msg, String srvId, String usrId, JOSPPerm.Connection connType) {
        if (!JOSPProtocol.isCmdMsg(msg))
            return false;

        JOSPProtocol.ActionCmd cmd;
        try {
            cmd = JOSPProtocol.fromMsgToCmd(msg, AbsJODAction.getActionClasses());
        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on parsing command '%s...' because %s", msg.substring(0, msg.indexOf("\n")), e.getMessage()), e);
            return false;
        }

        log.info(String.format("Forward command to component '%s'", cmd.getComponentPath()));
        Events.registerActionReq(srvId, usrId, connType, cmd);

        // search destination components
        JODComponentPath compPath = new DefaultJODComponentPath(cmd.getComponentPath());
        JODComponent comp = DefaultJODComponentPath.searchComponent(structure.getRoot(), compPath);

        // exec command msg
        log.trace(String.format("Processing command on '%s' component", compPath.getString()));
        if (comp == null) {
            log.warn(String.format("Error on processing update on '%s' component because component not found", compPath.getString()));
            Events.registerActionExecFail("Component not found", srvId, usrId, connType, cmd);
            return false;
        }
        if (!(comp instanceof JODAction)) {
            log.warn(String.format("Error on processing update on '%s' component because component not an action component", compPath.getString()));
            Events.registerActionExecFail("Component is not an action", srvId, usrId, connType, cmd);
            return false;
        }
        JODAction actionComp = (JODAction) comp;

        // exec component's action
        if (!actionComp.execAction(cmd)) {
            log.warn(String.format("Error on processing command on '%s' component", compPath.getString()));
            Events.registerActionExecFail("Unknown error on executing action command", srvId, usrId, connType, cmd);
            return false;
        }
        log.info(String.format("Command status of '%s' component", compPath.getString()));

        log.debug(String.format("Command '%s...' processed", msg.substring(0, Math.min(10, msg.length()))));
        Events.registerActionExec(srvId, usrId, connType, cmd);
        return true;
    }

    private boolean processHistoryReqMsg(String msg, String srvId, String usrId, JOSPPerm.Connection connType) {
        if (connType == JOSPPerm.Connection.LocalAndCloud) {
            log.warn(String.format("Error on processing message %s because received from cloud communication", JOSPProtocol_ServiceToObject.HISTORY_MSG_REQ_NAME));
            return false;
        }

        String fullSrvId;
        String compPathStr;
        String reqId;
        HistoryLimits limits;
        try {
            fullSrvId = JOSPProtocol_ServiceToObject.getHistoryReqMsg_FullSrvId(msg);
            compPathStr = JOSPProtocol_ServiceToObject.getHistoryReqMsg_CompPath(msg);
            reqId = JOSPProtocol_ServiceToObject.getHistoryReqMsg_ReqId(msg);
            limits = JOSPProtocol_ServiceToObject.getHistoryReqMsg_Limits(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.HISTORY_MSG_REQ_NAME, e.getMessage()), e);
            return false;
        }

        // search destination components
        JODComponentPath compPath = new DefaultJODComponentPath(compPathStr);
        JODComponent comp = DefaultJODComponentPath.searchComponent(structure.getRoot(), compPath);
        if (comp == null) {
            log.warn(String.format("Error on processing message %s because component '%s' not found", JOSPProtocol_ServiceToObject.HISTORY_MSG_REQ_NAME, compPathStr));
            return false;
        }

        // prepare response
        List<JOSPHistory> statusesHistory = comp.getHistoryStatus(limits);
        String response = JOSPProtocol_ObjectToService.createHistoryResMsg(objInfo.getObjId(), compPathStr, reqId, statusesHistory);
        log.debug(String.format("Found '%d' status history for '%s' component", statusesHistory.size(), compPathStr));

        // send response
        JODLocalClientInfo locConn = findLocalClientsInfo(fullSrvId);
        if (locConn == null) {
            log.warn(String.format("Error on sending message %s because can't find service '%s'", JOSPProtocol_ObjectToService.HISTORY_MSG_RES_NAME, fullSrvId));
            return true; // it processed the message successfully but can't send the response
        }

        try {
            sendToSingleLocalService(locConn, response, JOSPPerm.Type.State);
        } catch (ServiceNotConnected e) {
            log.warn(String.format("Error on sending message %s because %s", JOSPProtocol_ObjectToService.HISTORY_MSG_RES_NAME, e.getMessage()), e);
            return false;
        }

        return true;
    }

    private boolean processEventsReqMsg(String msg, String srvId, String usrId, JOSPPerm.Connection connType) {
        if (connType == JOSPPerm.Connection.LocalAndCloud) {
            log.warn(String.format("Error on processing message %s because received from cloud communication", JOSPProtocol_ServiceToObject.EVENTS_MSG_REQ_NAME));
            return false;
        }

        String fullSrvId;
        String reqId;
        HistoryLimits limits;
        String filterEventType;
        try {
            fullSrvId = JOSPProtocol_ServiceToObject.getEventsReqMsg_FullSrvId(msg);
            reqId = JOSPProtocol_ServiceToObject.getEventsReqMsg_ReqId(msg);
            limits = JOSPProtocol_ServiceToObject.getEventsReqMsg_Limits(msg);
            filterEventType = JOSPProtocol_ServiceToObject.getEventsReqMsg_FilterEventType(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.EVENTS_MSG_REQ_NAME, e.getMessage()), e);
            return false;
        }


        // prepare response
        List<JOSPEvent> eventsHistory;
        if (filterEventType.isEmpty())
            eventsHistory = events.getHistoryEvents(limits);
        else {
            EventType type = EventType.valueOf(filterEventType);
            eventsHistory = events.filterHistoryEvents(limits, new JavaJSONArrayToFile.Filter<JOSPEvent>() {
                @Override
                public boolean accepted(JOSPEvent o) {
                    return o.getType() == type;
                }
            });
        }
        String response = JOSPProtocol_ObjectToService.createEventsResMsg(objInfo.getObjId(), reqId, eventsHistory);

        // send response
        JODLocalClientInfo locConn = findLocalClientsInfo(fullSrvId);
        try {
            sendToSingleLocalService(locConn, response, JOSPProtocol_ObjectToService.EVENTS_MSG_RES_MIN_PERM);
        } catch (ServiceNotConnected e) {
            log.warn(String.format("Error on sending message %s because %s", JOSPProtocol_ObjectToService.EVENTS_MSG_RES_NAME, e.getMessage()), e);
            return false;
        }

        return true;
    }

    private boolean processObjectSetNameMsg(String msg) {
        String newName;
        try {
            newName = JOSPProtocol_ServiceToObject.getObjectSetNameMsg_Name(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.OBJ_SETNAME_REQ_NAME, e.getMessage()), e);
            return false;
        }

        objInfo.setObjName(newName);
        return true;
    }

    private boolean processObjectSetOwnerIdMsg(String msg) {
        String newOwnerId;
        try {
            newOwnerId = JOSPProtocol_ServiceToObject.getObjectSetOwnerIdMsg_OwnerId(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.OBJ_SETOWNERID_REQ_NAME, e.getMessage()), e);
            return false;
        }

        objInfo.setOwnerId(newOwnerId);
        return true;
    }

    private boolean processObjectAddPermMsg(String msg) {
        String srvId;
        String usrId;
        JOSPPerm.Type permType;
        JOSPPerm.Connection connType;
        try {
            srvId = JOSPProtocol_ServiceToObject.getObjectAddPermMsg_SrvId(msg);
            usrId = JOSPProtocol_ServiceToObject.getObjectAddPermMsg_UsrId(msg);
            permType = JOSPProtocol_ServiceToObject.getObjectAddPermMsg_PermType(msg);
            connType = JOSPProtocol_ServiceToObject.getObjectAddPermMsg_ConnType(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.OBJ_ADDPERM_REQ_NAME, e.getMessage()), e);
            return false;
        }

        permissions.addPermissions(srvId, usrId, permType, connType);
        return true;
    }

    private boolean processObjectUpdPermMsg(String msg) {
        String permId;
        String srvId;
        String usrId;
        JOSPPerm.Type permType;
        JOSPPerm.Connection connType;
        try {
            permId = JOSPProtocol_ServiceToObject.getObjectUpdPermMsg_PermId(msg);
            srvId = JOSPProtocol_ServiceToObject.getObjectUpdPermMsg_SrvId(msg);
            usrId = JOSPProtocol_ServiceToObject.getObjectUpdPermMsg_UsrId(msg);
            permType = JOSPProtocol_ServiceToObject.getObjectUpdPermMsg_PermType(msg);
            connType = JOSPProtocol_ServiceToObject.getObjectUpdPermMsg_ConnType(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.OBJ_UPDPERM_REQ_NAME, e.getMessage()), e);
            return false;
        }

        permissions.updPermissions(permId, srvId, usrId, permType, connType);
        return true;
    }

    private boolean processObjectRemPermMsg(String msg) {
        String permId;
        try {
            permId = JOSPProtocol_ServiceToObject.getObjectRemPermMsg_PermId(msg);

        } catch (JOSPProtocol.ParsingException e) {
            log.warn(String.format("Error on processing message %s because %s", JOSPProtocol_ServiceToObject.OBJ_REMPERM_REQ_NAME, e.getMessage()), e);
            return false;
        }

        permissions.remPermissions(permId);
        return true;
    }


    // Connections access

    /**
     * {@inheritDoc}
     */
    @Override
    public JCPAPIsClientObj getCloudAPIs() {
        return jcpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODGwO2SClient getCloudConnection() {
        return gwClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODLocalServer getLocalServer() {
        return localServer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JODLocalClientInfo> getAllLocalClientsInfo() {
        return localServer == null ? new ArrayList<>() : localServer.getLocalClientsInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODLocalClientInfo findLocalClientsInfo(String fullSrvId) {
        if (localServer == null)
            return null;

        return localServer.getLocalClientInfo(fullSrvId);
    }


    // Mngm methods

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocalRunning() {
        return localServer != null && localServer.getState().isRunning();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startLocal() throws LocalCommunicationException {
        if (isLocalRunning())
            return;

        log.info(String.format("Start and publish local object's server '%s'", objInfo.getObjId()));

        try {
            log.debug("Starting local object's server");
            localServer.startup();
            log.debug("Local object's server started");
            Events.registerLocalStart("Comm Local Started", localServer);

            log.debug("Publishing local object's server");
            if (localServerPublisher == null)
                initializeLocalServerPublisher(localServer.getPort());
            localServerPublisher.publish(true);
            log.debug("Local object's server published");
            Events.registerLocalStart("Comm Local Published", localServer);

        } catch (Throwable e) {
            if (e instanceof ServerStartupException)
                log.warn(String.format("Error on initializing local communication object's server '%s' because %s", objInfo.getObjId(), e.getMessage()), e);
            else if (e instanceof Publisher.PublishException)
                log.warn(String.format("Error on publishing local communication object's server '%s' because %s", objInfo.getObjId(), e.getMessage()), e);
            else
                log.warn(String.format("Unknown error on initializing local communication object's server '%s' because %s", objInfo.getObjId(), e.getMessage()), e);

            Events.registerLocalStart("Comm Local Started", localServer, e);
            throw new LocalCommunicationException(String.format("Error on starting local communication object's server '%s'", objInfo.getObjId()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopLocal() throws LocalCommunicationException {
        if (!isLocalRunning())
            return;

        log.info(String.format("Stop and hide local object's server '%s' on port '%d'", objInfo.getObjId(), localServer.getPort()));

        try {
            log.debug("Stopping local object's server");
            localServer.shutdown();
            log.debug("Local object's server stopped");
            Events.registerLocalStop("Comm Local Stopped", localServer);

            log.debug("Hiding local object's server");
            if (localServerPublisher == null)
                initializeLocalServerPublisher(localServer.getPort());
            localServerPublisher.hide(true);
            log.debug("Local object's server hided");
            Events.registerLocalStop("Comm Local Hided", localServer);

        } catch (ServerException e) {
            log.warn(String.format("Error on shutdown local communication object's server '%s' because %s", objInfo.getObjId(), e.getMessage()), e);
            Events.registerLocalStart("Comm Local Stopped", localServer, e);
            throw new LocalCommunicationException(String.format("Error on shutdown local communication object's server '%s'", objInfo.getObjId()), e);

        }
    }


    // Cross component references

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStructure(JODStructure structure) throws StructureSetException {
        if (this.structure != null)
            throw new StructureSetException();
        this.structure = (JODStructure_002) structure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODStructure getStructure() throws StructureSetException {
        if (this.structure == null)
            throw new StructureSetException();
        return this.structure;
    }


    // JCP Client listeners

    @SuppressWarnings("FieldCanBeLocal")
    private final JCPClient2.ConnectionListener jcpConnectionListener = new JCPClient2.ConnectionListener() {

        private boolean connFailedPrinted = false;

        @Override
        public void onConnected(JCPClient2 jcpClient) {
            log.info(String.format("JCP APIs Client connected with %s flow", getFlowName(jcpClient)));
            Events.registerJCPConnection("JCP Connected", jcpClient, getFlowName(jcpClient));
            connFailedPrinted = false;
        }

        @Override
        public void onConnectionFailed(JCPClient2 jcpClient, Throwable t) {
            if (t instanceof JCPClient2.JCPNotReachableException) {
                if (connFailedPrinted) {
                    log.debug(String.format("JCP APIs at '%s' still unreachable", jcpClient.getAPIsUrl()));
                } else {
                    log.warn(String.format("Can't connect to JCP APIs at '%s', retry later", jcpClient.getAPIsUrl()));
                    connFailedPrinted = true;
                }
            } else
                log.warn(String.format("Error on JCP APIs connection attempt at '%s'", jcpClient.getAPIsUrl()), t);
            Events.registerJCPConnection("JCP Connection failed", jcpClient, getFlowName(jcpClient), t);
        }

        @Override
        public void onAuthenticationFailed(JCPClient2 jcpClient, Throwable t) {
            log.warn(String.format("Error on authenticating to JCP APIs because %s", t.getMessage()), t);
            Events.registerJCPConnection("JCP Connection failed (authentication phase)", jcpClient, getFlowName(jcpClient), t);
        }

        @Override
        public void onDisconnected(JCPClient2 jcpClient) {
            log.info(String.format("JCP APIs Client disconnected with %s flow", getFlowName(jcpClient)));
            Events.registerJCPDisconnection("JCP Disconnected", jcpClient, getFlowName(jcpClient));
        }

    };

    @SuppressWarnings("FieldCanBeLocal")
    private final PeerConnectionListener gwClientListener = new PeerConnectionListener() {

        @Override
        public void onConnecting(Peer peer) {
        }

        @Override
        public void onWaiting(Peer peer) {
        }

        @Override
        public void onConnect(Peer peer) {
            log.info("JCP GWs client connected");
        }

        @Override
        public void onDisconnecting(Peer peer) {
        }

        @Override
        public void onDisconnect(Peer peer) {
            log.info("JCP GWs Client disconnected");
        }

        @Override
        public void onFail(Peer peer, String failMsg, Throwable t) {
            // Connection problem
            if (t instanceof PeerConnectionException) {
                log.warn(String.format("Error on JCP GWs client connection: %s (%s)", failMsg, t), t);
                return;
            }

            log.warn(String.format("Error on JCP GWs client: %s (%s)", failMsg, t), t);
        }

    };

    private static String getFlowName(JCPClient2 jcpClient) {
        if (jcpClient.isUserAnonymous()) return "ClientCred";
        if (jcpClient.isUserAuthenticated()) return "AuthCode";
        return "NotConnected";
    }

}
