# JOSP Object Daemon - Specs: Logs

JOD Agent logs are stored in the ```logs/``` dir.
File logs are configured as a [RollingFile](https://logging.apache.org/log4j/2.x/manual/appenders.html#RollingFileAppender) file.
That means, each time the log file reaches the size of 5MB, a new log file is
created and the old one is compressed and renamed.

## `log4j2.xml`

...
