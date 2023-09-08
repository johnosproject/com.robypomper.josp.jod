# JOD - Specs: Encryption

[SPECS](../specs.md) | [IMPLS](../impls.md) | [CHANGELOG](../../CHANGELOG.md) | [TODOs](../../TODOs.md) | [LICENCE](../../LICENCE.md)

With the intention of **providing the highest level of security**, both the
[JOD Gateway O2S Client](#cloud-communication) and the [JOD Local Server](#direct-communication)
use communication channels based on TCP and encrypted with SSL. For Cloud
communication, the SSL encryption use a pre-shared certificate on the JCP GWs
server side; and a self-generated certificate as JOSP Object's identity.
Each JOSP Object, register his self-generated certificate to the JCP when requires
the JCP Gateways' access info to JCP APIs. On other hand, the Direct communication
requires a pre-connection step where JOSP Object and Service share their own
certificates. After that, they can open a direct encrypted communication.

Also, the [Public JCP](/docs/comps/jcp/public_jcp.md), with the aim to
improve your security, expose only HTTPs endpoints and encrypt all communication
with his own SSL certificate.
