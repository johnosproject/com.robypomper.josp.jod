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

package com.robypomper.josp.jod.comm;

import com.robypomper.comm.exception.PeerConnectionException;
import com.robypomper.comm.server.Server;
import com.robypomper.comm.server.ServerAbsSSL;
import com.robypomper.comm.server.ServerClient;
import com.robypomper.comm.server.ServerClientsListener;
import com.robypomper.comm.trustmanagers.AbsCustomTrustManager;
import com.robypomper.comm.trustmanagers.DynAddTrustManager;
import com.robypomper.java.JavaJKS;
import com.robypomper.java.JavaSSL;
import com.robypomper.java.JavaThreads;
import com.robypomper.josp.jod.events.Events;
import com.robypomper.josp.jod.objinfo.JODObjectInfo;
import com.robypomper.josp.jod.permissions.JODPermissions;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.protocol.JOSPPerm;
import com.robypomper.josp.protocol.JOSPProtocol;
import com.robypomper.josp.protocol.JOSPProtocol_ObjectToService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;


/**
 * Server that listen and process local client (JSL) connections.
 * <p>
 * This class provide a Cert Sharing Server (a server that allow to share
 * client and server certificates).
 */
public class JODLocalServer extends ServerAbsSSL {

    // Class constants

    public static final String KS_PASS = "123456";
    public static final String KS_DEF_PATH = "./configs/local_ks.jks";


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JODLocalServer.class);
    private final JODObjectInfo objInfo;
    private final JODCommunication communication;
    private final JODPermissions permissions;
    private final List<JODLocalClientInfo> localClients = new ArrayList<>();


    // Constructor

    public static JODLocalServer instantiate(JODCommunication communication, JODObjectInfo objInfo,
                                             JODPermissions permissions, int port,
                                             String ksPath, String ksPass, String ksAlias, String defKsPath) throws JavaJKS.LoadingException, JavaJKS.GenerationException, JavaSSL.GenerationException {
        AbsCustomTrustManager trustManager = new DynAddTrustManager();
        Certificate localCertificate = null;
        SSLContext sslCtx = null;

        String objHWId = objInfo.getObjId().substring(0,objInfo.getObjId().indexOf('-'));

        if (ksPath == null || ksPath.isEmpty()) {
            ksPath = defKsPath != null && !defKsPath.isEmpty() ? defKsPath : KS_DEF_PATH;   // name - the system-dependent filename
            ksPass = KS_PASS;
        }

        if (ksAlias == null || ksAlias.isEmpty()) {
            ksAlias = objHWId + "-LocalCert";
            //ksAlias = objInfo.getObjId() + "-LocalCert";
        }

        KeyStore clientKeyStore;
        if (new File(ksPath).exists()) {
            log.debug(String.format("Load keystore from '%s' path.", ksPath));
            clientKeyStore = JavaJKS.loadKeyStore(ksPath, ksPass);
        }
        else {
            // certificateId = objInfo.getObjId(), ksPass = KS_PASS, certAlias = objHwId + "-LocalCert"
            log.debug(String.format("Generate keystore and store to '%s' path.", ksPath));
            clientKeyStore = JavaJKS.generateKeyStore(objInfo.getObjId(), KS_PASS, ksAlias);
            JavaJKS.storeKeyStore(clientKeyStore, ksPath, KS_PASS);
        }

        log.debug(String.format("Extract local certificate with '%s' alias.", ksAlias));
        localCertificate = JavaJKS.extractCertificate(clientKeyStore, ksAlias);
        if (localCertificate == null)
            throw new JavaJKS.GenerationException(String.format("Certificate alias '%s' not found in '%s' keystore.", ksAlias, ksPath));
        sslCtx = JavaSSL.generateSSLContext(clientKeyStore, KS_PASS, trustManager);

        return new JODLocalServer(communication, objInfo, permissions, port, sslCtx, trustManager, localCertificate);
    }

    private JODLocalServer(JODCommunication communication, JODObjectInfo objInfo,
                           JODPermissions permissions, int port,
                           SSLContext sslCtx, AbsCustomTrustManager trustManager, Certificate localPublicCertificate) {
        super(objInfo.getObjId(), port, JOSPProtocol.JOSP_PROTO_NAME, sslCtx, trustManager, localPublicCertificate, JOSPProtocol.CLIENT_AUTH_REQUIRED, JOSPProtocol.CERT_SHARING_ENABLE);

        this.objInfo = objInfo;
        this.communication = communication;
        this.permissions = permissions;

        addListener(new ServerClientsListener() {

            @Override
            public void onConnect(Server server, ServerClient client) {
                onClientConnection(client);
            }

            @Override
            public void onDisconnect(Server server, ServerClient client) {
                onClientDisconnection(client);
            }

            @Override
            public void onFail(Server server, ServerClient client, String failMsg, Throwable exception) {
                onClientFail(server, client, failMsg, exception);
            }
        });
    }


    // Message methods

    @Override
    public boolean processData(ServerClient client, byte[] data) {
        return false;
    }

    @Override
    public boolean processData(ServerClient client, String data) {
        return communication.processFromServiceMsg(data, JOSPPerm.Connection.OnlyLocal);
    }


    // Connections mngm

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
    public JODLocalClientInfo getLocalConnectionByServiceId(String serviceId) {
        for (JODLocalClientInfo conn : localClients)
            if (conn.getFullSrvId().equals(serviceId))
                return conn;

        return null;
    }

    /**
     * Process the new cient connection.
     * <p>
     * Generate a new {@link JODLocalClientInfo} from the given {@link ServerClient}
     * and check if another client from the same instance is already known. If it
     * is then check if the already known client is still connected, then discard
     * the new client; else it relplace the old client with the new one.
     *
     * @param client the new client's info.
     */
    private void onClientConnection(ServerClient client) {
        JavaThreads.initAndStart(new OnClientConnectionRunnable(client), "LOCAL_JSL_CONNECTED", client.toString());
    }

    private class OnClientConnectionRunnable implements Runnable {

        private final ServerClient client;

        private OnClientConnectionRunnable(ServerClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            synchronized (localClients) {
                log.debug(String.format("Adding client connection '%s:%s'", client.getSocket().getInetAddress(), client.getSocket().getPort()));

                if (!client.getState().isConnecting())
                    JavaThreads.softSleep(100);

                if (!client.getState().isConnected())
                    JavaThreads.softSleep(1000);

                if (!client.getState().isConnected()) {
                    emitOnFail(client, String.format("Can't open connection with '%s' service, because connection not opened.", client), null);
                    return;
                }

                JODLocalClientInfo newConn = new DefaultJODLocalClientInfo(client);
                JODLocalClientInfo oldConn = getLocalConnectionByServiceId(newConn.getFullSrvId());
                if (oldConn == null) {
                    log.info(String.format("Added JSL '%s' service with client connection '%s'", newConn.getFullSrvId(), newConn.getClientFullAddress()));
                    localClients.add(newConn);
                    try {
                        sendObjectPresentation(newConn);
                    } catch (JODCommunication.ServiceNotConnected | JODStructure.ParsingException e) {
                        emitOnFail(client, String.format("Error on sending object's presentation to new service's client '%s', discharge client", client), e);
                        localClients.remove(newConn);
                        return;
                    }
                    Events.registerLocalConn("Local JSL connected", newConn, client);
                }
                else if (!oldConn.isConnected()) {
                    log.info(String.format("Replaced JSL '%s' service client connection '%s'", newConn.getFullSrvId(), newConn.getClientFullAddress()));
                    localClients.remove(oldConn);
                    localClients.add(newConn);
                    try {
                        sendObjectPresentation(newConn);
                    } catch (JODCommunication.ServiceNotConnected | JODStructure.ParsingException e) {
                        emitOnFail(client, String.format("Error on sending object's presentation to updated service's client '%s', discharge client", client), e);
                        localClients.remove(newConn);
                        return;
                    }
                    Events.registerLocalConn("Local JSL replaced connection", newConn, client);

                } else {
                    log.info(String.format("Discharge JSL '%s' service client connection '%s' because already connected with '%s'", newConn.getFullSrvId(), newConn.getClientFullAddress(), oldConn.getClientFullAddress()));
                    try {
                        newConn.disconnectLocal();

                    } catch (JODCommunication.LocalCommunicationException ignore) { /* client discharged, ignoring disconnection error */ }

                    Events.registerLocalConn("Local JSL discharged connection because JSL service already connected", newConn, client);
                }
            }
        }

    }

    private void sendObjectPresentation(JODLocalClientInfo locConn) throws JODCommunication.ServiceNotConnected, JODStructure.ParsingException {
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectInfoMsg(objInfo.getObjId(), objInfo.getObjName(), objInfo.getJODVersion(), objInfo.getOwnerId(), objInfo.getModel(), objInfo.getBrand(), objInfo.getLongDescr(), communication.getCloudConnection().getState().isConnected()), JOSPPerm.Type.Status);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectStructMsg(objInfo.getObjId(), objInfo.getStructForJSL()), JOSPPerm.Type.Status);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createObjectPermsMsg(objInfo.getObjId(), permissions.getPermsForJSL()), JOSPPerm.Type.CoOwner);

        JOSPPerm.Type permType = permissions.getServicePermission(locConn.getSrvId(), locConn.getUsrId(), JOSPPerm.Connection.OnlyLocal);
        communication.sendToSingleLocalService(locConn, JOSPProtocol_ObjectToService.createServicePermMsg(objInfo.getObjId(), permType, JOSPPerm.Connection.OnlyLocal), permType);
    }

    /**
     * Process the client disconnection.
     *
     * @param client the disconnected client's info.
     */
    private void onClientDisconnection(ServerClient client) {
        log.debug(String.format("Removing client connection '%s:%s'", client.getSocket().getInetAddress(), client.getSocket().getPort()));

        JODLocalClientInfo closedConn = new DefaultJODLocalClientInfo(client);
        JODLocalClientInfo locConn;
        synchronized (localClients) {
            locConn = getLocalConnectionByServiceId(closedConn.getFullSrvId());
            if (locConn == null) {
                assert false : "locConn can't be null";
                return;
            }
            if (locConn.getClientId().compareTo(closedConn.getClientId()) != 0) {
                log.info(String.format("Removed unused connection '%s' for service '%s'", locConn.getClientFullAddress(), locConn.getFullSrvId()));
                return;
            }
            localClients.remove(locConn);
        }
        Events.registerLocalDisc("Local JSL disconnected", locConn, client);
        log.info(String.format("Removed JSL '%s' service because closed his connection '%s'", locConn.getFullSrvId(), locConn.getClientFullAddress()));
    }

    private void onClientFail(Server server, ServerClient client, String failMsg, Throwable exception) {
        if (client == null) {
            if (exception instanceof PeerConnectionException) {
                Socket exClient = ((PeerConnectionException) exception).getSocket();
                log.debug(String.format("Error occurred during client '%s:%d' connection, client discharged", exClient.getInetAddress(), exClient.getPort()));
                log.debug(String.format("Error occurred during client '%s:%d' connection, details: [%s] %s", exClient.getInetAddress(), exClient.getPort(), exception.getClass().getSimpleName(), exception));
            }
        } else
            log.warn(String.format("Error local client '%s' connection (%s) [%s] %s", client, failMsg, exception.getClass().getSimpleName(), exception));
    }
}
