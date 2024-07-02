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

package com.robypomper.josp.jod.history;

import com.robypomper.java.JavaJSONArrayToFile;
import com.robypomper.josp.callers.apis.core.objects.Caller20;
import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.clients.JCPClient2;
import com.robypomper.josp.jod.JODSettings_002;
import com.robypomper.josp.jod.events.CloudStats;
import com.robypomper.josp.jod.structure.JODComponent;
import com.robypomper.josp.jod.structure.JODStateUpdate;
import com.robypomper.josp.protocol.HistoryLimits;
import com.robypomper.josp.protocol.JOSPHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Un buffer locale per tutti i componenti
 */
@SuppressWarnings("Convert2Lambda")
public class JODHistory_002 implements JODHistory {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JODHistory_002.class);
    private final JODSettings_002 locSettings;
    private JCPAPIsClientObj jcpClient;
    private Caller20 apiObjsCaller;
    private final StatusHistoryArray histories;
    private final CloudStats stats;
    private boolean isSyncing = false;


    // Constructor

    /**
     * Create new History system.
     *
     * @param settings  the JOD settings.
     * @param jcpClient the JCP client.
     */
    public JODHistory_002(JODSettings_002 settings, JCPAPIsClientObj jcpClient) {
        this.locSettings = settings;
        setJCPClient(jcpClient);

        File historiesFile = locSettings.getHistoryFileArrayPath();
        File statsFile = locSettings.getHistoryFileStatsPath();

        // Load statuses
        StatusHistoryArray tmpHistories = null;
        if (!historiesFile.getParentFile().exists()) {
            if (!historiesFile.getParentFile().mkdirs())
                log.warn("Error on creating History file's dir.");
        } else if (historiesFile.exists())
            try {
                tmpHistories = new StatusHistoryArray(historiesFile,
                        locSettings.getHistoryKeepInMemory(),
                        locSettings.getHistoryBufferSize(),
                        locSettings.getHistoryBufferReleaseSize(),
                        locSettings.getHistoryFileSize(),
                        locSettings.getHistoryFileReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on loading History file.", e);
            }

        // Load stats
        CloudStats tmpStats = null;
        if (!statsFile.getParentFile().exists()) {
            if (!statsFile.getParentFile().mkdirs())
                log.warn("Error on creating History stats file's dir.");
        }
        if (statsFile.exists()) {
            try {
                tmpStats = new CloudStats(statsFile);
            } catch (IOException e) {
                log.warn("Error on loading History stats file.", e);
            }
        }

        //  if  stats NOT readable  and  statuses NOT readable
        if (tmpStats == null && tmpHistories == null) {
            // generate stats
            try {
                if (statsFile.exists())
                    if (!statsFile.delete())
                        log.warn("Error on deleting History stats file.");
                tmpStats = new CloudStats(statsFile);
                tmpStats.write();
            } catch (IOException e) {
                log.warn("Error on creating History stats file.", e);
            }
            // generate histories
            if (!historiesFile.delete())
                log.warn("Error on deleting History file.");
            try {
                tmpHistories = new StatusHistoryArray(historiesFile,
                        locSettings.getHistoryKeepInMemory(),
                        locSettings.getHistoryBufferSize(),
                        locSettings.getHistoryBufferReleaseSize(),
                        locSettings.getHistoryFileSize(),
                        locSettings.getHistoryFileReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on creating History file.", e);
            }

            //  else if  stats NOT readable  and  statuses readable
        } else if (tmpStats == null) {      // tmpHistories != null ALWAYS true
            // statuses already loaded
            // generate stats from statuses
            try {
                if (statsFile.exists())
                    if (!statsFile.delete())
                        log.warn("Error on deleting History stats file.");
                tmpStats = new CloudStats(statsFile);
                tmpStats.write();
                tmpStats.setLastUploaded(-1, 0);
            } catch (IOException e) {
                log.warn("Error on creating History stats file.", e);
            }
            //      ...

            //  else if  stats readable  and  statuses NOT readable
        } else if (tmpHistories == null) {      // tmpStats != null ALWAYS true
            // stats already loaded
            //      generate statuses _from stats
            if (!historiesFile.delete())
                log.warn("Error on deleting History file.");
            try {
                tmpHistories = new StatusHistoryArray(historiesFile,
                        locSettings.getHistoryKeepInMemory(),
                        locSettings.getHistoryBufferSize(),
                        locSettings.getHistoryBufferReleaseSize(),
                        locSettings.getHistoryFileSize(),
                        locSettings.getHistoryFileReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on creating History file.", e);
            }
            //          statuses non può essere generato
            //          aggiornare stats a: buffered statuses cancellati

            //  else if  stats readable  and  statuses readable
            // } else if (tmpStats != null && tmpStatuses != null) {
            // stats already loaded
            // statuses already loaded
        }

        assert tmpHistories != null : "StatusHistoryArray can't be null";
        assert tmpStats != null : "HistoryCloudStats can't be null";

        histories = tmpHistories;
        histories.registerObserver(storageObserver);
        stats = tmpStats;

        log.info("Initialized JODHistory instance");
        log.debug(String.format("                                   History buffered %d statuses on file %d", histories.countBuffered(), histories.countFile()));
        log.debug(String.format("                                   History stats lastStored: %d lastUploaded: %d", stats.getLastStored(), stats.getLastUploaded()));
    }


    // Register new status

    @Override
    public void register(JODComponent comp, JODStateUpdate update) {
        synchronized (histories) {
            long newId = stats.getRegisteredCount() + 1;
            JOSPHistory s = new JOSPHistory(newId, comp.getPath().getString(), comp.getType(), new Date(), update.encode());
            histories.append(s);

            // Update stats
            stats.setLastRegistered(s.getId(), 1);
            stats.writeIgnoreExceptions();
        }

        if (isSyncing)
            sync();
    }

    private void sync() {
        if (stats.getLastUploaded() == stats.getLastStored()) return;
        if (jcpClient == null || !jcpClient.isConnected()) return;

        List<JOSPHistory> toUpload;
        synchronized (histories) {
            try {
                toUpload = histories.getById(stats.getLastUploaded() != -1 ? stats.getLastUploaded() : null, stats.getLastStored());
                if (stats.getLastUploaded() != -1 && toUpload.size() > 1)
                    toUpload.remove(0);

            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn(String.format("Can't read history from file (CloudStats values lastUpd: %d; lastStored: %d) (%s)", stats.getLastUploaded(), stats.getLastStored(), e));
                return;
            }

            if (toUpload.isEmpty()) {
                log.debug(String.format("No statuses found to uploads (CloudStats values lastUpd: %d; lastStored: %d", stats.getLastUploaded(), stats.getLastStored()));
                return;
            }

            log.debug(String.format("Upload from %d to %d (%d statuses)", toUpload.get(0).getId(), toUpload.get(toUpload.size() - 1).getId(), toUpload.size()));
            for (JOSPHistory e : toUpload)
                log.trace(String.format("- event[%d] %s", e.getId(), e.getPayload()));

            try {
                apiObjsCaller.postHistory(JOSPHistory.toHistoryMessagees(toUpload));

            } catch (JCPClient2.ConnectionException |
                     JCPClient2.AuthenticationException |
                     JCPClient2.ResponseException |
                     JCPClient2.RequestException e) {
                log.warn(String.format("Can't upload statuses history (CloudStats values lastUpd: %d; lastStored: %d) (%s)", stats.getLastUploaded(), stats.getLastStored(), e));
                return;
            }

            stats.setLastUploaded(toUpload.get(toUpload.size() - 1).getId(), toUpload.size());
            stats.writeIgnoreExceptions();
        }
    }

    @Override
    public void startCloudSync() {
        isSyncing = true;
        log.info("Start statuses sync to cloud");
        log.debug(String.format("Events buffered %d statuses on file %d", histories.countBuffered(), histories.countFile()));
        log.debug(String.format("Events stats lastStored: %d lastUploaded: %d", stats.getLastStored(), stats.getLastUploaded()));

        sync();
    }

    @Override
    public void stopCloudSync() {
        synchronized (histories) {
            isSyncing = false;
            try {
                histories.storeCache();

                log.info("Stop history sync to cloud");
                log.debug(String.format("Events buffered %d statuses on file %d", histories.countBuffered(), histories.countFile()));
                log.debug(String.format("Events stats lastStored: %d lastUploaded: %d", stats.getLastStored(), stats.getLastUploaded()));

            } catch (JavaJSONArrayToFile.FileException ignore) {
                assert false;
            }
        }
    }


    // Getters and setters

    @Override
    public List<JOSPHistory> getHistoryStatus(JODComponent comp, HistoryLimits limits) {
        JavaJSONArrayToFile.Filter<JOSPHistory> filter = new JavaJSONArrayToFile.Filter<JOSPHistory>() {
            @Override
            public boolean accepted(JOSPHistory o) {
                return o.getCompPath().equalsIgnoreCase(comp.getPath().getString());
            }
        };

        if (HistoryLimits.isLatestCount(limits))
            return histories.tryLatest(filter, limits.getLatestCount());

        if (HistoryLimits.isAncientCount(limits))
            return histories.tryAncient(filter, limits.getAncientCount());

        if (HistoryLimits.isIDRange(limits))
            return histories.tryById(filter, limits.getFromIDOrDefault(), limits.getToIDOrDefault());

        if (HistoryLimits.isDateRange(limits))
            return histories.tryByDate(filter, limits.getFromDateOrDefault(), limits.getToDateOrDefault());

        if (HistoryLimits.isPageRange(limits)) {
            try {
                List<JOSPHistory> all = histories.filterAll(filter);
                int page = limits.getPageNumOrDefault();
                int size = limits.getPageSizeOrDefault();

                int posStart = page * size;
                if (posStart > all.size() - 1)
                    return new ArrayList<>();

                int posEnd = (page * size) + size - 1;
                if (posEnd >= all.size() - 1)
                    posEnd = all.size() - 1;
                return all.subList(posStart, posEnd + 1);

            } catch (JavaJSONArrayToFile.FileException e) {
                return new ArrayList<>();
            }
        }

        try {
            return histories.filterAll(filter);

        } catch (JavaJSONArrayToFile.FileException e) {
            return new ArrayList<>();
        }
    }

    public void setJCPClient(JCPAPIsClientObj jcpClient) {
        if (jcpClient == null) return;

        this.jcpClient = jcpClient;
        this.jcpClient.addConnectionListener(jcpConnectListener);
        this.apiObjsCaller = new Caller20(jcpClient);
    }


    // Mngm methods

    @Override
    public void storeCache() throws IOException {
        histories.storeCache();
    }


    // JCP Client listeners

    private final JCPClient2.ConnectionListener jcpConnectListener = new JCPClient2.ConnectionListener() {

        @Override
        public void onConnected(JCPClient2 jcpClient) {
            sync();
        }

        @Override
        public void onConnectionFailed(JCPClient2 jcpClient, Throwable t) {
        }

        @Override
        public void onAuthenticationFailed(JCPClient2 jcpClient, Throwable t) {
        }

        @Override
        public void onDisconnected(JCPClient2 jcpClient) {
        }

    };

    private final JavaJSONArrayToFile.Observer<JOSPHistory> storageObserver = new JavaJSONArrayToFile.Observer<JOSPHistory>() {
        @Override
        public void onAdded(List<JOSPHistory> items) {
            // Nothing to do, already done in register method
        }

        @Override
        public void onFlushed(List<JOSPHistory> items, boolean auto) {
            // Update stats
            stats.setLastStored(items.get(items.size() - 1).getId(), items.size());
            stats.writeIgnoreExceptions();
        }

        @Override
        public void onRemoved(List<JOSPHistory> items, boolean auto) {
            int countLost = 0;
            if (stats.getLastUploaded() == -1) countLost = items.size();
            else {
                for (JOSPHistory e : items)
                    if (e.getId() > stats.getLastUploaded())
                        countLost++;
            }
            // Update stats
            stats.setLastDelete(items.get(items.size() - 1).getId(), items.size(), countLost);
            stats.writeIgnoreExceptions();
        }
    };

}
