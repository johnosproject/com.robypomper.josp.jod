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

package com.robypomper.josp.jod.executor;

import com.robypomper.josp.jod.structure.JODComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default Executor class used by {@link JODExecutor} implementations.
 */
public abstract class AbsJODExecutor extends AbsJODWorker implements JODExecutor {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(AbsJODExecutor.class);
    private boolean enabled = false;


    // Constructor

    /**
     * {@inheritDoc}
     */
    public AbsJODExecutor(String name, String proto, JODComponent component) {
        super(name, proto, component);
    }


    // Getters

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }


    // JODExecutor's impl

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exec() {
        log.debug(String.format("Executing '%s' executor", getName()));
        if (!enabled) {
            log.warn(String.format("Error on exec '%s' executor because disabled", getName()));
            return false;
        }

        // Do something...
        log.trace("Do something...");


        log.debug(String.format("Executor '%s' executed", getName()));
        return true;
    }


    // Mngm

    /**
     * {@inheritDoc}
     */
    @Override
    public void enable() {
        log.debug(String.format("                                   Enable '%s' executor", getName()));
        if (isEnabled()) return;

        enabled = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disable() {
        log.debug(String.format("                                   Disable '%s' executor", getName()));
        if (!isEnabled()) return;

        enabled = false;
    }

}
