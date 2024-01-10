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

package com.robypomper.josp.jod.events;

import com.robypomper.java.JavaJSONArrayToFile;
import com.robypomper.josp.callers.apis.core.events.Caller20;
import com.robypomper.josp.clients.JCPAPIsClientObj;
import com.robypomper.josp.clients.JCPClient2;
import com.robypomper.josp.jod.JODSettings_002;
import com.robypomper.josp.protocol.HistoryLimits;
import com.robypomper.josp.protocol.JOSPEvent;
import com.robypomper.josp.types.josp.AgentType;
import com.robypomper.josp.types.josp.EventType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * This is the JOD Events implementation.
 * <p>
 * ...
 *
 * TODO make JODEvents non singleton but accessible from JOD.getEvents()
 */
@SuppressWarnings("Convert2Lambda")
public class JODEvents_002 implements JODEvents {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JODEvents_002.class);
    private final JODSettings_002 locSettings;
    private JCPAPIsClientObj jcpClient;
    private Caller20 apiEventsCaller;
    private final EventsArray events;
    private final CloudStats stats;
    private boolean isSyncing = false;


    // Constructor

    /**
     * Create new Events system.
     * <p>
     * This constructor create an instance of {@link JODEvents} and (if doesn't
     * exist) create local events files.
     *
     * @param settings  the JOD settings.
     * @param jcpClient the JCP client.
     */
    public JODEvents_002(JODSettings_002 settings, JCPAPIsClientObj jcpClient) {
        this.locSettings = settings;
        setJCPClient(jcpClient);

        File eventsFile = locSettings.getEventsFileArrayPath();
        File statsFile = locSettings.getEventsFileStatsPath();

        EventsArray tmpEvents = null;
        if (!eventsFile.getParentFile().exists()) {
            if (!eventsFile.getParentFile().mkdirs())
                log.warn("Error on creating Events file's dir.");
        } else if (eventsFile.exists())
            try {
                tmpEvents = new EventsArray(eventsFile,
                        locSettings.getEventsKeepInMemory(),
                        locSettings.getEventsBufferSize(),
                        locSettings.getEventsBufferReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on loading Events file.", e);
            }

        CloudStats tmpStats = null;
        if (!statsFile.getParentFile().exists())
            if (!statsFile.getParentFile().mkdirs())
                log.warn("Error on creating Events stats file's dir.");
        else if (statsFile.exists()) {
            try {
                tmpStats = new CloudStats(statsFile);
            } catch (IOException e) {
                log.warn("Error on creating Events stats file.", e);
            }
        }

        //  if  stats NOT readable  and  events NOT readable
        if (tmpStats == null && tmpEvents == null) {
            // generate stats
            try {
                if (statsFile.exists())
                    if (!statsFile.delete())
                        log.warn("Error on deleting Events stats file.");
                tmpStats = new CloudStats(statsFile);
                tmpStats.store();
            } catch (IOException e) {
                log.warn("Error on creating Events stats file.", e);
            }
            // generate events
            if (!eventsFile.delete())
                log.warn("Error on deleting Events file.");
            try {
                tmpEvents = new EventsArray(eventsFile,
                        locSettings.getEventsKeepInMemory(),
                        locSettings.getEventsBufferSize(),
                        locSettings.getEventsBufferReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on creating Events file.", e);
            }

        //  else if  stats NOT readable  and  events readable
        } else if (tmpStats == null) {      // tmpEvents != null ALWAYS true
            // events already loaded
            // generate stats from events
            try {
                if (statsFile.exists())
                    if (!statsFile.delete())
                        log.warn("Error on deleting Events stats file.");
                tmpStats = new CloudStats(statsFile);
                tmpStats.store();
                tmpStats.lastUploaded = 0;
            } catch (IOException e) {
                log.warn("Error on creating Events stats file.", e);
            }
            //      ...

        //  else if  stats readable  and  events NOT readable
        } else if (tmpEvents == null) {      // tmpStats != null ALWAYS true
            // stats already loaded
            //      generate events _from stats
            if (!eventsFile.delete())
                log.warn("Error on deleting Events file.");
            try {
                tmpEvents = new EventsArray(eventsFile,
                        locSettings.getEventsKeepInMemory(),
                        locSettings.getEventsBufferSize(),
                        locSettings.getEventsBufferReleaseSize());
            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn("Error on creating Events file.", e);
            }
            //          events non può essere generato
            //          aggiornare stats a: buffered events cancellati

            //  else if  stats readable  and  events readable
        // } else if (tmpStats != null && tmpEvents != null) {
        //     stats already loaded
        //     events already loaded
        }

        events = tmpEvents;
        stats = tmpStats;

        assert events != null: "EventsArray can't be null";
        assert stats != null: "CloudStats can't be null";

        log.info("Initialized JODEvents instance");
        log.debug(String.format("                                   Events buffered %d events on file %d", events.countBuffered(), events.countFile()));
        log.debug(String.format("                                   Events stats lastStored: %d lastUploaded: %d", stats.lastStored, stats.lastUploaded));
    }


    // Register new event

    @Override
    public void register(EventType type, String phase, String payload) {
        register(type, phase, payload, null);
    }

    @Override
    public void register(EventType type, String phase, String payload, Throwable error) {
        synchronized (events) {
            long newId = events.count() + 1;
            String srcId = locSettings.getObjIdCloud();
            String errorPayload = null;
            if (error != null)
                errorPayload = String.format("{\"type\": \"%s\", \"msg\": \"%s\", \"stack\": \"%s\"}", error.getClass().getSimpleName(), error.getMessage(), Arrays.toString(error.getStackTrace()));
            JOSPEvent e = new JOSPEvent(newId, type, srcId, AgentType.Obj, new Date(), phase, payload, errorPayload);
            events.append(e);
            stats.lastStored = e.getId();
            stats.storeIgnoreExceptions();
        }

        if (isSyncing)
            sync();
    }

    private void sync() {
        if (stats.lastUploaded == stats.lastStored) return;
        if (jcpClient == null || !jcpClient.isConnected()) return;

        List<JOSPEvent> toUpload;
        synchronized (events) {
            try {
                toUpload = events.getById(stats.lastUploaded != -1 ? stats.lastUploaded : null, stats.lastStored);
                if (stats.lastUploaded != -1 && toUpload.size() > 1) toUpload.remove(0);

            } catch (JavaJSONArrayToFile.FileException e) {
                log.warn(String.format("Can't read events from file (CloudStats values lastUpd: %d; lastStored: %d) (%s)", stats.lastUploaded, stats.lastStored, e));
                return;
            }

            if (toUpload.isEmpty()) {
                log.debug(String.format("No events found to uploads (CloudStats values lastUpd: %d; lastStored: %d", stats.lastUploaded, stats.lastStored));
                return;
            }

            log.debug(String.format("Upload from %d to %d (%d events)", toUpload.get(0).getId(), toUpload.get(toUpload.size() - 1).getId(), toUpload.size()));
            for (JOSPEvent e : toUpload)
                log.trace(String.format("                                   - event[%d] %s", e.getId(), e.getPayload()));

            try {
                apiEventsCaller.uploadEvents(JOSPEvent.toEvents(toUpload));

            } catch (JCPClient2.ConnectionException | JCPClient2.AuthenticationException | JCPClient2.ResponseException | JCPClient2.RequestException e) {
                log.warn(String.format("Can't upload events (CloudStats values lastUpd: %d; lastStored: %d) (%s)", stats.lastUploaded, stats.lastStored, e));
                return;
            }

            stats.uploaded += toUpload.size();
            stats.lastUploaded = toUpload.get(toUpload.size() - 1).getId();
            stats.storeIgnoreExceptions();
        }
    }

    @Override
    public void startCloudSync() {
        isSyncing = true;
        log.info("Start events sync to cloud");
        log.debug(String.format("                                   Events buffered %d events on file %d", events.countBuffered(), events.countFile()));
        log.debug(String.format("                                   Events stats lastStored: %d lastUploaded: %d", stats.lastStored, stats.lastUploaded));

        sync();
    }

    @Override
    public void stopCloudSync() {
        synchronized (events) {
            isSyncing = false;
            try {
                int pre = events.countBuffered();
                events.storeCache();
                int post = events.countBuffered();
                log.debug(String.format("Stored %d events to file", pre - post));

                log.info("Stop event sync to cloud");
                log.debug(String.format("                                   Events buffered %d events on file %d", events.countBuffered(), events.countFile()));
                log.debug(String.format("                                   Events stats lastStored: %d lastUploaded: %d", stats.lastStored, stats.lastUploaded));

            } catch (JavaJSONArrayToFile.FileException ignore) {
                assert false;
            }
        }
    }


    // Getters and setters

    @Override
    public List<JOSPEvent> getHistoryEvents(HistoryLimits limits) {

        JavaJSONArrayToFile.Filter<JOSPEvent> filter = new JavaJSONArrayToFile.Filter<JOSPEvent>() {
            @Override
            public boolean accepted(JOSPEvent o) {
                return true;
            }
        };

        return filterHistoryEvents(limits, filter);
    }

    @Override
    public List<JOSPEvent> filterHistoryEvents(HistoryLimits limits, JavaJSONArrayToFile.Filter<JOSPEvent> filter) {
        if (HistoryLimits.isLatestCount(limits))
            return events.tryLatest(filter, limits.getLatestCount());

        if (HistoryLimits.isAncientCount(limits))
            return events.tryAncient(filter, limits.getAncientCount());

        if (HistoryLimits.isIDRange(limits))
            return events.tryById(filter, limits.getFromIDOrDefault(), limits.getToIDOrDefault());

        if (HistoryLimits.isDateRange(limits))
            return events.tryByDate(filter, limits.getFromDateOrDefault(), limits.getToDateOrDefault());

        if (HistoryLimits.isPageRange(limits))
            try {
                List<JOSPEvent> all = events.filterAll(filter);

                int page = limits.getPageNumOrDefault();
                int size = limits.getPageSizeOrDefault();

                int posStart = page*size;
                if (posStart>all.size()-1)
                    return new ArrayList<>();

                int posEnd = (page*size) + size - 1;
                if (posEnd>=all.size()-1)
                    posEnd = all.size()-1;
                return all.subList(posStart,posEnd+1);

            } catch (JavaJSONArrayToFile.FileException e) {
                return new ArrayList<>();
            }

        try {
            return events.filterAll(filter);

        } catch (JavaJSONArrayToFile.FileException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void setJCPClient(JCPAPIsClientObj jcpClient) {
        if (jcpClient == null) return;

        this.jcpClient = jcpClient;
        this.jcpClient.addConnectionListener(jcpConnectListener);
        this.apiEventsCaller = new Caller20(jcpClient);
    }


    // Mngm methods

    @Override
    public void storeCache() throws IOException {
        events.storeCache();
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

}
