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

import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.jod.structure.JODState;
import com.robypomper.josp.jod.structure.JODStateUpdate;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.protocol.JOSPPerm;

import java.util.List;


/**
 * Interface for Object's communication system.
 * <p>
 * is responsible for the JOSP communications and the relative message routing.
 * To communicate with JOSP Services, the JOD Agent support both JOSP Communications
 * types:
 * <ul>
 *     <li>Direct/Local communication via {@link JODLocalServer}</li>
 *     <li>Cloud/Remote communication via {@link JODGwO2SClient}</li>
 * </ul>
 * <p>
 * The local communication include an SSL TCP server, his publication on the
 * ZeroConf protocol and local client management.
 * <p>
 * Cloud communication provide a channel to remote service via the JCP. When
 * activate, it start a tcp/ws socket to the JOSP GW on the JCP cloud. Then
 * the JOSP GW act as gateway from current object and remote services. Even
 * if the JOSP GW check service/user permissions, current object perform security
 * checks.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface JODCommunication {

    // To Service Msg

    /**
     * Is the main method, used by the JOSP Object to communicate with all the JOSP Services
     * (both local and remote). It is mostly used to send state update messages, but
     * also to synchronize the JOSP Object (the object's info, structure and permissions)
     * with all JOSP Services.
     * <p>
     * JOSP Protocol messages that can be sent using this method are:
     * <ul>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectStateUpdMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectInfoMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectPermsMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectStructMsg}</li>
     * </ul>
     *
     * @param msg message to send to all JOSP Services (local and remote).
     * @param minReqPerm minimum required permission to send the message.
     * @return true if the message was sent to at least one JOSP Service, false otherwise.
     */
    boolean sendToServices(String msg, JOSPPerm.Type minReqPerm);

    /**
     * Is used by the JOSP Object to send a message only to the JCP GWs.
     * <p>
     * JOSP Protocol messages that can be sent using this method are:<br/>
     * N/A
     *
     * @param msg message to send to the cloud.
     * @return true if the message was sent to the cloud, false otherwise.
     * @throws CloudNotConnected if the cloud is not connected.
     */
    boolean sendToCloud(String msg) throws CloudNotConnected;

    /**
     * Is used by the JOSP Object to send a message only to a specific JSL service.
     * <p>
     * It is fist used to send the JOSP Object's presentation message to the JOSP Service
     * on local connection, but also to send response messages to the JOSP Services'
     * requests (like the History messages).
     * <p>
     * JOSP Protocol messages that can be sent using this method are:
     * <ul>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectInfoMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectStructMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createObjectPermsMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createServicePermMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createHistoryResMsg}</li>
     *     <li>{@link com.robypomper.josp.protocol.JOSPProtocol_ObjectToService#createEventsResMsg} </li>
     * </ul>
     *
     * @param locConn the local client info of the service to send the message to.
     * @param msg message to send to the service.
     * @param minReqPerm minimum required permission to send the message.
     * @return true if the message was sent to the service, false otherwise.
     * @throws ServiceNotConnected if the service is not connected.
     */
    boolean sendToSingleLocalService(JODLocalClientInfo locConn, String msg, JOSPPerm.Type minReqPerm) throws ServiceNotConnected;

    /**
     * Dispatch <code>component</code> <code>update</code> to connected and allowed
     * services.
     * <p>
     * This method is required by {@link JODState} when receive an update from
     * his {@link com.robypomper.josp.jod.executor.JODPuller} or
     * {@link com.robypomper.josp.jod.executor.JODListener} object.
     *
     * @param component the object's component that updated his state.
     * @param update    the status update info.
     */
    void sendObjectUpdMsg(JODState component, JODStateUpdate update);

    void syncObject();


    // From Service Msg

    boolean processFromServiceMsg(String msg, JOSPPerm.Connection connType);


    // Connections access

    /**
     * @return the JCP APIs connection, null if not connected.
     */
    JCPAPIsClientObj getCloudAPIs();

    /**
     * @return the Gw S2O connection, null if not connected.
     */
    JODGwO2SClient getCloudConnection();

    /**
     * @return the Local server, null if not started.
     */
    JODLocalServer getLocalServer();

    /**
     * @return a list containing all local connections.
     */
    List<JODLocalClientInfo> getAllLocalClientsInfo();

    /**
     * @return an array containing all local connections.
     */
    JODLocalClientInfo findLocalClientsInfo(String serviceId);


    // Mngm methods

    /**
     * @return <code>true</code> if local communication server is running.
     */
    boolean isLocalRunning();

    /**
     * Start local Object's server and publish it.
     */
    void startLocal() throws LocalCommunicationException;

    /**
     * Stop local Object's server and de-publish it, then close all opened connections.
     */
    void stopLocal() throws LocalCommunicationException;


    // Cross component references

    /**
     * Set the {@link JODStructure} reference to the current object.
     * <p>
     * This cross-system reference is required by the Action Execution Flow.
     *
     * @param structure the {@link JODStructure} reference.
     */
    void setStructure(JODStructure structure) throws StructureSetException;

    /**
     * @return the object's Structure sub system reference.
     */
    JODStructure getStructure() throws StructureSetException;


    // Exceptions

    /**
     * Exceptions for {@link #setStructure(JODStructure)} called twice.
     */
    class StructureSetException extends Throwable {
        private static final String MSG = "Structure already set for current Communication.";

        public StructureSetException() {
            super(MSG);
        }
    }

    /**
     * Exceptions for local communication errors.
     */
    class LocalCommunicationException extends Throwable {

        public LocalCommunicationException(String msg) {
            super(msg);
        }

        public LocalCommunicationException(String msg, Throwable e) {
            super(msg, e);
        }

    }

    /**
     * Exceptions for cloud communication errors.
     */
    class CloudCommunicationException extends Throwable {

        public CloudCommunicationException(String msg) {
            super(msg);
        }

        public CloudCommunicationException(String msg, Throwable e) {
            super(msg, e);
        }

    }

    /**
     * Exceptions for cloud communication errors.
     */
    class MissingPermissionException extends Throwable {

        private static final String MSG = "Can't elaborate '%s' request because missing permission on service '%s' / user '%s'";

        public MissingPermissionException(String reqType, String srvId, String usrId) {
            super(String.format(MSG, reqType, srvId, usrId));
        }

    }

    class ServiceNotConnected extends Throwable {
        private static final String MSG = "Can't access to '%s' service because not connected.";

        public ServiceNotConnected(JODLocalClientInfo locConn) {
            super(String.format(MSG, locConn.getFullSrvId()));
        }

        public ServiceNotConnected(JODLocalClientInfo locConn, Throwable t) {
            super(String.format(MSG, locConn.getFullSrvId()), t);
        }
    }

    class CloudNotConnected extends Throwable {
        private static final String MSG = "Can't access to O2S gateway because not connected.";

        public CloudNotConnected(JODGwO2SClient cloudConn) {
            super(MSG);
        }

        public CloudNotConnected(JODGwO2SClient cloudConn, Throwable t) {
            super(MSG, t);
        }
    }

}
