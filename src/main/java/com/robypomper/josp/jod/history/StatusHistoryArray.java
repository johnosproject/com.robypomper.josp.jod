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
import com.robypomper.josp.protocol.JOSPHistory;

import java.io.File;
import java.util.Date;

public class StatusHistoryArray extends JavaJSONArrayToFile<JOSPHistory, Long> {

    public StatusHistoryArray(File jsonFile, boolean keepInMemory, int maxBufferSize, int releaseBufferSize, int maxFileSize, int releaseFileSize) throws FileException {
        super(jsonFile, JOSPHistory.class, keepInMemory, maxBufferSize, releaseBufferSize, maxFileSize, releaseFileSize);
    }

    @Override
    protected int compareItemIds(Long id1, Long id2) {
        return id1.compareTo(id2);
    }

    @Override
    protected Long getItemId(JOSPHistory value) {
        return value.getId();
    }

    @Override
    protected Date getItemDate(JOSPHistory value) {
        return value.getUpdatedAt();
    }

}
