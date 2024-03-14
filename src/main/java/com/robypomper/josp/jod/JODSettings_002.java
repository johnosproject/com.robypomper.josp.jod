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

import com.robypomper.josp.protocol.JOSPPerm;
import com.robypomper.settings.DefaultSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class JODSettings_002 extends DefaultSettings implements JOD.Settings {

    //@formatter:off
    public static final String JCP_CONNECT              = "jcp.connect";
    public static final String JCP_CONNECT_DEF          = "true";
    /** Used as `connectionTimerDelaySeconds` value into DefaultJCPClient2. */
    public static final String JCP_REFRESH_TIME         = "jcp.client.refresh";
    public static final String JCP_REFRESH_TIME_DEF     = "30";
    public static final String JCP_SSL                  = "jcp.client.ssl";
    public static final String JCP_SSL_DEF              = "true";
    public static final String JCP_URL_APIS             = "jcp.url.apis";
    public static final String JCP_URL_DEF_APIS         = "api.johnosproject.org";
    public static final String JCP_URL_AUTH             = "jcp.url.auth";
    public static final String JCP_URL_DEF_AUTH         = "auth.johnosproject.org";
    public static final String JCP_CLIENT_ID            = "jcp.client.id";
    public static final String JCP_CLIENT_ID_DEF        = "";
    public static final String JCP_CLIENT_SECRET        = "jcp.client.secret";
    public static final String JCP_CLIENT_SECRET_DEF    = "";

    public static final String JODOBJ_NAME              = "jod.obj.name";
    public static final String JODOBJ_NAME_DEF          = "";
    public static final String JODOBJ_IDCLOUD           = "jod.obj.id_cloud";
    public static final String JODOBJ_IDCLOUD_DEF       = "";
    public static final String JODOBJ_IDHW              = "jod.obj.id_hw";
    public static final String JODOBJ_IDHW_DEF          = "";
    /**
     * Path to use as main dir for all relative paths, like the `jod.comm.local.ks.path`.
     * By default, it's an empty string that means to use the working directory as base path.
     */
    // TODO rename to JODOBJ_BASE_PATH
    public static final String JODOBJ_BASE_DIR          = "jod.obj.baseDir";
    public static final String JODOBJ_BASE_DIR_DEF      = "";

    public static final String JODPULLER_IMPLS          = "jod.executor_mngr.pullers";
    public static final String JODPULLER_IMPLS_DEF      = "";
    public static final String JODLISTENER_IMPLS        = "jod.executor_mngr.listeners";
    public static final String JODLISTENER_IMPLS_DEF    = "";
    public static final String JODEXECUTOR_IMPLS        = "jod.executor_mngr.executors";
    public static final String JODEXECUTOR_IMPLS_DEF    = "";

    public static final String JODSTRUCT_PATH           = "jod.structure.path";
    public static final String JODSTRUCT_PATH_DEF       = "configs/struct.jod";

    public static final String JODPERM_PATH             = "jod.permissions.path";
    public static final String JODPERM_PATH_DEF         = "configs/perms.jod";
    public static final String JODPERM_REFRESH          = "jod.permissions.refresh";
    public static final String JODPERM_REFRESH_DEF      = "30";
    public static final String JODPERM_GENSTARTEGY      = "jod.permissions.generation_strategy";
    public static final String JODPERM_GENSTARTEGY_DEF  = "standard";
    public static final String JODPERM_OWNER            = "jod.permissions.owner";
    public static final String JODPERM_OWNER_DEF        = JOSPPerm.WildCards.USR_ANONYMOUS_ID.toString();

    public static final String JODCOMM_LOCAL_ENABLED    = "jod.comm.local.enabled";
    public static final String JODCOMM_LOCAL_ENABLED_DEF = "true";
    /**
     * Path for the service's local keystore. It can be absolute or relative to `jod.obj.baseDir`.
     * By default, it's an empty string, that means it will generate his own certificate at first
     * object's connection and save it into `jod.comm.local.ks.defPath`.
     */
    public static final String JODCOMM_LOCAL_KS_PATH    = "jod.comm.local.ks.path";
    public static final String JODCOMM_LOCAL_KS_PATH_DEF = "";
    /**
     * Password for the service's local keystore. 
     * By default, it's an empty string that means no password.
     */
    public static final String JODCOMM_LOCAL_KS_PASS    = "jod.comm.local.ks.pass";
    public static final String JODCOMM_LOCAL_KS_PASS_DEF = "";
    /**
     * Alias of the certificate stored into the service's local keystore. 
     * By default, it's an empty string that means `$SRV_ID-LocalCert`.
     */
    public static final String JODCOMM_LOCAL_KS_ALIAS    = "jod.comm.local.ks.alias";
    public static final String JODCOMM_LOCAL_KS_ALIAS_DEF = "";
    /**
     * Default path for the service's local keystore, used when no path is specified
     * into `` property and a new keystore is generated.
     * By default, it's `{@value #JODCOMM_LOCAL_KS_DEF_PATH_DEF}`.
     * It can be also an empty string, then the `{@value com.robypomper.josp.jod.comm.JODLocalServer#KS_DEF_PATH}`
     * value will be used.
     */
    public static final String JODCOMM_LOCAL_KS_DEF_PATH = "jod.comm.local.ks.defPath";
    public static final String JODCOMM_LOCAL_KS_DEF_PATH_DEF = "./configs/local_ks.jks";
    public static final String JODCOMM_LOCAL_DISCOVERY  = "jod.comm.local.discovery";
    public static final String JODCOMM_LOCAL_DISCOVERY_DEF = "Auto";
    public static final String JODCOMM_LOCAL_PORT       = "jod.comm.local.port";
    public static final String JODCOMM_LOCAL_PORT_DEF   = "0";

    public static final String JODCOMM_CLOUD_ENABLED = "jod.comm.cloud.enabled";
    public static final String JODCOMM_CLOUD_ENABLED_DEF = "true";

    /**
     * If 'true' the history file will be retained in memory and any access to
     * the underling file will be done using the same instance. Otherwise, the
     * file is completely read every access.
     *
     * Default `false`.
     */
    public static final String JODHISTORY_KEEP_IN_MEMORY = "jod.history.keep_in_memory";
    public static final String JODHISTORY_KEEP_IN_MEMORY_DEF = "false";
    /**
     * Size of the history buffer.
     * <p>
     * When the buffer is full, then 'jod.history.buffer_release_size' items
     * are written to the file and removed from the buffer.
     * <p>
     * Default `250`.
     */
    public static final String JODHISTORY_BUFFER_SIZE = "jod.history.buffer_size";
    public static final String JODHISTORY_BUFFER_SIZE_DEF = "250";
    /**
     * Number of history's items to flush on the file when the buffer is full.
     * <p>
     * It must be a value lower than `jod.history.buffer_size`, otherwise it will
     * flush all history to the file.
     * <p>
     * Default `200`.
     */
    public static final String JODHISTORY_BUFFER_RELEASE_SIZE = "jod.history.buffer_release_size";
    public static final String JODHISTORY_BUFFER_RELEASE_SIZE_DEF = "200";
    /**
     * Size of the history file.
     * <p>
     * When the file is full, then 'jod.history.file_release_size' items
     * are deleted permanently and removed from the file.
     * <p>
     * Default `10000`.
     */
    public static final String JODHISTORY_FILE_SIZE = "jod.history.file_size";
    public static final String JODHISTORY_FILE_SIZE_DEF = "10000";

    /**
     * Number of history's items to delete from the file when it is full.
     * <p>
     * It must be a value lower than `jod.history.buffer_size`, otherwise it will
     * delete all items from the file.
     * <p>
     * Default `2000`.
     */
    public static final String JODHISTORY_FILE_RELEASE_SIZE = "jod.history.file_release_size";
    public static final String JODHISTORY_FILE_RELEASE_SIZE_DEF = "2000";
    /**
     * File path for history's items.
     */
    public static final String JODHISTORY_FILE_ARRAY_PATH = "jod.history.file_array";
    public static final String JODHISTORY_FILE_ARRAY_PATH_DEF = "cache/history.jbs";
    /**
     * File path for history's stats.
     */
    public static final String JODHISTORY_FILE_STATS_PATH = "jod.history.file_stats";
    public static final String JODHISTORY_FILE_STATS_PATH_DEF = "cache/history.jst";

    /**
     * If 'true' the events file will be retained in memory and any access to
     * the underling file will be done using the same instance. Otherwise, the
     * file is completely read every access.
     *
     * Default `false`.
     */
    public static final String JODEVENTS_KEEP_IN_MEMORY = "jod.events.keep_in_memory";
    public static final String JODEVENTS_KEEP_IN_MEMORY_DEF = "false";
    /**
     * Size of the events buffer.
     * <p>
     * When the buffer is full, then 'jod.events.buffer_release_size' items
     * are written to the file and removed from the buffer.
     * <p>
     * Default `250`.
     */
    public static final String JODEVENTS_BUFFER_SIZE = "jod.events.buffer_size";
    public static final String JODEVENTS_BUFFER_SIZE_DEF = "250";
    /**
     * Number of event's items to flush on the file when the buffer is full.
     * <p>
     * It must be a value lower than `jod.events.buffer_size`, otherwise it will
     * flush all events to the file.
     * <p>
     * Default `200`.
     */
    public static final String JODEVENTS_BUFFER_RELEASE_SIZE = "jod.events.buffer_release_size";
    public static final String JODEVENTS_BUFFER_RELEASE_SIZE_DEF = "200";
    /**
     * Size of the events file.
     * <p>
     * When the file is full, then 'jod.events.file_release_size' items
     * are deleted permanently and removed from the file.
     * <p>
     * Default `10000`.
     */
    public static final String JODEVENTS_FILE_SIZE = "jod.events.file_size";
    public static final String JODEVENTS_FILE_SIZE_DEF = "10000";
    /**
     * Number of event's items to delete from the file when it is full.
     * <p>
     * It must be a value lower than `jod.events.file_size`, otherwise it will
     * delete all items from the file.
     * <p>
     * Default `2000`.
     */
    public static final String JODEVENTS_FILE_RELEASE_SIZE = "jod.events.file_release_size";
    public static final String JODEVENTS_FILE_RELEASE_SIZE_DEF = "2000";
    /**
     * File path for events' items.
     */
    public static final String JODEVENTS_FILE_ARRAY_PATH = "jod.events.file_array";
    public static final String JODEVENTS_FILE_ARRAY_PATH_DEF = "cache/events.jbe";
    /**
     * File path for events' stats.
     */
    public static final String JODEVENTS_FILE_STATS_PATH = "jod.events.file_stats";
    public static final String JODEVENTS_FILE_STATS_PATH_DEF = "cache/events.jst";


    //@formatter:on


    // Constructor

    public static JOD.Settings instance(File file) throws IOException {
        return new JODSettings_002(file);
    }

    public static JOD.Settings instance(Map<String, Object> properties) {
        return new JODSettings_002(properties);
    }

    public JODSettings_002(File file) throws IOException {
        super(file);
    }

    public JODSettings_002(Map<String, Object> properties) {
        super(properties);
    }


    // JCP Client

    public boolean getJCPConnect() {
        return getBoolean(JCP_CONNECT, JCP_CONNECT_DEF);
    }

    public int getJCPRefreshTime() {
        return getInt(JCP_REFRESH_TIME, JCP_REFRESH_TIME_DEF);
    }

    public boolean getJCPUseSSL() {
        return getBoolean(JCP_SSL, JCP_SSL_DEF);
    }

    public String getJCPUrlAPIs() {
        return getString(JCP_URL_APIS, JCP_URL_DEF_APIS);
    }

    public String getJCPUrlAuth() {
        return getString(JCP_URL_AUTH, JCP_URL_DEF_AUTH);
    }

    public String getJCPId() {
        return getString(JCP_CLIENT_ID, JCP_CLIENT_ID_DEF);
    }

    public String getJCPSecret() {
        return getString(JCP_CLIENT_SECRET, JCP_CLIENT_SECRET_DEF);
    }


    // Object info

    public String getObjName() {
        return getString(JODOBJ_NAME, JODOBJ_NAME_DEF);
    }

    public void setObjName(String objName) {
        store(JODOBJ_NAME, objName, true);
    }

    public String getObjIdCloud() {
        return getString(JODOBJ_IDCLOUD, JODOBJ_IDCLOUD_DEF);
    }

    public void setObjIdCloud(String objId) {
        store(JODOBJ_IDCLOUD, objId, true);
    }

    public String getObjIdHw() {
        return getString(JODOBJ_IDHW, JODOBJ_IDHW_DEF);
    }

    public void setObjIdHw(String objIdHw) {
        store(JODOBJ_IDHW, objIdHw, true);
    }

    // TODO make getObjBaseDir() method return a File instance
    public String getObjBaseDir() {
        return getString(JODOBJ_BASE_DIR, JODOBJ_BASE_DIR_DEF);
    }


    // Executor Manager

    public String getJODPullerImpls() {
        return getString(JODPULLER_IMPLS, JODPULLER_IMPLS_DEF);
    }

    public String getJODListenerImpls() {
        return getString(JODLISTENER_IMPLS, JODLISTENER_IMPLS_DEF);
    }

    public String getJODExecutorImpls() {
        return getString(JODEXECUTOR_IMPLS, JODEXECUTOR_IMPLS_DEF);
    }


    // Structure

    public File getStructurePath() {
        return getFile(JODSTRUCT_PATH, JODSTRUCT_PATH_DEF);
    }


    // Permissions

    public File getPermissionsPath() {
        return getFile(JODPERM_PATH, JODPERM_PATH_DEF);
    }

    public int getPermissionsRefreshTime() {
        return getInt(JODPERM_REFRESH, JODPERM_REFRESH_DEF);
    }

    public JOSPPerm.GenerateStrategy getPermissionsGenerationStrategy() {
        String val = getString(JODPERM_GENSTARTEGY, JODPERM_GENSTARTEGY_DEF);
        return JOSPPerm.GenerateStrategy.valueOf(val.toUpperCase());
    }

    /**
     * Object startup
     * - init jod
     * - read from local configs                             when: owner set
     * on error                                            when: owner not set
     * - set default anonymous owner on local configs
     * - read from local configs
     * - start jod
     * <p>
     * <p>
     * Owner set from service 2 object
     * - store on local configs
     * - set owner on cloud
     * <p>
     * Owner set from service 2 object via cloud
     * - jcp: store on cloud object's temporary properties
     * - jcp: set owner on object
     * - store on local configs
     * - set owner on cloud
     */
    public String getOwnerId() {
        return getString(JODPERM_OWNER, JODPERM_OWNER_DEF);
    }

    public void setOwnerId(String ownerId) {
        store(JODPERM_OWNER, ownerId, true);
    }


    // Communication

    public boolean getLocalEnabled() {
        return getBoolean(JODCOMM_LOCAL_ENABLED, JODCOMM_LOCAL_ENABLED_DEF);
    }

    // TODO make getLocalKeyStorePath() method return a File instance
    public String getLocalKeyStorePath() {
        String path = getString(JODCOMM_LOCAL_KS_PATH, JODCOMM_LOCAL_KS_PATH_DEF);
        if (!Paths.get(path).isAbsolute())
            path = Paths.get(getObjBaseDir(), path).toString();
        return path;
    }

    public String getLocalKeyStorePass() {
        return getString(JODCOMM_LOCAL_KS_PASS, JODCOMM_LOCAL_KS_PASS_DEF);
    }

    public String getLocalKeyStoreAlias() {
        return getString(JODCOMM_LOCAL_KS_ALIAS, JODCOMM_LOCAL_KS_ALIAS_DEF);
    }

    // TODO make getLocalKeyStorePath() method return a File instance
    public String getLocalKeyStoreDefaultPath() {
        String path = getString(JODCOMM_LOCAL_KS_DEF_PATH, JODCOMM_LOCAL_KS_DEF_PATH_DEF);
        if (!Paths.get(path).isAbsolute())
            path = Paths.get(getObjBaseDir(), path).toString();
        return path;
    }

    public String getLocalDiscovery() {
        return getString(JODCOMM_LOCAL_DISCOVERY, JODCOMM_LOCAL_DISCOVERY_DEF);
    }

    public int getLocalServerPort() {
        return getInt(JODCOMM_LOCAL_PORT, JODCOMM_LOCAL_PORT_DEF);
    }

    public boolean getCloudEnabled() {
        return getBoolean(JODCOMM_CLOUD_ENABLED, JODCOMM_CLOUD_ENABLED_DEF);
    }


    // History

    public boolean getHistoryKeepInMemory() {
        return getBoolean(JODHISTORY_KEEP_IN_MEMORY, JODHISTORY_KEEP_IN_MEMORY_DEF);
    }

    public int getHistoryBufferSize() {
        return getInt(JODHISTORY_BUFFER_SIZE, JODHISTORY_BUFFER_SIZE_DEF);
    }

    public int getHistoryBufferReleaseSize() {
        return getInt(JODHISTORY_BUFFER_RELEASE_SIZE, JODHISTORY_BUFFER_RELEASE_SIZE_DEF);
    }

    public int getHistoryFileSize() {
        return getInt(JODHISTORY_FILE_SIZE, JODHISTORY_FILE_SIZE_DEF);
    }

    public int getHistoryFileReleaseSize() {
        return getInt(JODHISTORY_FILE_RELEASE_SIZE, JODHISTORY_FILE_RELEASE_SIZE_DEF);
    }

    public File getHistoryFileArrayPath() {
        return getFile(JODHISTORY_FILE_ARRAY_PATH, JODHISTORY_FILE_ARRAY_PATH_DEF);
    }

    public File getHistoryFileStatsPath() {
        return getFile(JODHISTORY_FILE_STATS_PATH, JODHISTORY_FILE_STATS_PATH_DEF);
    }


    // Events

    public boolean getEventsKeepInMemory() {
        return getBoolean(JODEVENTS_KEEP_IN_MEMORY, JODEVENTS_KEEP_IN_MEMORY_DEF);
    }

    public int getEventsBufferSize() {
        return getInt(JODEVENTS_BUFFER_SIZE, JODEVENTS_BUFFER_SIZE_DEF);
    }

    public int getEventsBufferReleaseSize() {
        return getInt(JODEVENTS_BUFFER_RELEASE_SIZE, JODEVENTS_BUFFER_RELEASE_SIZE_DEF);
    }

    public int getEventsFileSize() {
        return getInt(JODEVENTS_FILE_SIZE, JODEVENTS_FILE_SIZE_DEF);
    }

    public int getEventsFileReleaseSize() {
        return getInt(JODEVENTS_FILE_RELEASE_SIZE, JODEVENTS_FILE_RELEASE_SIZE_DEF);
    }

    public File getEventsFileArrayPath() {
        return getFile(JODEVENTS_FILE_ARRAY_PATH, JODEVENTS_FILE_ARRAY_PATH_DEF);
    }

    public File getEventsFileStatsPath() {
        return getFile(JODEVENTS_FILE_STATS_PATH, JODEVENTS_FILE_STATS_PATH_DEF);
    }

}
