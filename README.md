# JOSP Object Daemon

Into this repository are contained all sources for the JOD agent from the
[John O.S. Project](https://www.johnosproject.com)

<table><tr>
<td>
<img src="docs/JOSP_JOD_Logo_250.png" width="100">
</td>
<td>

**Artifact Name:** jospJOD<br />
**Artifact Group:** com.robypomper.josp<br />
**Artifact Version:** 2.2.4

</td>
</tr>
</table>

[README](README.md) | [SPECS](docs/specs.md) | [IMPLS](docs/impls.md) | [CHANGELOG](CHANGELOG.md) | [TODOs](TODOs.md) | [LICENCE](LICENCE.md)


The JOSP Object Daemon is a simple daemon that provide a JOSP Object. When it
is running, you can interact with it using a JOSP Service or the JSL Shell
itself.

**NB:** This repository, by default, executes the JOD with his interactive
shell as a foreground cmdline application. To change this behaviour, please set
the property `isDaemon` to `true` into `gradle/artifacts.gradle` file.


## Run

This is a Java application and this repository use the Gradle build system. So
you can run this app directly from the Gradle wrapper included into the sources.

Remember that you'll need Java installed on your machine.

```commandLine
$ ./gradlew run     // for Linux/Mac
$ gradlew.bat run   // for Windows
```

The run task will compile, assemble and run the JOD agent using the
`envs/runnables/jod` folder as working dir. In this directory you'll find all
configs and log files used/generated by the JOD instance.

Next time, you'll execute the `run` task, it will use the same files so any
changes against multiple execution will not be overwritten.<br/>
To reset the JOD instance, remove his working dir.

It is possible also customize which file will be copied into the working dir.
Simply open the `gradle/artifacts.gradle` files and edit the properties before
the `run` task definition. More info on [Build System > Run and clean](#run-and-clean)
tasks section.

### CmdLine args

The JOD agent takes only one optional args from the command line: `--configs`.
This arg allow defines another path for the `configs/jod.yml` config file.
<br/>More details on [Specs/CmdLine](docs/specs/cmdline.md) page.

### Configs files

The JOD agent use the following configuration files. All files must be relative
to the JOD execution dir.

| File                 | Specs                                          | Description                                                                                                                                                                                                                                                                                                                       |
|----------------------|------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `configs/jod.yml`    | [Specs/`jod.yml`](docs/specs/jod_yml.md)       | Main JOD agent config file. It's a YAML file that contains all configuration used by the agent: from the object's id to the workers' types.                                                                                                                                                                                       |
| `configs/struct.jod` | [Specs/Structure](docs/specs/structure.md)     | A JSON file that contains only the object's structure.                                                                                                                                                                                                                                                                            |
| `configs/perms.jod`  | [Specs/Permissions](docs/specs/permissions.md) | A `;`-separated file that contains pairs key-values. It defines which permission must be applied to the object. It's not mandatory, and it can be managed entirely by the JOD Agent. By default, the JOD agent create a permissions file with right object's id that allows connections from any service via local communication. |
| `configs/log4j2.xml` | [Specs/Logs](docs/specs/logs.md)               | XML file containing [Log4J2 configurations](https://logging.apache.org/log4j/2.x/manual/configuration.html).                                                                                                                                                                                                                      |

Checkout the configs file examples on the [Resources > Configs files](#example-configs-files)
section.


## Develop

### Organization and conventions

This project is based on Gradle build system and then include his 8.3 wrapper.
It is possible execute the build system simply with the command:

```
PROJ_DIR$ ./gradlew         # on Linux
PROJ_DIR$ ./gradlew.bat     # on Windows
```

This project follow the JOSP Project convention starting from version 2.2.4.
That means the project configs are available in to the `gradle/josp_project.gradle`
file. In this file are defined all properties used along other Gradle scripts.
Others Gradle files help to keep consistency across all JOSP Project repositories.

* `gradle.build`: main Gradle file
* `gradle/josp_definitions.gradle`: define JOSP versions and project modes
* `gradle/josp_project.gradle`: define current project/repository
* `gradle/josp_versions.gradle`: define versions for current project and his dependencies
* `gradle/publications_repo.gradle`: add current project's publication urls as build system repositories
* `gradle/artifacts.gradle`: project's artifacts' definitions from their sourceSets until their publications
* `gradle/tests.gradle`: define project's tests (sourceSets and dependencies)
* `gradle/publications.gradle`: configure the remote publications including archives signature (if private access
  enabled)
* `gradle/wrapper.gradle`: add a "create wrapper" task

### Run and clean

* `./gradlew run` \
  Execute the JOD application with his shell as a foreground cmdline application.
  It can be customized editing the `gradle/artifacts.gradle` file.


* `./gradlew clean` \
  Clean the project dir.

### Build and publish

* `jar`: compile and assemble artifact's sources and resources.
* `jarDeps`: collect all artifact's dependencies.
* `jarDocs`: collect and compress all artifact's JavaDocs
* `jarSrc`: collect and compress all artifact's source files
* `javadoc`: (documentation) generate all artifact's JavaDocs

**NB:** All those `jar` tasks' outputs will be included into the main project publication.

* `publishToMavenLocal` \
  Copies all defined publications to the local Maven cache, including their
  metadata (POM files, etc.).
* `publish` \
  An aggregate task that publishes all defined publications to all defined
  repositories. It does not include copying publications to the local Maven
  cache.
* `publishJospJODToMavenLocal` \
  Copies the main project's publication to the local Maven cache along with the
  publication’s POM file and other metadata.
* `publishJospJODToMavenRepository` \
  Publishes the main project's publication to the repository named "Maven".

Local Maven cache, typically is placed into `$HOME/.m2/repository`. \
On other hands, the "Maven" repository is configured to upload artifacts to the
[Nexus/Sonatype](https://oss.sonatype.org/) public repository. More info on
this repo at [Resources > Publication repo](#publication-repository) section.

### Tests

* `test`: run all test contained into the `src/test/java` folder using the
  JUnit/Jupiter framework.


## Resources

### Example configs files

This repository include several configuration files as examples and for testing
purposes. Depending on the config file you can find examples into `src/main/configs`.
dir.

### Dependencies

This repo as part of the [John OS Project](https://www.johnosproject.com), get
most of his dependencies from other JOSP packages. Those dependencies are
defined into the [`gradle/artifacts.gradle`](/gradle/artifacts.gradle) file and
their versions are defined into the [`gradle/josp_versions.gradle`](/gradle/josp_versions.gradle)
file.

Here the list of all direct JOSP JOD dependencies:

* `com.robypomper.josp:jospCommons` : [Commons](https://github.com/johnosproject/com.robypomper.josp.commons) library from JOSP Project
* `net.sourceforge.htmlcleaner:htmlcleaner` : HTML parser [HtmlCleaner](https://htmlcleaner.sourceforge.net/)
* `com.jayway.jsonpath:json-path` : A Java DSL for reading JSON documents [JSONPath](https://github.com/json-path/JsonPath)
* `com.github.hypfvieh:dbus-java-core` : [hypfvieh.DBus](https://github.com/hypfvieh/dbus-java) implementation
* `com.github.hypfvieh:dbus-java-transport-jnr-unixsocket` : [hypfvieh.DBus](https://github.com/hypfvieh/dbus-java) implementation
* `commons-cli:commons-cli`: Command line options parser [Commons CLI](https://commons.apache.org/proper/commons-cli/)
* `com.googlecode.clichemaven:cliche` : [Cliche](https://code.google.com/archive/p/cliche/) Command-Line Shell
* `org.apache.logging.log4j:log4j-slf4j2-impl`: [Log4J](https://logging.apache.org/log4j/2.x/)  logger's implementation

### Publication repository

Artifact's remote publications allow publishing JOSP components to remote
repository and make them public available. Because of that artifact's remote
publications are available only if ```enablePrivate``` config is ```true```.

All artifacts will be published with the base group ```com.robypomper.josp```.
Java JOSP components are published on the maven repository [sonatype.org](https://oss.sonatype.org/)
where the ```com.robypomper``` was registered with
[OSSRH-45810](https://issues.sonatype.org/browse/OSSRH-45810?page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel&focusedCommentId=595848#comment-595848)
issue request. That means, specific Nexus/Sonatype's user and password are
required to publish JOSP components, those credentials must be stored in the
`$USER_HOME/.gradle/gradle.properties` file.

Here an example of the `gradle.properties` file, where `sonatypeXY` props are
referred to the publication repo credentials; and the `signing.XY` props to the
local signing key.

```agsl
sonatypeUser={jira_username}
sonatypePassword={jira_password}

signing.keyId={last 8-digit of the GPG key}
signing.password={passphrase of the GPG key}
signing.secretKeyRingFile={file containing the GPG key}
```

**Nexus/Sonatype's references:**

* [Nexus/Sonatype portal](https://oss.sonatype.org/)
* [Staged artifacts](https://oss.sonatype.org/#nexus-search;quick~com.robypomper.josp)
* [Getting started](https://central.sonatype.org/publish/publish-guide/)
* [GPG Keys](https://central.sonatype.org/publish/requirements/gpg/)
* [Gradle configs](https://central.sonatype.org/publish/publish-gradle/) (old gradle plugin 'maven')
* [Confirm release](https://central.sonatype.org/publish/release/)


## Versions

This repository was based on the version `2.2.3`.

**Older version of JOSP source code:**

Previous versions are hosted on [com.robypomper.josp]() Git repository.

* v [2.2.3](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.2.3/)
* v [2.2.2](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.2.2/)
* v [2.2.1](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.2.1/)
* v [2.2.0](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.2.0/)
* v [2.1.0](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.1.0/)
* v [2.0.0](https://bitbucket.org/johnosproject_shared/com.robypomper.josp/src/2.0.0/)


## Licences

The JOD agent contained in the current repository is distributed using the
[GPLv3](LICENCE.md) licence.


## Collaborate

**Any kind of collaboration is welcome!** This is an Open Source project, so we
are happy to share our experience with other developers, makers and users. Bug
reporting, extension development, documentation and guides etc... are activities
where anybody can help to improve this project.

Every contribution, with the aim of creating a healthy community, will be subject
to the [Code of Conduct](CODE_OF_CONDUCT.md).

One of the John O.S. Project’s goals is to release more John Objects Utils & Apps
to allow connecting even more connected objects from other standards and protocols.
Checkout the Utils & Apps extensions list and start collaborating with a development
team or create your own extension.

At the same time we are always looking for new use cases and demos. So, whether
you have just an idea or are already implementing your IoT solution, don't
hesitate to contact us. We will be happy to discuss with you about technical
decisions and help build your solution with John’s component.

Please email [tech@johnosproject.com](mailto:tech@johnosproject.com).
