# JOSP Object Daemon - Specs: Logs

This application is based on the Simple Logging Facade 4 Java [SLF4J](https://www.slf4j.org/)
and the [Apache Log4J](https://logging.apache.org/log4j/2.x/) logging system.
Actually, the Log4J dependency is configured to the main JOD library. But for
the future release, it will be moved to the `onlyRuntime` configurations. That
means, it will use the Log4J to handle logging messages only when the JOD Library
is executed directly (not imported as dependency).

**Run the JOD directly:**
When you run the JOD directly, you can configure the logging messages using
the `$JOD_DIR/configs/log4j2.xml` file.

By default, JOD Agent logs are stored in the ```logs/``` dir.
File logs are configured as a [RollingFile](https://logging.apache.org/log4j/2.x/manual/appenders.html#RollingFileAppender) file.
That means, each time the log file reaches the size of 5MB, a new log file is
created and the old one is compressed and renamed.

**Include the JOD Library:**
On the other case, when you include the JOD Library into your own software, you
can configure and print log messages just including a SLF4J implementation,
like the `org.apache.logging.log4j:log4j-slf4j2-impl` and set up relative configs
file (`$JOD_DIR/configs/log4j2.xml` for org.apache.logging.log4j).

Here how to include the `log4j-slf4j2-impl` as Gradle dependency into your
`build.gradle` file:

```groovy
dependencies {
    // Add log4j-slf4j2-impl as Gradle dependency
    runtime "org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0"
}
```


## Default `log4j2.xml`

Default `log4j2.xml` files are available in to the `/arc/main/configs/log4j2`
directory. Actually, two default `log4j2.xml` files are available, used for
development and release phases:

* [`log4j2_dev.xml`](/src/main/configs/log4j2/log4j2_dev.xml)
* [`log4j2_release.xml`](/src/main/configs/log4j2/log4j2_release.xml)

Both files are configured to print log messages to the console and to the file.
Also for both files, the log messages are grouped in 3 packages: artifact, josp
libraries and all the others.<br/>
These files differ in the log levels:

| Phase   | Package  | Console | File  |
|---------|----------|---------|-------|
| Develop | Artifact | DEBUG   | ALL   |
| Develop | JOSP     | DEBUG   | ALL   |
| Develop | Others   | WARN    | INFO  |
| Release | Artifact | INFO    | DEBUG |
| Release | JOSP     | FATAL   | INFO  |
| Release | Others   | FATAL   | WARN  |


## Customize `log4j2.xml`

The JOD Agent's log messages are grouped in packages, so you can configure the
log messages for each package separately. The JOD Agent's log messages include
also the JOSP Commons library's log messages. Here the full list of the JOD Agent's
loggers:

**From JOD:**
* `com.robypomper.josp.jod.objinfo`
* `com.robypomper.josp.jod.structure`
* `com.robypomper.josp.jod.executor`
* `com.robypomper.josp.jod.comm`
* `com.robypomper.josp.jod.permissions`
* `com.robypomper.josp.jod.history`
* `com.robypomper.josp.jod.events`
* `com.robypomper.josp.jod.shell`

**From JOSP Commons:**
* `com.robypomper.comm`
* `com.robypomper.discovery`
* `com.robypomper.josp`
* `com.robypomper.java`
* `com.robypomper.settings`
* `javax.jmdns`

Here an example for the `log4j2.xml` config file:

```xml
<Configuration>
    <Loggers>
        
        <Logger name="com.robypomper.josp.jod" additivity="false">
            <AppenderRef ref="consoleInfo_fileDebug"/>
        </Logger>
        
        <Logger name="com.robypomper.josp.jod.structure" additivity="false">
            <AppenderRef ref="consoleDebug_fileDebug"/>
        </Logger>

        <Logger name="com.robypomper" additivity="false">
            <AppenderRef ref="consoleNone_fileWarning"/>
        </Logger>
        
        <Logger name="javax.jmdns" additivity="false">
            <AppenderRef ref="consoleNone_fileWarning"/>
        </Logger>
        
    </Loggers>
</Configuration>
```
