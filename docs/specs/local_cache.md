# JOSP Object Daemon - Specs: Local Cache

JOD Agent sync Events and Status' Histories to the JCP.

To allow objects register events and status histories also when they are not
connected to the JCP, the JOD Agent use the files contained in the ```cache/```
dir.

* `event.jst`: object's events cache state
* `event.jbe`: object's events data
* `history.jst`: statuses histories cache state
* `history.jbe`: statuses histories data

The local cache is based on the two classes:

* *[JODEvents](../../src/main/java/com/robypomper/josp/jod/events/JODEvents.java)*
* *[JODHistory](../../src/main/java/com/robypomper/josp/jod/history/JODHistory.java)*

Each one of those classes provides the methods to store/retrieve the related
data and sync them with the JCP.

## Data files

Data files are JSON arrays ordered DESC by their timestamp. The most recent item
is the first item in the file.

To support the JOD Agent in the data management, there is the `JavaJSONArrayToFile`:
a utility class to help store and retrieve JSON arrays from files.
This class provides also an internal buffer to store the data in memory and
reduce the disk access.

When the memory buffer is full, the data are flushed to the file.<br/>
When also the file is full, the oldest data are deleted.<br/>
Between the data generation and his deletion, the JOD Agent try to upload them
to the JCP.

Buffer size and file size are highly dependent on the available disk space and
the data generation rate. So, they are configurable via the `jod.yml`
[configuration file](jod_yml.md). For more info see the [JOD Local Cache configs](#jod-local-cache-configs)
section.


## Cache state file

Those kind of files are used to store the JCP synchronization state.

With the information contained in the cache state file, the JOD Agent can
understand what data must be uploaded to the JCP and what data can be deleted
safely. In addition, the JOD Agent can understand if some data has been lost
because deleted before being synchronized.

Here an example from a JOD Agent never connected to the JCP. The cache state
file contains the following data:

```json
{
  "lastRegistered" : 5965,
  "lastStored" : 5965,
  "lastUploaded" : -1,
  "lastDelete" : -1,
  "registeredCount" : 5965,
  "storedCount" : 5340,
  "uploadedCount" : 0,
  "deletedCount" : 0,
  "lostCount" : 0
}
```


## JOSP Object Daemon - Local Cache configs

The JOD Agent's local cache configuration is defined in the
`jod.yml` [configuration file](jod_yml.md). The properties that define
the local cache are:

* `jod.history.keep_in_memory` ("false"): If 'true' the history file will be retained in memory                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
* `jod.history.buffer_size` ("250"): Size of the history buffer.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
* `jod.history.buffer_release_size` ("200"): Number of history's items to flush on the file when the buffer is full.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
* `jod.history.file_size` ("10000"): Size of the history file.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
* `jod.history.file_release_size` ("2000"): Number of history's items to delete from the file when it is full.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
* `jod.history.file_array` ("./cache/history.jbs"): File path for history's file items.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
* `jod.history.file_stats` ("./cache/history.jst"): File path for history's file stats.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
* `jod.events.keep_in_memory` ("false"): If 'true' the events file will be retained in memory                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
* `jod.events.buffer_size` ("250"): Size of the events buffer.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
* `jod.events.buffer_release_size` ("200"): Number of event's items to flush on the file when the buffer is full.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
* `jod.events.file_size` ("10000"): Size of the events file.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
* `jod.events.file_release_size` ("2000"): Number of event's items to delete from the file when it is full.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
* `jod.events.file_array` ("./cache/events.jbs"): File path for event's file items.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
* `jod.events.file_stats` ("./cache/events.jst"): File path for event's file stats.
