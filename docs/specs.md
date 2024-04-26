# JOSP Object Daemon - Specifications

[README](../README.md) | [SPECS](specs.md) | [IMPLS](impls.md) | [CHANGELOG](../CHANGELOG.md) | [TODOs](../TODOs.md) | [LICENCE](../LICENCE.md)


## Object Info
* [jod.yml](specs/jod_yml.md)
* [Object ID](specs/object_id.md)
* Object Owner
* Object Info fields: Full ID, Object Name, JOD Version, [Structure](#structure), Brand, Model, Long desc -> Move to structure
* [JOD Info](specs/jod-info.md)

## Object's Structure
* [struct.jod](specs/structure.md)
* [Pillars](specs/pillars.md)
  * [Boolean Pillar](specs/pillars_boolean.md)
  * [Range Pillar](specs/pillars_range.md)
  * [Container Pillar](specs/pillars_container.md)
* [Workers](specs/workers.md)
  * [Placeholders](specs/workers_placeholders.md)
  * [Custom workers](specs/workers_custom.md)

## Communication
* [JOSP Commons :: JOSP Protocol](josp_comps/josp_commons_josp_protocol.md)
* [Direct and Cloud Communication](specs/communication.md)
  * [Local Communication](specs/communication_local.md)
  * [Cloud Communication](specs/communication_cloud.md)
* [JOSP Commons :: Discovery System](josp_comps/josp_commons_discovery.md)
* [JOSP Commons :: Security Levels](josp_comps/josp_commons_josp_communication_securitylevels.md)
* [Authentication](specs/auth.md)

## Permissions
* [Object's permissions](specs/permissions.md)

## History  & Events
* States' Histories
* Object's Events
* [Local Cache](specs/local_cache.md)

## JOD Shell
* [CmdLine and Args](specs/cmdline.md)
* [JOD Shell](specs/shell.md)

## Logging
* [Logs](specs/logs.md)
