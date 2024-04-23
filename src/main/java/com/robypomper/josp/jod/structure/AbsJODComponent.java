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

package com.robypomper.josp.jod.structure;

import com.robypomper.josp.jod.history.JODHistory;
import com.robypomper.josp.protocol.HistoryLimits;
import com.robypomper.josp.protocol.JOSPHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of {@link JODComponent} interface.
 * <p>
 * In addition to the method defined by the interface, this class manage the
 * reference to the JOD Structure system. This reference is used by container
 * components to initialize other components. Also the state components use
 * this reference to propagate the status updates to the JOD Communication system.
 */
public abstract class AbsJODComponent implements JODComponent {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(AbsJODComponent.class);
    private final JODStructure structure;
    private final JODHistory history;
    private JODContainer parent = null;
    private final String name;
    private final String descr;


    // Constructor

    /**
     * Default constructor that initialize component's commons properties.
     * <p>
     * Moreover it store the reference to the JOD Structure system.
     *
     * @param structure the JOD Structure system.
     * @param name      the name of the component.
     * @param descr     the description of the component.
     */
    public AbsJODComponent(JODStructure structure, JODHistory history, String name, String descr) {
        this.structure = structure;
        this.history = history;
        this.name = name;
        this.descr = descr != null ? descr : "";

        log.debug(String.format("                                   Initialized JODComponent/%s instance for '%s' component", this.getClass().getSimpleName(), name));
    }


    // Commons properties

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescr() {
        return descr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODContainer getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JODComponentPath getPath() {
        if (parent == null)
            return new DefaultJODComponentPath(StructureDefinitions.PATH_STR_ROOT);
        return parent.getPath().add(getName());
    }


    // Status History

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JOSPHistory> getHistoryStatus(HistoryLimits limits) {
        return getHistory().getHistoryStatus(this,limits);
    }


    // Implementation methods

    /**
     * Method called by {@link AbsJODContainer#setComponents(Map)} to set his self
     * as parent of the current component.
     *
     * <b>This method can be called only one time.</b> During their initialization,
     * {@link JODContainer} and his sub classes, call the setComponents(Map)
     * method and then this method.
     *
     * @param parent the container parent of current component.
     */
    public void setParent(JODContainer parent) throws JODStructure.ComponentInitException {
        if (this.parent != null)
            throw new JODStructure.ComponentInitException(String.format("Component '%s', can't set twice 'parent' property", getName()));

        this.parent = parent;
    }

    /**
     * @return the JOD Structure system.
     */
    protected JODStructure getStructure() {
        return structure;
    }

    /**
     * @return the JOD History system.
     */
    protected JODHistory getHistory() {
        return history;
    }

}
