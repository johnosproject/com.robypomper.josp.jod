# JOSP Object Daemon - Specs: Communication Local

Also known as Direct Communication, the Local Communication is the connection
used by JOD Agents to communicate with JOSP Services when both are connected on
the same local network.

Technically, the JOD Agent acts as a server and the JOSP Service as a client.
Once started, the JOD Agent publishes himself on the local network using the
mDNS protocol. The JOSP Service, in turn, searches for JOD Agents on the local
network and connects to them.<br/>
Once the connection is established, the JOD Agent and the JOSP Service can
exchange messages and data using the [JOSP Protocol](../josp_comps/josp_commons_josp_protocol.md).

The Local Communication on JOD Agent is managed by the
[JODLocalServer](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalServer.java)
class. It handles the incoming connections from the JOSP Services.<br/>
The JODLocalServer creates a new [JODLocalClientInfo](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalClientInfo.java)
object for each incoming connection and keep always at most one connection for
each JOSP Service.<br/>
At the same time, the JODLocalServer sends the object's presentation message to
the JOSP Service, as defined by the [JOSP Protocol](../josp_comps/josp_commons_josp_protocol.md), and starts
forwarding the incoming messages to the [JODCommunication](../../src/main/java/com/robypomper/josp/jod/comm/JODCommunication.java)
instance.<br/>
Once the JOSP Service is registered or updated, his JODLocalClientInfo object is
available using the `getLocalClientInfo` or `getLocalClientInfoByServiceId`
methods.

In order to support the [Security Levels](../josp_comps/josp_commons_josp_communication_securitylevels.md) feature,
the JODLocalServer can use different server implementations: with and without SSL.
If SSL is enabled, then the server can enable the SSLShared feature to share the
server certificate with the JOSP Services (the clients). That enables the "Share"
Security levels. Moreover, always if SSL server is enabled, the certificate can
contain the [JOD Object's ID](object_id.md). That enables the "Instance"
Security levels. More info and examples are available on the
[Security Levels](../josp_comps/josp_commons_josp_communication_securitylevels.md) page.

The incoming messages are processed by the `JODLocalServer::processData(ServerClient, String)}`
method that forward the message to the `JODCommunication#processFromServiceMsg(String, JOSPPerm.Connection)`
method.<br/>
On the other side the outgoing messages are sent using the
`JODCommunication#sendToServices(String, JOSPPerm.Type)` or the
`JODCommunication#sendToSingleLocalService(JODLocalClientInfo, String, JOSPPerm.Type)`
methods.


## Local Connection process

The [JODLocalServer](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalServer.java)
is the main class that manages the local connection process. It's responsible for
publishing the service and handling the connection between JOD Agents and
JOSP Services on the local network.

**1. Publish JOD Agent's service on the local network**<br/>
On Local Communication's startup, the [JODCommunication](../../src/main/java/com/robypomper/josp/jod/comm/JODCommunication.java)
startups a local server and publishes the corresponding service on the local
network using the mDNS protocol. The service is published using the `_josp2._tcp.`
type and the `{OBJ_NAME}-{RND_NUMBER}` string as name.<br/>

**2. Discover and connect to JOD Agent on the local network**<br/>
Once the JOD Agent's service is published, the JOSP Service can discover it
and connect to it.<br/>
Depending on the JOD Agent and Service configurations, different connections
types can be established, check the [Security Levels](../josp_comps/josp_commons_josp_communication_securitylevels.md)
page for more information.<br/>
Because of Security Levels support, the JOSP Service can attempt to connect to
the JOD Agent using different connection configurations. In case of failure,
the JODLocalServer can receive invalid incoming connections (reachability test,
wrong encryption settings, wrong certificates, etc...).

**3. Handle the connection**<br/>
When a connection to JOD Agent is established, the JODLocalServer creates a
new [JODLocalClientInfo](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalClientInfo.java)
object to manage the connection and associate it to a remote JOSP Service.<br/>
Then the JODLocalServer checks if the related JOSP Service is already connected
using another connection. If so, it discharges the new connection and keep the
old one. Otherwise, it adds the new connection to the list of connected services
and sends the [object's presentation message](../josp_comps/josp_commons_josp_protocol.md) to
the JOSP Service.<br/>

The main methods from [JODLocalServer](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalServer.java)
are the `processOnClientConnected(ServerClient)` and `processOnClientDisconnected(ServerClient)`
that handle the client's connections and disconnections.

*JODLocalServer::processOnClientConnected*

1. Set connection LUID (local-unique-id)
2. Send JOD Object's Id message to client
   -> ERR on send object's Id message, discharge the client
3. Get/Wait JOSP Service's fullId
   -> ERR on wait JOSP Service's fullId, discharge the client
4. Wait for JOSP Service client connection establishment
   -> ERR on wait for client connection, discharge the client
5. Create JODLocalClientInfo from connection and JOSP Service's fullId
6. Check existing JODLocalClientInfo for service's fullId
   a. Add new JOSP Service
      1. add new JODLocalClientInfo to localClients list
      2. send object's presentation message to server
         -> ERR on send object's presentation message, remove and discharge the service client
   b. Replace JOSP Service's connection (existing = disconnected)
      1. remove existing JODLocalClientInfo from localClients list
      2. add new JODLocalClientInfo to localClients list
      3. send object's presentation message to server
         -> ERR on send object's presentation message, remove and discharge the service client
   c. Discharge connection because JOSP Service already connected
      1. disconnect JOSP Service's connection (local)

*JODLocalServer::processOnClientDisconnected*

1. Get existing JODLocalClientInfo for disconnected client
2. Remove JODLocalClientInfo from localClients list


## JOSP Object Daemon - Local Communication configs

The JOD Agent's local communication configuration is defined in the
`jod.yml` [configuration file](jod_yml.md). The properties that define
the local communication are:

* `jod.comm.local.enabled` ("true"): If 'true' the server for Local Communication will be enabled, otherwise it will not be started.
* `jod.comm.local.discovery` ("Auto"): Discovery implementation for the Local Communication server.
* `jod.comm.local.port` ("0"): Port for the Local Communication server. If it is `0`, then the server  will choose a random port.
* `jod.comm.local.enableSSL` ("false"): If 'true' the local server will use SSL, otherwise it will use plain TCP.
* `jod.comm.local.sslSharingEnabled` ("true"): If 'true' the local server expose a `ServerCertSharing` server to exchange certificates with local clients.
* `jod.comm.local.ks.path` ("./configs/local_ks.jks"): Path for the service's local keystore.
* `jod.comm.local.ks.pass` ("123456"): Password for the service's local keystore. It must be at least 6 characters long.
* `jod.comm.local.ks.alias` (""): Alias of the certificate stored into the service's local keystore. If it is an empty string, then the `$SRV_ID-LocalCert` format is used.

The available [Discovery System implementations](../josp_comps/josp_commons_discovery.md)
are:

* `avahi`: Avahi cmdline tool
* `dnssd`: DNS-SD cmdline tool
* `jmdns`: JmDNS Java library
* `jmmdns`: JmmDNS Java library
