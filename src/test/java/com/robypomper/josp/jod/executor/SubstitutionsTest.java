/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
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

import com.robypomper.josp.consts.JOSPConstants;
import com.robypomper.josp.jod.objinfo.JODObjectInfo;
import com.robypomper.josp.jod.structure.JODComponent;
import com.robypomper.josp.jod.structure.pillars.JODBooleanAction;
import com.robypomper.josp.jod.structure.pillars.JODBooleanState;
import com.robypomper.josp.jod.structure.pillars.JODRangeAction;
import com.robypomper.josp.jod.structure.pillars.JODRangeState;
import com.robypomper.josp.protocol.JOSPActionCommandParams;
import com.robypomper.josp.protocol.JOSPProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 *
 *
 * Substitution class usage:
 * <code>
 // AbsWorker
 // protected String parseConfigSubstitution(String strValue) {
 return new Substitutions(strValue)
 //.substituteObject(jod.getObjectInfo())
 //.substituteObjectConfigs(jod.getObjectInfo())
 .substituteComponent(component)
 .toString();

 // ExecutorFiles
 // public boolean exec(JOSPProtocol.ActionCmd commandAction, JODBooleanAction.JOSPBoolean cmdAction)
 String actionStr = new Substitutions(actionStrFormat)
 //.substituteObject(jod.getObjectInfo())
 //.substituteObjectConfigs(jod.getObjectInfo())
 .substituteComponent(getComponent())
 .substituteState((JODState)getComponent())
 .substituteAction(commandAction)
 .toString();
 // public boolean exec(JOSPProtocol.ActionCmd commandAction, JODRangeAction.JOSPRange cmdAction)
 String actionStr = new Substitutions(actionStrFormat)
 .substituteAction(commandAction)
 .toString();


 // ExecutorShell
 // public boolean exec(JOSPProtocol.ActionCmd commandAction, JODBooleanAction.JOSPBoolean cmdAction)
 String cmdUpd = new Substitutions(cmd)
 //.substituteObject(jod.getObjectInfo())
 //.substituteObjectConfigs(jod.getObjectInfo())
 .substituteComponent(getComponent())
 .substituteState((JODState)getComponent())
 .substituteAction(commandAction)
 .toString();
 String redirectUpd = redirect.isEmpty() ? null :
 new Substitutions(redirect)
 //.substituteObject(jod.getObjectInfo())
 //.substituteObjectConfigs(jod.getObjectInfo())
 .substituteComponent(getComponent())
 .substituteState((JODState)getComponent())
 .substituteAction(commandAction)
 .toString();
 // public boolean exec(JOSPProtocol.ActionCmd commandAction, JODRangeAction.JOSPRange cmdAction)
 String cmdUpd = new Substitutions(cmd)
 .substituteAction(commandAction)
 .toString();
 String redirectUpd = redirect.isEmpty() ? null :
 new Substitutions(redirect)
 .substituteAction(commandAction)
 .toString();

 // PullerShell
 // public void pull()
 String cmdFormatted = new Substitutions(cmd)
 //.substituteObject(jod.getObjectInfo())
 //.substituteObjectConfigs(jod.getObjectInfo())
 .substituteComponent(getComponent())
 .substituteState((JODState) getComponent())
 .toString();
 * </code>
 *
 *
 *
 *
 */
@ExtendWith(MockitoExtension.class)
public class SubstitutionsTest {

    private static class MockActionCmd extends JOSPProtocol.ActionCmd {
        public MockActionCmd(String serviceId, String userId, String instanceId, String objectId, String componentPath, JOSPActionCommandParams command) {
            super(serviceId, userId, instanceId, objectId, componentPath, command);
        }
    }

    @Mock private static JODObjectInfo DEF_OBJ_INFO;
    @Mock private static JODComponent DEF_COMPONENT;
    @Mock private static JODWorker DEF_WORKER;
    @Mock private static JODBooleanState DEF_STATE_BOOLEAN;
    @Mock private static JODRangeState DEF_STATE_RANGE;
    private final static String DEF_OBJ_ID = "...";
    private final static String DEF_JOD_VERSION = "...";
    private final static String DEF_OBJ_NAME = "...";
    private final static String DEF_OBJ_OWNER = "...";
    private final static String DEF_OBJ_MODEL = "...";
    private final static String DEF_OBJ_BRAND = "...";
    private final static String DEF_COMP_NAME = "...";
    private final static String DEF_COMP_TYPE = "...";
    private static JOSPProtocol.ActionCmd DEF_ACTION_BOOLEAN = new MockActionCmd(
            "xxxxx-xxxxx-xxxxx",
            JOSPConstants.ANONYMOUS_ID,
            "yyyyy",
            DEF_OBJ_ID,
            "comp/path", //DEF_COMP_PATH,
            new JODBooleanAction.JOSPBoolean("false,true")
    );
    private final static JOSPProtocol.ActionCmd DEF_ACTION_RANGE = new MockActionCmd(
            "xxxxx-xxxxx-xxxxx",
            JOSPConstants.ANONYMOUS_ID,
            "yyyyy",
            DEF_OBJ_ID,
            "comp/path", //DEF_COMP_PATH,
            new JODRangeAction.JOSPRange("0,5")
    );

    @Test
    public void objInfoTest() {
        String strWithPlaceholders = Substitutions.OBJ_JOD_VER + ", "
                + Substitutions.OBJ_ID + ", "
                + Substitutions.OBJ_NAME + ", "
                + Substitutions.OBJ_OWNER + ", "
                + Substitutions.OBJ_MODEL + ", "
                + Substitutions.OBJ_BRAND;
        when(DEF_OBJ_INFO.getJODVersion()).thenReturn(DEF_JOD_VERSION);
        when(DEF_OBJ_INFO.getObjId()).thenReturn(DEF_OBJ_ID);
        when(DEF_OBJ_INFO.getObjName()).thenReturn(DEF_OBJ_NAME);
        when(DEF_OBJ_INFO.getOwnerId()).thenReturn(DEF_OBJ_OWNER);
        when(DEF_OBJ_INFO.getModel()).thenReturn(DEF_OBJ_MODEL);
        when(DEF_OBJ_INFO.getBrand()).thenReturn(DEF_OBJ_BRAND);
        String strExpected = DEF_JOD_VERSION + ", "
                + DEF_OBJ_ID + ", "
                + DEF_OBJ_NAME + ", "
                + DEF_OBJ_OWNER + ", "
                + DEF_OBJ_MODEL + ", "
                + DEF_OBJ_BRAND;

        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteObject(DEF_OBJ_INFO)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    @Test
    public void componentTest() {
        String strWithPlaceholders = Substitutions.COMP_NAME + ", "
                + Substitutions.COMP_TYPE + ", "
                + Substitutions.COMP_PATH + ", "
                + Substitutions.COMP_PARENT_NAME + ", "
                + Substitutions.COMP_PARENT_PATH;
        when(DEF_COMPONENT.getName()).thenReturn(DEF_COMP_NAME);
        when(DEF_COMPONENT.getType()).thenReturn(DEF_COMP_TYPE);
        String strExpected = DEF_COMP_NAME + ", "
                + DEF_COMP_TYPE + ", "
                + "N/A, "
                + "N/A, "
                + "N/A";

        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteComponent(DEF_COMPONENT)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    @Test
    public void workerTest() {
        // No substitutions for WORKER
    }

    @Test
    public void stateBooleanTest() {
        String strWithPlaceholders = Substitutions.STATE_VAL;
        String strExpected = "FALSE";
        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);


        strWithPlaceholders = Substitutions.STATE_VAL_BOOL;
        strExpected = "FALSE";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.STATE_VAL_BIN;
        strExpected = "0";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    @Test
    public void stateRangeTest() {
        String strWithPlaceholders = Substitutions.STATE_VAL;
        String strExpected = "0.000000";
        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.STATE_VAL_COMMA;
        strExpected = "0,000000";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.STATE_VAL_POINT;
        strExpected = "0.000000";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.STATE_VAL_INT;
        strExpected = "0";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteState(DEF_STATE_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    @Test
    public void actionBooleanTest() {
        String strWithPlaceholders = Substitutions.ACTION_VAL + ", "
                + Substitutions.ACTION_VAL_OLD + ", "
                + Substitutions.ACTION_SRV_ID + ", "
                + Substitutions.ACTION_USR_ID;
        String strExpected = "FALSE, TRUE, xxxxx-xxxxx-xxxxx, " + JOSPConstants.ANONYMOUS_ID;
        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.ACTION_VAL_BOOL + ", "
                + Substitutions.ACTION_VAL_OLD_BOOL;
        strExpected = "FALSE, TRUE";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.ACTION_VAL_BIN + ", "
                + Substitutions.ACTION_VAL_OLD_BIN;
        strExpected = "0, 1";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_BOOLEAN)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    @Test
    public void actionRangeTest() {
        String strWithPlaceholders = Substitutions.ACTION_VAL + ", "
                + Substitutions.ACTION_VAL_OLD + ", "
                + Substitutions.ACTION_SRV_ID + ", "
                + Substitutions.ACTION_USR_ID;
        String strExpected = "0.000000, 5.000000, xxxxx-xxxxx-xxxxx, " + JOSPConstants.ANONYMOUS_ID;
        String substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.ACTION_VAL_COMMA + ", "
                + Substitutions.ACTION_VAL_OLD_COMMA;
        strExpected = "0,000000, 5,000000";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.ACTION_VAL_POINT + ", "
                + Substitutions.ACTION_VAL_OLD_POINT;
        strExpected = "0.000000, 5.000000";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);

        strWithPlaceholders = Substitutions.ACTION_VAL_INT + ", "
                + Substitutions.ACTION_VAL_OLD_INT;
        strExpected = "0, 5";
        substitutionResult = new Substitutions(strWithPlaceholders)
                .substituteAction(DEF_ACTION_RANGE)
                .toString();
        Assertions.assertEquals(strExpected, substitutionResult);
    }

    //@Test
    public void repetitiveTest() {
        for (int i = 0; i<1000; i++) {
            objInfoTest();
            componentTest();
            workerTest();
            stateBooleanTest();
            stateRangeTest();
            actionBooleanTest();
            actionRangeTest();
        }
    }

    private void checkComponent(String str1, String str2) {
        Assertions.assertEquals(
                new Substitutions(str1).substituteComponent(DEF_COMPONENT).toString(),
                new Substitutions(str2).substituteComponent(DEF_COMPONENT).toString()
        );
    }

    private void checkState(String str1, String str2) {
        Assertions.assertEquals(
                new Substitutions(str1).substituteState(DEF_STATE_BOOLEAN).toString(),
                new Substitutions(str2).substituteState(DEF_STATE_BOOLEAN).toString()
        );
    }

    private void checkAction(String str1, String str2) {
        Assertions.assertEquals(
                new Substitutions(str1).substituteAction(DEF_ACTION_BOOLEAN).toString(),
                new Substitutions(str2).substituteAction(DEF_ACTION_BOOLEAN).toString()
        );
    }

}
