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
import com.robypomper.comm.server.*;
import com.robypomper.comm.trustmanagers.AbsCustomTrustManager;
import com.robypomper.comm.trustmanagers.DynAddTrustManager;
import com.robypomper.java.JavaJKS;
import com.robypomper.java.JavaSSL;
import com.robypomper.java.JavaThreads;
import com.robypomper.josp.jod.JODSettings_002;
import com.robypomper.josp.jod.events.Events;
import com.robypomper.josp.jod.objinfo.JODObjectInfo;
import com.robypomper.josp.jod.permissions.JODPermissions;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.protocol.JOSPPerm;
import com.robypomper.josp.protocol.JOSPProtocol;
import com.robypomper.josp.protocol.JOSPProtocol_ObjectToService;

import com.robypomper.josp.protocol.JOSPSecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.*;


/**
 * Server that listen and process local client (JSL) connections.
 * <p>
 * This class wraps a {@link Server} and provide a way to manage local
 * connections from JSL clients. The main methods are the {@link #processOnClientConnected(ServerClient)}
 * and {@link #processOnClientDisconnected(ServerClient)} that handle the
 * client's connections and disconnections.
 * <p>
 * It handles the incoming connections from the JOSP Services.<br/>
 * The JODLocalServer creates a new {@link JODLocalClientInfo} object for each
 * incoming connection and keep always at most one connection for each JOSP
 * Service.<br/>
 * At the same time, the JODLocalServer sends the object's presentation message
 * to the JOSP Service, as defined by the JOSP Protocol, and starts forwarding
 * the incoming messages to the {@link JODCommunication_002} instance.<br/>
 * Once the JOSP Service is registered or updated, his JODLocalClientInfo object
 * is available using the `getLocalClientInfo` or `getLocalClientInfoByServiceId`
 * methods.
 * <p>
 * In order to support the {@link JOSPSecurityLevel}, the JODLocalServer can use
 * different server implementations: with and without SSL. If SSL is enabled,
 * then the server can enable the SSLShared feature to share the server
 * certificate with the JOSP Services (the clients). That enables the "Share"
 * Security levels. Moreover, always if SSL server is enabled, the certificate
 * can contain the JOSP Object's ID and the JOSP Object's owner ID. That enables
 * the "Instance" Security levels.
 * <p>
 * The incoming messages are processed by the {@link #processData(ServerClient, String)}
 * method that forward the message to the {@link JODCommunication#processFromServiceMsg(String, JOSPPerm.Connection)}
 * method.<br/>
 * On the other side the outgoing messages are sent using the
 * {@link JODCommunication#sendToServices(String, JOSPPerm.Type)} or the
 * {@link JODCommunication#sendToSingleLocalService(JODLocalClientInfo, String, JOSPPerm.Type)}
 * methods.
 * <p>
 * <b>JOSP Service connection:</b>
 * <pre>
 * 1. Set connection LUID (local-unique-id)
 * 2. Send object's Id message to server
 *    -> ERR on send object's Id message, discharge the client
 * 3. Get/Wait service's fullId
 *    -> ERR on wait service's fullId, discharge the client
 * 4. Wait for client connection
 *    -> ERR on wait for client connection, discharge the client
 * 5. Create JODLocalClientInfo from connection and service's fullId
 * 6. Get existing JODLocalClientInfo for service's fullId
 * 7. Compare new and existing JODLocalClientInfo
 *    a. Add new JOSP Service
 *       1. add new JODLocalClientInfo to localClients list
 *       2. send object's presentation message to server
 *          -> ERR on send object's presentation message, remove and discharge the service client
 *    b. Replace JOSP Service's connection (existing = disconnected)
 *       1. remove existing JODLocalClientInfo from localClients list
 *       2. add new JODLocalClientInfo to localClients list
 *       3. send object's presentation message to server
 *          -> ERR on send object's presentation message, remove and discharge the service client
 *    c. Discharge connection because JSL Service already connected
 *       1. disconnect JOSP Service's connection (local)
 * </pre>
 *
 * Class Sections:
 * <ul>
 *     <li>Constructors</li>
 *     <li>Server's wrapper methods</li>
 *     <li>Process received messages</li>
 *     <li>Clients' Connections</li>
 *     <li>Certificates mngm</li>
 *     <li>Internal local server's client listener</li>
 *     <li>Internal events processors</li>
 *     <li>LUID: Connection Local Unique ID</li>
 * </ul>
 *
 * JOD Settings used:
 * <ul>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_PORT}</li>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_SSL_ENABLED}</li>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_SSL_SHARING_ENABLED}</li>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_KS_PATH}</li>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_KS_PASS}</li>
 *     <li>{@link JODSettings_002#JODCOMM_LOCAL_KS_ALIAS}</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class JODLocalServer {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JODLocalServer.class);
    /**
     * Reference to the JOD Object's info.
     */
    private final JODObjectInfo objInfo;
    /**
     * Reference to the JOD Communication.
     */
    private final JODCommunication communication;
    /**
     * Reference to the JOD Permissions.
     */
    private final JODPermissions permissions;
    /**
     * The server's port. If is `0`m then a random port is used.
     */
    private final int port;
    /**
     * If the SSL is enabled.
     */
    private final boolean sslEnabled;
    /**
     * If the SSL Sharing is enabled.
     */
    private final boolean sslSharingEnabled;
    /**
     * The server instance. It can be a {@link ServerAbsTCP} or a
     * {@link ServerAbsSSL} depending on {@link #sslEnabled} value.
     */
    private final Server server;
    /**
     * List of all local JOSP Services (the clients) connected to the server.
     */
    private final List<JODLocalClientInfo> localClients = new ArrayList<>();
    /**
     * The SSL context used by the server. It's `null` if SSL is not enabled.
     */
    private final SSLContext sslCtx;
    /**
     * The server's certificate. It's `null` if SSL is not enabled.
     */
    private final Certificate serverCertificate;
    /**
     * The server's certificate ID. It's `null` if SSL is not enabled.
     */
    private final String serverCertificateId;
    /**
     * The trust manager used by the server.
     */
    private final AbsCustomTrustManager trustManager = new DynAddTrustManager();


    // Internal classes

    /**
     * Internal representation of the server with SSL disabled.
     */
    private class NoSSLServer extends ServerAbsTCP {

        public NoSSLServer(String objId, int port) {
            super(objId, port, JOSPProtocol.JOSP_PROTO_NAME);
        }

        @Override
        public boolean processData(ServerClient client, byte[] data) {
            return JODLocalServer.this.processData(client, data);
        }

        @Override
        public boolean processData(ServerClient client, String data) {
            return JODLocalServer.this.processData(client, data);
        }

    }


    /**
     * Internal representation of the server with SSL enabled.
     */
    private class SSLServer extends ServerAbsSSL {

        public SSLServer(String objId, int port,
                         SSLContext sslCtx, AbsCustomTrustManager trustManager, Certificate localPublicCertificate, boolean clientAuthRequired, boolean enableCertSharing) {
            super(objId, port, JOSPProtocol.JOSP_PROTO_NAME, sslCtx, trustManager, localPublicCertificate, clientAuthRequired, enableCertSharing);
        }

        @Override
        public boolean processData(ServerClient client, byte[] data) {
            return JODLocalServer.this.processData(client, data);
        }

        @Override
        public boolean processData(ServerClient client, String data) {
            return JODLocalServer.this.processData(client, data);
        }

    }


    // Constructors

    /**
     * Create a new JODLocalServer instance using configs from the given
     * {@link JODSettings_002}.
     * <p>
     * This method, if SSL is enabled, loads or generates a new certificate and
     * the SSLContext for the local communication's server.
     *
     * @param communication the JOD Communication reference.
     * @param objInfo       the JOD Object's info reference.
     * @param permissions   the JOD Permissions reference.
     * @param settings      the JOD Settings reference.
     * @throws JODCommunication.LocalCommunicationException if some error occurs
     * during the server initialization.
     */
    public JODLocalServer(JODCommunication communication, JODObjectInfo objInfo,
                          JODPermissions permissions, JODSettings_002 settings) throws JODCommunication.LocalCommunicationException {
        this.sslEnabled = settings.getLocalSSLEnabled();
        this.sslSharingEnabled = settings.getLocalSSLSharingEnabled();
        this.port = settings.getLocalServerPort();
        String ksPath = settings.getLocalKeyStorePath();
        String ksPass = settings.getLocalKeyStorePass();
        String ksAlias = settings.getLocalKeyStoreAlias();

        // Prepare SSL, if needed
        if (!sslEnabled) {
            sslCtx = null;
            serverCertificate = null;
            serverCertificateId = null;

        } else {

            boolean mustLoad = new File(ksPath).exists();
            // Load/generate certificate
            try {
                String tmpCertificateId = objInfo.getObjId();
                if (ksAlias == null || ksAlias.isEmpty())
                    ksAlias = tmpCertificateId + "-LocalCert";
                KeyStore ks;
                if (mustLoad)
                    ks = JavaJKS.loadKeyStore(ksPath, ksPass);
                else
                    ks = JavaJKS.generateAndLoadNewKeyStoreFile(tmpCertificateId, ksPath, ksPass, ksAlias);
                serverCertificate = JavaJKS.extractKeyStoreCertificate(ks, ksAlias);
                serverCertificateId = JavaJKS.getCertificateId(serverCertificate);
                sslCtx = JavaSSL.generateSSLContext(ks, ksPass, trustManager);

            } catch (JavaJKS.LoadingException |
                     JavaJKS.GenerationException |
                     JavaSSL.GenerationException e) {
                if (mustLoad)
                    throw new JODCommunication.LocalCommunicationException(String.format("Error on loading the local communication certificate at '%s'", ksPath), e);
                else
                    throw new JODCommunication.LocalCommunicationException(String.format("Error on generating the local communication certificate at '%s'", ksPath), e);
            }
        }

        // Initialize server
        server = sslEnabled ?
                new SSLServer(objInfo.getObjId(), port,
                        sslCtx, trustManager, serverCertificate,
                        true, sslSharingEnabled)
                : new NoSSLServer(objInfo.getObjId(), port);

        this.objInfo = objInfo;
        this.communication = communication;
        this.permissions = permissions;

        server.addListener(remoteClientsListener);

        if (!sslEnabled) {
            log.info("Initialized Local Communication waiting for PLAIN connections");
        } else {
            String sslShare = sslSharingEnabled ? " ENABLED" : "DISABLED";
            log.info("Initialized Local Communication waiting for ENCRYPTED connections using '" + serverCertificateId + "' as certificate's id and with SSL Share " + sslShare);
        }
    }


    // Server's wrapper methods

    /**
     * Return the server's state.
     * <p>
     * The server's state can be one of the following:
     * <ul>
     *     <li>{@link ServerState#STARTUP}</li>
     *     <li>{@link ServerState#STARTED}</li>
     *     <li>{@link ServerState#SHUTDOWN}</li>
     *     <li>{@link ServerState#STOPPED}</li>
     * </ul>
     *
     * @return the server's state.
     */
    public ServerState getState() {
        return server.getState();
    }

    /**
     * Return the server's address.
     * <p>
     * It returns the address from the internal {@link com.robypomper.comm.peer.PeerInfoLocal} object.
     *
     * @return the server's address.
     */
    public InetAddress getAddr() {
        return server.getServerPeerInfo().getAddr();
    }

    /**
     * Return the server's port.
     * <p>
     * If the configured port is `0` and the server is STARTED, then it returns
     * the port from the server's {@link com.robypomper.comm.peer.PeerInfoLocal}
     * object. Otherwise, it returns the configured port.
     *
     * @return the server's port.
     */
    public int getPort() {
        if (port == 0 && server != null && server.getServerPeerInfo() != null && server.getServerPeerInfo().getPort() != null)
            return server.getServerPeerInfo().getPort();
        return port;
    }

    /**
     * @return the list of all connected clients. From the wrapped server.
     */
    public List<ServerClient> getClients() {
        return server.getClients();
    }

    /**
     * Start the server.
     *
     * @throws ServerStartupException if some error occurs during the server startup.
     */
    public void startup() throws ServerStartupException {
        server.startup();
    }

    /**
     * Shutdown the server.
     *
     * @throws ServerShutdownException if some error occurs during the server shutdown.
     */
    public void shutdown() throws ServerShutdownException {
        server.shutdown();
    }


    // Process received messages

    /**
     * Process the received data from the client.
     * <p>
     * This method is called by the server when a message is received from a
     * client. It can be a byte array or a string. The method checks if the
     * message is a valid JOSP message and then it calls the
     * {@link JODCommunication#processFromServiceMsg(String, JOSPPerm.Connection)}
     * method.
     *
     * @param client the client that sent the message.
     * @param data   the received data.
     * @return <code>true</code> if the data is processed, <code>false</code>
     * otherwise.
     */
    public boolean processData(ServerClient client, byte[] data) {
        return false;
    }

    public boolean processData(ServerClient client, String data) {
        log.debug(String.format("%s Processing data: %s", LUID(client), data));
        return communication.processFromServiceMsg(data, JOSPPerm.Connection.OnlyLocal);
    }


    // Clients' Connections

    /**
     * Return all server's {@link JODLocalClientInfo}.
     *
     * @return an unmodifiable array containing client's info.
     */
    public List<JODLocalClientInfo> getLocalClientsInfo() {
        return new ArrayList<>(localClients);
    }

    /**
     * Return the {@link JODLocalClientInfo} of the given <code>serviceId</code>.
     *
     * @param serviceId the required service's id.
     * @return the {@link JODLocalClientInfo} or <code>null</code> if given id
     * not found.
     */
    public JODLocalClientInfo getLocalClientInfo(String serviceId) {
        for (JODLocalClientInfo conn : localClients)
            if (conn.getFullSrvId().equals(serviceId))
                return conn;

        return null;
    }


    // Certificates mngm

    /**
     * Check if the local (server) certificate is full.
     *
     * @return true if the certificate used by current server contains the JOSP
     * Object's full ID.
     */
    public boolean isLocalCertificateFull() {
        return serverCertificateId != null && JOSPProtocol.isFullObjId(serverCertificateId);
    }

    /**
     * Check if the remote (client) certificate is full.
     *
     * @param fullSrvId the full JOSP Service id to check.
     * @return true if the given string is a valid full JOSP Service's ID.
     */
    public boolean isRemoteCertificateFull(String fullSrvId) {
        return JOSPProtocol.isFullSrvId(fullSrvId);
    }

    /**
     * @return the TrustManager used by the SSL context for the local server.
     */
    public AbsCustomTrustManager getTrustManager() {
        return trustManager;
    }


    // Internal local server's client listener

    private final ServerClientsListener remoteClientsListener = new ServerClientsListener() {

        @Override
        public void onConnect(Server server, ServerClient client) {
            onClientConnected(server, client);
        }

        @Override
        public void onDisconnect(Server server, ServerClient client) {
            onClientDisconnected(server, client);
        }

        @Override
        public void onFail(Server server, ServerClient client, String failMsg, Throwable exception) {
            onClientFailed(server, client, failMsg, exception);
        }

    };

    private void onClientConnected(Server server, ServerClient client) {
        processOnClientConnected(client);
    }

    private void onClientDisconnected(Server server, ServerClient client) {
        processOnClientDisconnected(client);
    }

    private void onClientFailed(Server server, ServerClient client, String failMsg, Throwable exception) {
        processOnClientFailed(client, failMsg, exception);
    }


    // Internal events processors

    private void processOnClientConnected(ServerClient client) {
        // Set connection LUID (local-unique-id)
        registerLUID(client);
        log.info(String.format("%s New connection from client '%s'", LUID(client), client.getLocalId()));

        // Send object's Id message to server
        if (!isLocalCertificateFull()) {
            String msg = objInfo.getObjId() + "\n";
            try {
                log.info(String.format("%s Sending message '%s' to service '%s' (via local)", LUID(client), msg.substring(0, msg.indexOf('\n')), client.getLocalId()));
                client.sendData("\n" + msg);
            } catch (PeerNotConnectedException | PeerStreamException e) {
                log.warn(String.format("%s Error on sending message '%s' to service (via local) because %s, discharge connection", LUID(client), msg.substring(0, msg.indexOf('\n')), e.getMessage()), e);
                errorDischargingClient(client);
                return;
            }
        }

        // Get/Wait service's fullId
        String fullSrvId;
        try {
            fullSrvId = getOrWaitFullSrvId(client);
            log.debug(String.format("%s Get JOSP Service's id `%s` from client '%s:%d'.", LUID(client), fullSrvId, client.getSocket().getInetAddress(), client.getSocket().getPort()));
        } catch (IOException e) {
            log.warn(String.format("%s %s, discharge connection", LUID(client), e.getMessage()));
            errorDischargingClient(client);
            return;
        }
        boolean useCertificatedId = fullSrvId.compareTo(client.getRemoteId()) == 0;

        log.info(String.format("%s Register new connection from client '%s' for '%s' service", LUID(client), client.getLocalId(), fullSrvId));
        synchronized (localClients) {

            // Wait for client connection
            if (!client.getState().isConnecting())
                JavaThreads.softSleep(100);
            if (!client.getState().isConnected())
                JavaThreads.softSleep(1000);
            if (!client.getState().isConnected()) {
                log.warn(String.format("%s Connection not established, discharge connection", LUID(client)));
                errorDischargingClient(client);
                return;
            }

            // Create new connection
            JODLocalClientInfo newConn = new DefaultJODLocalClientInfo(client, fullSrvId,
                    sslEnabled, sslSharingEnabled, useCertificatedId);

            // Check existing JODLocalClientInfo, if any (aka if JOSP Service already know)
            JODLocalClientInfo existingConn = getLocalClientInfo(newConn.getFullSrvId());
            if (existingConn == null) {
                // Add new JOSP Service
                log.debug(String.format("%s New JSL service '%s' connected", LUID(client), newConn.getFullSrvId()));
                localClients.add(newConn);
                try {
                    sendObjectPresentation(newConn);
                } catch (JODCommunication.ServiceNotConnected |
                         JODStructure.ParsingException e) {
                    log.warn(String.format("%s Error on sending object's presentation because %s, discharge client", LUID(client), e.getMessage()));
                    errorDischargingServiceClient(newConn, LUID(client));
                    return;
                }
                log.info(String.format("%s Added new JOSP Service '%s' remote client with connection '%s@%s:%d'", LUID(client), newConn.getFullSrvId(), newConn.getSecurityLevel(), newConn.getClientAddress(), newConn.getClientPort()));
                Events.registerLocalConn(String.format("Local JSL connected [%s]", newConn.getSecurityLevel()), newConn, client);

            } else if (!existingConn.isConnected()) {
                // Replace JOSP Service's connection (existing = disconnected)
                log.debug(String.format("%s Replacing JSL service '%s' connection", LUID(client), newConn.getFullSrvId()));
                localClients.remove(existingConn);
                localClients.add(newConn);
                try {
                    sendObjectPresentation(newConn);
                } catch (JODCommunication.ServiceNotConnected |
                         JODStructure.ParsingException e) {
                    log.warn(String.format("%s Error on sending object's presentation because %s, discharge client", LUID(client), e.getMessage()));
                    errorDischargingServiceClient(newConn, LUID(client));
                    return;
                }
                log.info(String.format("%s Replaced JSL service '%s'  client connection '%s' [SecurityLevel: %s]", LUID(client), newConn.getFullSrvId(), newConn.getClientFullAddress(), newConn.getSecurityLevel()));
                Events.registerLocalConn(String.format("Local JSL replaced connection [%s]", newConn.getSecurityLevel()), newConn, client);

            } else {
                // Discharge connection because JOSP Service already connected
                log.debug(String.format("%s Discharging JSL service '%s' connection", LUID(client), newConn.getFullSrvId()));
                try {
                    newConn.disconnectLocal();
                } catch (JODCommunication.LocalCommunicationException ex) {
                    log.warn(String.format("%s Error on discharging service `%s`'s client '%s:%d'.", LUID(client), newConn.getFullSrvId(), newConn.getClient().getSocket().getInetAddress(), newConn.getClient().getSocket().getPort()), ex);
                }
                log.info(String.format("%s Set JSL service '%s' backup connection '%s' [SecurityLevel: %s] because already connected", LUID(client), newConn.getFullSrvId(), newConn.getClientFullAddress(), newConn.getSecurityLevel()));
                Events.registerLocalConn(String.format("Local JOSP Service connection %s discharged because already connected", newConn.getSecurityLevel()), newConn, client);
            }
        }
    }

    private void processOnClientDisconnected(ServerClient client) {
        JODLocalClientInfo closedConn;
        synchronized (localClients) {
            // Get existing JODLocalClientInfo for disconnected client
            closedConn = getLocalConnectionByClient(client);
            if (closedConn == null) return;

            // Remove JODLocalClientInfo from localClients list
            localClients.remove(closedConn);
        }
        Events.registerLocalDisc("Local JSL disconnected", closedConn, client);
        log.info(String.format("%s Removed JSL service '%s' (%s) because %s", LUID(client), closedConn.getFullSrvId(), closedConn.getClientFullAddress(), closedConn.getClient().getDisconnectionReason()));
    }

    private void processOnClientFailed(ServerClient client, String failMsg, Throwable exception) {
        String clientStr = client != null ? client.getLocalId() : "N/A";
        if (exception.getCause() instanceof SSLPeerUnverifiedException) {
            log.warn(String.format("%s Error authenticate local client '%s' connection", LUID(client), clientStr));
            return;
        }
        log.warn(String.format("%s Error local client '%s' connection (%s) [%s] %s", LUID(client), clientStr, failMsg, exception.getClass().getSimpleName(), exception));
    }

    /**
     * Get the full service id.
     * <p>
     * First it try to get it from the client's connection's (as the remote id)
     * but if it's not a valid full/partial service id, then it waits for the
     * service id as first line received from the client.
     * <p>
     * The service id is contained into the remote id if it uses an SSL certificate
     * conform with the JOSP Security levels.
     *
     * @param client the client represent the service connected
     * @return the full service id
     * @throws IOException if some error occurs with network communication or
     *                     sockets configuration.
     */
    private String getOrWaitFullSrvId(ServerClient client) throws IOException {
        String fullSrvId;
        try { // SocketException
            fullSrvId = client.getRemoteId();
            // test fullSrvId
            if (!isRemoteCertificateFull(fullSrvId)) {
                // Wait for service's presentation message from the client
                Socket socket = client.getSocket();
                int tmpSoTimeout = socket.getSoTimeout();
                //socket.setSoTimeout(1000);

                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    fullSrvId = in.readLine();
                } catch (IOException e) {
                    throw new IOException(String.format("Error on reading service's id message because %s", e.getMessage()), e);
                }

                // test fullSrvId
                if (!isRemoteCertificateFull(fullSrvId))
                    throw new IOException(String.format("Error on parsing service's id message from client (Invalid ID '%s')", fullSrvId));

                socket.setSoTimeout(tmpSoTimeout);
            }
        } catch (SocketException e) {
            throw new IOException("Error on setting socket timeout, discharge client.", e);
        }
        return fullSrvId;
    }

    private void sendObjectPresentation(JODLocalClientInfo locConn) throws JODCommunication.ServiceNotConnected, JODStructure.ParsingException {
        log.debug(String.format("%s Send object presentation to JSL service %s", LUID(locConn.getClient()), locConn.getFullSrvId()));
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectInfoMsg(objInfo.getObjId(), objInfo.getObjName(), objInfo.getJODVersion(), objInfo.getOwnerId(), objInfo.getModel(), objInfo.getBrand(), objInfo.getLongDescr(), communication.getCloudConnection().getState().isConnected()), JOSPPerm.Type.State);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectStructMsg(objInfo.getObjId(), objInfo.getStructForJSL()), JOSPPerm.Type.State);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectPermsMsg(objInfo.getObjId(), permissions.getPermsForJSL()), JOSPPerm.Type.CoOwner);

        JOSPPerm.Type permType = permissions.getServicePermission(locConn.getSrvId(), locConn.getUsrId(), JOSPPerm.Connection.OnlyLocal);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createServicePermMsg(objInfo.getObjId(), permType, JOSPPerm.Connection.OnlyLocal), permType);
    }

    private void errorDischargingClient(ServerClient client) {
        try {
            client.disconnect();
        } catch (PeerDisconnectionException ex) {
            log.warn(String.format("%s Error on discharging client '%s'.", LUID(client), client.getLocalId()), ex);
        }
    }

    private void errorDischargingServiceClient(JODLocalClientInfo client, String luid) {
        localClients.remove(client);
        try {
            client.disconnectLocal();
        } catch (JODCommunication.LocalCommunicationException ex) {
            log.warn(String.format("%s Error on discharging service `%s`'s client '%s:%d'.", luid, client.getFullSrvId(), client.getClient().getSocket().getInetAddress(), client.getClient().getSocket().getPort()), ex);
        }
    }

    private JODLocalClientInfo getLocalConnectionByClient(ServerClient client) {
        for (JODLocalClientInfo conn : localClients)
            if (conn.getClient() == client)
                return conn;
        return null;
    }


    // LUID: Connection Local Unique ID

    private final Map<ServerClient, Integer> luids = new HashMap<>();
    private int lastLUID = -1;

    private void registerLUID(ServerClient client) {
        synchronized (luids) {
            luids.put(client, ++lastLUID);
        }
    }

    private String LUID(ServerClient client) {
        if (!luids.containsKey(client))
            return "[RC#: ----]";
        int luid = luids.get(client);
        return String.format("[RC#: %04x]", luid);
    }

}
