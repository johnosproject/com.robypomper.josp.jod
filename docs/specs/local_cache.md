# JOD - Specs: Local Cache

[SPECS](../specs.md) | [IMPLS](../impls.md) | [CHANGELOG](../../CHANGELOG.md) | [TODOs](../../TODOs.md) | [LICENCE](../../LICENCE.md)

JOD Agent sync events and status histories to the JCP.

To allow objects register events and status histories also when are not connected
to the JCP, the JOD Agent use files contained in the ```cache/``` dir.

* event.jst: object's events cache state
* event.jbe: object's events data
* history.jst: statuses histories data
* history.jbe: statuses histories data
