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

package com.robypomper.josp.jod;

import com.robypomper.java.JavaThreads;
import com.robypomper.java.JavaVersionUtils;
import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.clients.JCPClient2;
import com.robypomper.josp.defs.core.Versions;
import com.robypomper.josp.jod.comm.JODCommunication;
import com.robypomper.josp.jod.comm.JODCommunication_002;
import com.robypomper.josp.jod.events.Events;
import com.robypomper.josp.jod.events.JODEvents;
import com.robypomper.josp.jod.events.JODEvents_002;
import com.robypomper.josp.jod.executor.JODExecutorMngr;
import com.robypomper.josp.jod.executor.JODExecutorMngr_002;
import com.robypomper.josp.jod.history.JODHistory;
import com.robypomper.josp.jod.history.JODHistory_002;
import com.robypomper.josp.jod.objinfo.JODObjectInfo;
import com.robypomper.josp.jod.objinfo.JODObjectInfo_002;
import com.robypomper.josp.jod.permissions.JODPermissions;
import com.robypomper.josp.jod.permissions.JODPermissions_002;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.jod.structure.JODStructure_002;
import com.robypomper.josp.protocol.JOSPProtocol;
import com.robypomper.josp.states.StateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;

public class JOD_002 extends AbsJOD {

    // Class constants

    public static final String VERSION = BuildInfoJospJOD.current.buildVersion;
    private static final int MAX_INSTANCE_ID = 10000;


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JOD_002.class);


    // Constructor

    protected JOD_002(Settings settings,
                      JCPAPIsClientObj jcpClient,
                      JODObjectInfo objInfo,
                      JODStructure structure,
                      JODCommunication comm,
                      JODExecutorMngr executor,
                      JODPermissions permissions,
                      JODEvents events,
                      JODHistory history) {
        super(settings, jcpClient, objInfo, structure, comm, executor, permissions, events, history);
    }

    public static JOD instance(JODSettings_002 settings) throws JODStructure.ParsingException, JODCommunication.LocalCommunicationException, JCPClient2.AuthenticationException, StateException, JODPermissions.PermissionInvalidObjIdException {
        log.info("\n\n" + JavaVersionUtils.buildJavaVersionStr("John Object Daemon", VERSION));

        long start = new Date().getTime();

        JODEvents_002 events = new JODEvents_002(settings, null);
        Events.setInstance(events);

        String instanceId = Integer.toString(new Random().nextInt(MAX_INSTANCE_ID));
        log.info(String.format("Init JOD instance id '%s'", instanceId));
        Events.registerJODStart("Start sub-system creation", instanceId);

        JCPAPIsClientObj jcpClient = initJCPClient(settings);
        events.setJCPClient(jcpClient);

        JODObjectInfo_002 objInfo = new JODObjectInfo_002(settings, jcpClient, VERSION);

        JODExecutorMngr_002 executor = new JODExecutorMngr_002(settings, objInfo);

        JODHistory_002 history = new JODHistory_002(settings, jcpClient);

        JODStructure_002 structure = new JODStructure_002(objInfo, executor, history);

        JODPermissions_002 permissions = new JODPermissions_002(settings, objInfo, jcpClient);

        JODCommunication_002 comm = new JODCommunication_002(settings, objInfo, jcpClient, permissions, events, instanceId);

        try {
            comm.setStructure(structure);
        } catch (JODCommunication.StructureSetException ignore) {
            assert false;
        }
        try {
            permissions.setCommunication(comm);
            structure.setCommunication(comm);
        } catch (JODStructure.CommunicationSetException ignore) {
            assert false;
        }

        objInfo.setSystems(structure, executor, comm, permissions);

        long time = new Date().getTime() - start;
        Events.registerJODStart("End sub-system creation", time);

        return new JOD_002(settings, jcpClient, objInfo, structure, comm, executor, permissions, events, history);
    }

    private static JCPAPIsClientObj initJCPClient(JODSettings_002 settings) throws JCPClient2.AuthenticationException {
        JCPAPIsClientObj jcpClient = null;
        try {
            jcpClient = new JCPAPIsClientObj(
                    settings.getJCPUseSSL(),
                    settings.getJCPId(),
                    settings.getJCPSecret(),
                    settings.getJCPUrlAPIs(),
                    settings.getJCPUrlAuth(),
                    settings.getJCPRefreshTime());
        } catch (StateException ignore) {}
        assert jcpClient != null : "Can't throw exceptions during initialization";

        if (settings.getJCPConnect()) {
            try {
                jcpClient.connect();
                JavaThreads.softSleep(100); // wait to create the connection
                if (jcpClient.isConnected()) {
                    log.info("JCP Client initialized and connected successfully");
                    Events.registerJCPConnection("JCP Connected", jcpClient);
                }
                else
                    log.warn(String.format("JCP Client initialized but not connected, retry every %d seconds.", settings.getJCPRefreshTime()));

            } catch (JCPClient2.AuthenticationException e) {
                log.debug(String.format("Error on user authentication to the JCP %s", e.getMessage()), e);
                log.warn("Error on user authentication please check JCP client's id and secret in your object's configurations");
                log.warn("Current instance will NOT be able to communicate with the configured JCP, but direct communication still available.");

            } catch (StateException e) {
                assert false : "Exception StateException can't be thrown because connect() was call after client creation.";
            }
        } else
            log.info("JCP Client initialized but not connected as required by settings.");

        return jcpClient;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public String[] versionsJOSPProtocol() {
        return new String[]{JOSPProtocol.JOSP_PROTO_VERSION_2_0};
    }

    @Override
    public String[] versionsJCPAPIs() {
        return new String[]{Versions.VER_JCP_APIs_2_0};
    }

}
