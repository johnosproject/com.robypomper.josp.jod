# JOSP Object Daemon - Changelog

[README](README.md) | [SPECS](docs/specs.md) | [IMPLS](docs/impls.md) | [CHANGELOG](CHANGELOG.md) | [TODOs](TODOs.md) | [LICENCE](LICENCE.md)


## Version 2.2.4

* Improved the JOSP Object Daemon documentation
* Updated to JOSP Commons 2.2.4
* Updated com.jayway.jsonpath to 2.9.0 because the CVE-2023-51074
* Added integration tests
* Removed log4j-core dependency and all related Markers
* Replaced slf4j-api dependency with log4j-slf4j2-impl
* Updated GradleBuildInfo to version 2
* Workers
  * Added DBus Workers (Listener and Executor)
  * Updated HTTP's Formatter default type value to `TXT`
  * Added Random capability to ListenerTestAdv
* Communication
  * Added Security Levels to Local Communication
  * Updated SSL certificate provisioning
  * Added JOD settings for SSL certificate provisioning
  * Various fixes to Local server
* History & Events
  * Updated History and Events to use maxFileSize to prevent OutOfMemoryError
  * Added JOD settings for History and Events for auto-flush and maxFileSize


## Isolate JOD 2.2.3

* Removed all NOT JOD files
* Moved required files to jospJOD sourceSet
* Removed all NOT JOD Gradle configs
* Cleaned Gradle configs and tasks
* Moved jospJOD sourceSet to main sourceSet
* Moved all tests sourceSet to test sourceSet
* Updated all dependencies to latest versions
* Changed default jospDependenciesVersion behaviour
* Updated the Gradle Wrapper to version 8.3
* Removed buildSrc dependency
* Imported JOD docs from old repository
* Created TODOs.md
* Updated README.md, CHANGELOG.md and LICENCE.md to updated JOD repository
