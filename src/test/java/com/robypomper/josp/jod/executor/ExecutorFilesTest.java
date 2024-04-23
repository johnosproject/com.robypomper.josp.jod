/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
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

import com.robypomper.java.JavaFiles;
import com.robypomper.java.JavaFormatter;
import com.robypomper.josp.jod.structure.AbsJODState;
import com.robypomper.josp.jod.structure.JODState;
import com.robypomper.josp.jod.structure.JODStructure;
import com.robypomper.josp.jod.structure.pillars.JODBooleanAction;
import com.robypomper.josp.jod.structure.pillars.JODRangeAction;
import com.robypomper.josp.protocol.JOSPMsgParams;
import com.robypomper.josp.protocol.JOSPProtocol;
import com.robypomper.josp.test.mocks.jod.MockActionCmd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExecutorFilesTest {

    private final String ITEMS_SEP = JOSPMsgParams.ITEMS_SEP;

    @Test
    public void executorTest() throws InterruptedException, IOException, JODWorker.MissingPropertyException {
        String name = "executorTest";
        String proto = "files";
        String filePathStr = "listenerFilesTest.txt";
        Path filePath = Paths.get(filePathStr);
        String configs = "path=" + filePathStr;

        System.out.println("\nCREATE AND START EXECUTOR FOR FILES");
        JODState state = null;
        try {
            state = new AbsJODState(null,null,null,"","","NONE",null) {
                @Override
                public String getState() {
                    return "N/A";
                }

                @Override
                public String getType() {
                    return "JODStateMock";
                }
            };
        } catch (JODStructure.ComponentInitException ignore) {}
        ExecutorFiles e = new ExecutorFiles(name, proto, configs, state);
        JOSPProtocol.ActionCmd commandAction = new MockActionCmd();

        System.out.println("\nEXECUTE BOOLEAN ACTION");
        String updStr = ExecutorShellTest.formatUpdStr(true, false);
        JODBooleanAction.JOSPBoolean cmdActionBoolean = new JODBooleanAction.JOSPBoolean(updStr);
        when(commandAction.getCommand()).thenReturn(cmdActionBoolean);
        e.exec(commandAction, cmdActionBoolean);
        Thread.sleep(1000);
        String fileContent = JavaFiles.readString(filePath);
        Assertions.assertEquals("TRUE", fileContent.toUpperCase());

        System.out.println("\nEXECUTE RANGE ACTION");
        updStr = ExecutorShellTest.formatUpdStr(5.33, 0.0);
        JODRangeAction.JOSPRange cmdActionRange = new JODRangeAction.JOSPRange(updStr);
        when(commandAction.getCommand()).thenReturn(cmdActionRange);
        e.exec(commandAction, cmdActionRange);
        Thread.sleep(1000);
        String readFile2 = JavaFiles.readString(filePath);
        Assertions.assertEquals(new Double(5.33), JavaFormatter.strToDouble(readFile2));

        if (!filePath.toFile().delete())
            throw new IOException(String.format("Can't delete `%s` file", filePath));
    }

}
