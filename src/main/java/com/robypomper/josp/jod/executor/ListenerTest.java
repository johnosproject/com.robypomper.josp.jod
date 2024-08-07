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

import com.robypomper.josp.jod.structure.JODComponent;
import com.robypomper.josp.jod.structure.JODState;
import com.robypomper.josp.jod.structure.pillars.JODBooleanState;
import com.robypomper.josp.jod.structure.pillars.JODRangeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * JOD Listener test.
 * <p>
 * Each interaction of the server's infinite loop (from {@link #getServerLoop()}
 * method) call the {@link JODState} sub class's <code>setUpdate(...)</code>
 * method each 10 seconds (10.000ms).
 * <p>
 * Print log messages, from dedicated thread, on server startup and shutdown.
 */
public class ListenerTest extends AbsJODListenerLoop {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(ListenerTest.class);


    // Constructor

    /**
     * Default ListenerTest constructor.
     *
     * @param name       name of the listener.
     * @param proto      proto of the listener.
     * @param configsStr configs string, can be an empty string.
     */
    public ListenerTest(String name, String proto, String configsStr, JODComponent component) {
        super(name, proto, component);
        log.trace(String.format("ListenerTest for component '%s' init with config string '%s://%s'", getName(), proto, configsStr));
    }


    // Mngm

    /**
     * Server Loop method: print a log messages and start infinite loop where
     * call the {@link JODState} sub class's <code>setUpdate(...)</code> method
     * each 10 seconds (10.000ms).
     */
    @Override
    protected void getServerLoop() {
        log.trace(String.format("ListenerTest for component '%s' of proto '%s' running", getName(), getProto()));

        while (!mustShoutingDown()) {
            log.trace(String.format("ListenerTest for component '%s' of proto '%s' listened", getName(), getProto()));

            // For each JODState supported
            if (getComponent() instanceof JODBooleanState)
                ((JODBooleanState) getComponent()).setUpdate(true);
            else if (getComponent() instanceof JODRangeState)
                ((JODRangeState) getComponent()).setUpdate(5);

            try {
                //noinspection BusyWait
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                break;
            }
        }

        log.trace(String.format("ListenerTest for component '%s' terminated", getName()));
    }

}
