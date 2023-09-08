# JOD - Specs: Communication

[SPECS](../specs.md) | [IMPLS](../impls.md) | [CHANGELOG](../../CHANGELOG.md) | [TODOs](../../TODOs.md) | [LICENCE](../../LICENCE.md)

**To connect  a [JOSP Object](/docs/features/objects_integration.md) to a [JOSP EcoSystem](/docs/features/ecosystem.md),
the JOD Agent use a complex communication system.**<br/>
This system is delegated to handle communication with JOSP Services (both
[Direct](/docs/features/communication.md#direct-communication) and [Cloud](/docs/features/communication.md#cloud-communication)),
manage JCP Authentication, but also to encrypt all tx/rx data.

Because a **JOSP EcoSystem is composed by different 'pieces', a JOD Agent use
multiple communication channels** with different protocols to talks with all those 'pieces':

* to JCP APIs (via HTTP) to register and handle the JOSP object to the JCP
* to JCP Gateways (via JOSP) to communicate with JOSP Services via Cloud Communication
* from JOSP Services (via JOSP) to communicate with JOSP Services via Direct Communication

[JCP APIs](/docs/comps/jcp/core/apis/README.md) are a set of HTTP Methods
exposed by the JCP platform and are used by JOD Agent to handle the JOSP Object
on the JCP. That include: the generation of a unique object's id, the supply of
the JCP GWs access info, the storage of JOSP Object's events and status histories,
etc...<br/>
Moreover JCP APIs requires [JOD Agent authentication](#jcp-authentication) via
the JCP Auth service (an OAuth2 authorization server).

To communicate with JOSP Services, the JOD Agent support both JOSP Communications
types: [Direct](/docs/features/communication.md#direct-communication) and [Cloud](/docs/features/communication.md#cloud-communication).
