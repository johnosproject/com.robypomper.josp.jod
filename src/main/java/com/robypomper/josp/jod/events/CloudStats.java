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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class CloudStats {

    // Internal vars
    
    /** File to use for storing the stats */
    private final File file;
    /**
     * Last item registered (added to the memory buffer)
     */
    private long lastRegistered = -1;
    /**
     * Last item stored (flushed to the file)
     */
    private long lastStored = -1;
    /**
     * Last item uploaded (synchronized with the cloud)
     */
    private long lastUploaded = -1;
    /**
     * Last item deleted (removed from file)
     */
    private long lastDelete = -1;

    /**
     * Number of items registered (increased on register new item)
     */
    private long registered = 0;
    /**
     * Number of items stored (increased on flush to file)
     */
    private long stored = 0;
    /**
     * Number of items uploaded (increased on cloud sync)
     */
    private long uploaded = 0;
    /**
     * Number of items deleted (increased on file remove)
     */
    private long deleted = 0;
    /**
     * Number of items removed but not uploaded (increased on file remove)
     */
    private long lost = 0;


    // Constructors

    @JsonCreator
    private CloudStats(@JsonProperty("file") File file,
                       @JsonProperty("lastRegistered") long lastRegistered,
                       @JsonProperty("lastStored") long lastStored,
                       @JsonProperty("lastUploaded") long lastUploaded,
                       @JsonProperty("lastDelete") long lastDelete,
                       @JsonProperty("registeredCount") long registered,
                       @JsonProperty("storedCount") long stored,
                       @JsonProperty("uploadedCount") long uploaded,
                       @JsonProperty("deletedCount") long deleted,
                       @JsonProperty("lostCount") long lost) {
        this.file = file;
        this.lastRegistered = lastRegistered;
        this.lastStored = lastStored;
        this.lastUploaded = lastUploaded;
        this.lastDelete = lastDelete;
        this.registered = registered;
        this.stored = stored;
        this.uploaded = uploaded;
        this.deleted = deleted;
        this.lost = lost;
    }

    public CloudStats(File file) throws IOException {
        this.file = file;

        if (file.exists()) {
            CloudStats s = read();
            this.lastRegistered = s.lastRegistered;
            this.lastStored = s.lastStored;
            this.lastUploaded = s.lastUploaded;
            this.lastDelete = s.lastDelete;
            this.registered = s.registered;
            this.stored = s.stored;
            this.uploaded = s.uploaded;
            this.deleted = s.deleted;
            this.lost = s.lost;
        }
    }


    // Getters and setters

    public long getLastRegistered() {
        return lastRegistered;
    }

    public long getRegisteredCount() {
        return registered;
    }

    public void setLastRegistered(long id, int count) {
        registered += count;
        this.lastRegistered = id;
    }

    public long getLastStored() {
        return lastStored;
    }

    public long getStoredCount() {
        return stored;
    }

    public void setLastStored(long id, int count) {
        stored += count;
        lastStored = id;
    }

    public long getLastUploaded() {
        return lastUploaded;
    }

    public long getUploadedCount() {
        return uploaded;
    }

    public void setLastUploaded(long id, int count) {
        uploaded += count;
        lastUploaded = id;
    }

    public long getLastDelete() {
        return lastDelete;
    }

    public long getDeletedCount() {
        return deleted;
    }

    public long getLostCount() {
        return lost;
    }

    public void setLastDelete(long id, int count, int countLost) {
        deleted += count;
        lost += countLost;
        lastDelete = id;
    }


    // File read and write

    private CloudStats read() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, CloudStats.class);
    }

    public void write() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, this);
    }

    public void writeIgnoreExceptions() {
        try {
            write();
        } catch (IOException ignore) {}
    }
}
