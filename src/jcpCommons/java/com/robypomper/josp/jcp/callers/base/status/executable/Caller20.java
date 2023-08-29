/*******************************************************************************
 * The John Cloud Platform is the set of infrastructure and software required to provide
 * the "cloud" to an IoT EcoSystem, like the John Operating System Platform one.
 * Copyright 2021 Roberto Pompermaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.robypomper.josp.jcp.callers.base.status.executable;

import com.github.scribejava.core.model.Verb;
import com.robypomper.josp.clients.AbsAPIJCP;
import com.robypomper.josp.clients.JCPAPIsClientJCP;
import com.robypomper.josp.clients.JCPClient2;
import com.robypomper.josp.jcp.defs.base.internal.status.executable.Params20;
import com.robypomper.josp.jcp.defs.base.internal.status.executable.Paths20;

import java.util.Date;


/**
 * JCP All - Status / Executable 2.0
 */
public class Caller20 extends AbsAPIJCP {

    public Caller20(JCPAPIsClientJCP jcpClient) {
        super(jcpClient);
    }

    public JCPAPIsClientJCP getClient() {
        return (JCPAPIsClientJCP) jcpClient;
    }


    // Index methods

    public Params20.Index getIndex() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC, Params20.Index.class, isSecure());
    }


    // Online methods

    public Date getOnlineReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_ONLINE, Date.class, isSecure());
    }


    // Process methods

    public Params20.Process getProcessReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_PROCESS, Params20.Process.class, isSecure());
    }


    // Java methods

    public Params20.JavaIndex getJavaIndex() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA, Params20.JavaIndex.class, isSecure());
    }

    public Params20.JavaVM getJavaVMReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_VM, Params20.JavaVM.class, isSecure());
    }

    public Params20.JavaRuntime getJavaRuntimeReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_RUNTIME, Params20.JavaRuntime.class, isSecure());
    }

    public Params20.JavaTimes getJavaTimesReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_TIMES, Params20.JavaTimes.class, isSecure());
    }

    public Params20.JavaClasses getJavaClassesReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_CLASSES, Params20.JavaClasses.class, isSecure());
    }

    public Params20.JavaMemory getJavaMemoryReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_MEMORY, Params20.JavaMemory.class, isSecure());
    }

    public Params20.JavaThreads getJavaThreadsReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_THREADS, Params20.JavaThreads.class, isSecure());
    }

    public Params20.JavaThread getJavaThreadReq(long threadId) throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_JAVA_THREAD(threadId), Params20.JavaThread.class, isSecure());
    }


    // OS methods

    public Params20.OS getOSReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_OS, Params20.OS.class, isSecure());
    }


    // CPU methods

    public Params20.CPU getCPUReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_CPU, Params20.CPU.class, isSecure());
    }


    // Memory methods

    public Params20.Memory getMemoryReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_MEMORY, Params20.Memory.class, isSecure());
    }


    // Disks methods

    public Params20.Disks getDisksReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_DISKS, Params20.Disks.class, isSecure());
    }

    public Params20.Disk getDiskReq(String diskId) throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_DISK(diskId), Params20.Disk.class, isSecure());
    }


    // Networks methods

    public Params20.Networks getNetworksReq() throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_NETWORKS, Params20.Networks.class, isSecure());
    }

    public Params20.Network getNetworkReq(int networkId) throws JCPClient2.ConnectionException, JCPClient2.AuthenticationException, JCPClient2.ResponseException, JCPClient2.RequestException {
        return jcpClient.execReq(Verb.GET, Paths20.FULL_PATH_EXEC_NETWORK(networkId), Params20.Network.class, isSecure());
    }

}
