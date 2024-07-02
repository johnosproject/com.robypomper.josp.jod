# JOSP Object Daemon - Specs: Communication

Because a **JOSP EcoSystem is composed by different 'pieces', a JOD Agent use
multiple communication channels** with different protocols to talks with all those 'pieces':

* to JCP APIs (via HTTP) to register and handle the JOD Agent to the John Cloud Platform
* to JCP Gateways (via JOSP) to communicate with remote JOSP Services via Cloud Communication
* from JOSP Services (via JOSP) to communicate with local JOSP Services via Direct Communication

The JCP APIs client is initialized directly from the [JOD](../../src/main/java/com/robypomper/josp/jod/JOD.java)
class as the `JCPAPIsClientObj` object; and then shared to all other subcomponents.<br/>
At the same time, the JOD class, initializes the [JODCommunication](../../src/main/java/com/robypomper/josp/jod/comm/JODCommunication.java)
that is responsible for the JOSP communications and the relative message routing.
To communicate with JOSP Services, the JOD Agent support both JOSP Communications
types:

* [Direct/Local](communication_local.md) via [JODLocalServer](../../src/main/java/com/robypomper/josp/jod/comm/JODLocalServer.java)
* and [Cloud/Remote](communication_cloud.md) via [JODGwO2SClient](../../src/main/java/com/robypomper/josp/jod/comm/JODGwO2SClient.java)

To keep the communication secure the JOD Agent use encrypted communication with
the JCP APIs and the JCP Gateways. For the Local Communication, the JOD Agent
supports the [Security Levels](../josp_comps/josp_commons_josp_communication_securitylevels.md) feature that
allows the JOD Agent to communicate with JOSP Services using different security
levels based on the SSL/TLS connection.

## John Cloud Platform APIs

[JCP APIs](/docs/josp_comps/jcp.md#jcp-apis) are a set of HTTP Methods
exposed by the JCP platform and are used by JOD Agent to handle the JOD Agent
on the JCP. That include: the generation of a unique object's id, the supply of
the JCP GWs access info, the storage of JOD Agent's events and status histories,
etc...

The JCP APIs service requires the JOD Agent authenticate itself via "OAuth2 -
Client Credentials Flow" and the JCP APIs service will return a JWT token that
will be used to authenticate the JOD Agent in the subsequent requests. This
token is stored in the `JCPAPIsClientObj' object, so it can provide all methods
used to send the requests to the JCP APIs.


## Message routing

More than manage the local server and the cloud connection, the [JODCommunication](../../src/main/java/com/robypomper/josp/jod/comm/JODCommunication.java)
class is responsible for the message routing between the current JOD Agent,
the local JOSP Services and the JCP Gateway. In order to support the message flows
the JODCommunication class provides a set of methods to send and process
messages.

Based on the [JOSP Protocol](../josp_comps/josp_commons_josp_protocol.md), those are the
available flows:
- Current JOD Agent to All (all Local JOSP Services and JCP GWs)
- Current JOD Agent to Specific Local JOSP Services
- Current JOD Agent to JCP GWs
- Local JOSP Services to Current JOD Agent
- JCP GWs to Current JOD Agent

Methods to send messages to JOSP Services are:
- *JODCommunication::sendToServices(String msg, JOSPPerm.Type minReqPerm)*
- *JODCommunication::sendToCloud(String msg, JOSPPerm.Type minReqPerm)*
- *JODCommunication::sendToSingleLocalService(String msg, JOSPPerm.Type minReqPerm)*

Main method to process all incoming JOSP Protocol's messages is the
*JODCommunication::processFromServiceMsg(String msg, JOSPPerm.Connection conn)*
method. It's responsible for the message routing to the correct processing
method based on the message type and the JOSPPerm.Connection object.<br/>
Methods to process messages from JOSP Services are:
- *JODCommunication_002::processObjectCmdMsg()*
- *JODCommunication_002::processHistoryCompStatusMsg()*
- *JODCommunication_002::processHistoryEventsMsg()*
- *JODCommunication_002::processObjectSetNameMsg()*
- *JODCommunication_002::processObjectSetOwnerIdMsg()*
- *JODCommunication_002::processObjectAddPermMsg()*
- *JODCommunication_002::processObjectUpdPermMsg()*
- *JODCommunication_002::processObjectRemPermMsg()*


## Permissions checking

JODCommunication class is also responsible for the permissions checking.<br/>
The JOSPPerm class is used to manage the permissions for the JOD Agent and the
JOSP Services. It's used to check the permissions for the incoming messages,
method *JODCommunication::processFromServiceMsg(String msg, JOSPPerm.Connection conn)*;
and to manage the permissions for the outgoing messages using the
*JODCommunication::sendToServices(String msg, JOSPPerm.Type minReqPerm)*
and *JODCommunication::sendToSingleLocalService(String msg, JOSPPerm.Type minReqPerm)*
methods.

For the incoming messages, the required level of permission is defined by the
*JODCommunication::processFromServiceMsg(String msg, JOSPPerm.Connection conn)*
method based on the message type.<br/>
For the outgoing messages, the required level of permission is defined by the
original method that calls the *JODCommunication::sendToXY()* methods.


## JOSP Object Daemon - Communication configs

The JOD Agent's communication configuration is defined in the
`jod.yml` [configuration file](jod_yml.md). The properties that define
the communication are:

* `jod.comm.local.enabled` ("true"): If 'true' the server for Local Communication will be enabled, otherwise it will not be started.
* `jod.comm.local.discovery` ("Auto"): Discovery implementation for the Local Communication server.
* See the [JOD Local Communication configs](communication_local.md#jod-local-communication-configs)
* `jod.comm.cloud.enabled` ("true"): If 'true' the server for Local Communication will be enabled, otherwise it will not be started.
* See the [JOD Cloud Communication configs](communication_cloud.md#jod-cloud-communication-configs)

The available [Discovery System implementations](../josp_comps/josp_commons_discovery.md)
are:

* `avahi`: Avahi cmdline tool
* `dnssd`: DNS-SD cmdline tool
* `jmdns`: JmDNS Java library
* `jmmdns`: JmmDNS Java library
