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

package com.robypomper.josp.jod.executor;

import com.robypomper.java.JavaTimers;
import com.robypomper.josp.jod.structure.JODComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Timer;


/**
 * Default Puller class used by {@link JODPuller} implementations.
 */
public abstract class AbsJODPuller extends AbsJODWorker implements JODPuller {

    // Class's constants

    public static final String TH_PULLER_NAME = "_PULL_%s_";
    public static final int DEF_POLLING_TIME = 5000;


    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(AbsJODPuller.class);
    private Timer timer;


    // Constructor

    /**
     * {@inheritDoc}
     */
    public AbsJODPuller(String name, String proto, JODComponent component) {
        super(name, proto, component);
    }


    // Getters

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return timer != null;
    }

    /**
     * Polling time in ms.
     *
     * @return the polling time in ms.
     */
    protected long getPollingTime() {
        return DEF_POLLING_TIME;
    }


    // Mngm

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTimer() {
        log.debug(String.format("                                   Start '%s' puller", getName()));
        if (isEnabled()) return;

        log.debug("Starting puller timer");
        timer = JavaTimers.initAndStart(new PullerTimer(), true,String.format(TH_PULLER_NAME, getProto()),getName(),0,getPollingTime());

        log.debug("Puller timer started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopTimer() {
        log.info(String.format("Stop '%s' puller timer", getName()));
        if (!isEnabled()) return;

        log.debug("Stopping puller timer");
        JavaTimers.stopTimer(timer);
        timer = null;
        log.debug("Puller timer stopped");
    }

    private class PullerTimer implements Runnable {

        @Override
        public void run() {
            pull();
        }

    }

}
