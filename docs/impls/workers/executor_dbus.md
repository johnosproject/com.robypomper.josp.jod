# JOSP Object Daemon - Workers/Executor/DBus

When an [JOD Action](../../specs/pillars.md#actions) must be executed, this
executor calls a Remote Method via DBus. DBus name, object's path and all others
methods info are specified into the worker's configs' string.

Before call the DBus Object's method, the method's parameters are updated and
all their placeholder are replaced with current [Pillar](../../specs/workers_placeholders.md#pillar), [State](../../specs/workers_placeholders.md#state)
and [Action](../../specs/workers_placeholders.md#action) properties.

---

## Firmware Configs

The following configs must be specified by the DBus object/method's provider.

### ```dbus_name```

It defines on which DBus looks for the DBus Object's path.<br/>
This value normally is expressed as reverse url: `com.example.test`.

### ```dbus_obj_path```

The Object's path to looks for.<br/>
This config is NOT mandatory and by default it is `/`.<br/>
Commonly it includes also the DBus name with slashes instead dots, e.g. `/com/example/test/`.

When a DBus Object is request on the DBus, this executor checks also the object's
interface (see `dbus_iface`) before approve the match.

### ```dbus_iface```

The Object's interface is defined into the DBus descriptor from the object/method's
provider.<br/>
Commonly it includes also the DBus name with slashes instead dots, e.g. `com.example.test.IObject`.

This value is used by this executor to check the match with available DBus's Objects.

### ```dbus_method```

Simply the name of the remote method to execute, it is defined into the DBus Object definition.

### ```dbus_method_params```

A comma-separated list of `value: type` pairs that define the methods params.
The arg's `type` defines which class to use to instantiate the params. All
classes from the `java.lang` package can omit the package name. Otherwise, the
full class name is required.
Then the `value` field is used as String parameter for the `type` class. So,
for custom classes, remember to add a constructor with a single String param.

Here some example:

* `1:Integer`
* `1:Integer, MyText: String`
* `1: Integer, 1.1: Float, {...json...}: com.example.CustomClass`

If some error occurs during the argument creation, the executor continue and
use the arg's string as argument, moreover it will be printed a detailed message
on the JOD's logs as following:

```
2023-09-08 13:44:04,734 # [ WARN  |                 | 6. PEER_INF_LOO | c.r.j.j.e.i.d.ExecutorDBus     ] ExecutorDBus 'Play/Pause' error on converting method parameter, error on 'Float(String)' constructor found for 'TRUE' value. Use his value as String.
2023-09-08 13:44:04,736 # [ INFO  | JOD_EXEC_SUB    | 6. PEER_INF_LOO | c.r.j.j.e.i.d.ExecutorDBus     ] Executor 'Play/Pause' executed method 'PlayPause(true:Boolean,TRUE:String,TRUE:String)' => 'null'
```

Before, the `dbus_method_params` string is converted into an Objects array, it
is processed by the Workers substitutor. That means, configuring the method's
params you can use any one of the [Workers PlaceHolder](../../specs/workers_placeholders.md).

As an example, the following string will define two args (both String) that contains
respectively: the new value to set, and the current value.

`%A_VAL%: String, %S_VAL%: String`

For more info checkout the [State's](../../specs/workers_placeholders.md#state)
and [Action's Placeholder](../../specs/workers_placeholders.md#action) pages.

---

## Examples

```json title="struct.jod: BoolenAction/DBus @ JOD Struct DBus file"
"Play/Pause" : {
    "type": "BooleanAction",
    "listener" : "dbus://dbus_name=org.mpris.MediaPlayer2.vlc;dbus_obj_path=/org/mpris/MediaPlayer2;dbus_iface=org.mpris.MediaPlayer2.Player;dbus_prop=PlaybackStatus;init_data=1;",
    "executor" : "dbus://dbus_name=org.mpris.MediaPlayer2.vlc;dbus_obj_path=/org/mpris/MediaPlayer2;dbus_iface=org.mpris.MediaPlayer2.Player;dbus_method=PlayPause;dbus_method_params="
}
```

```json title="struct.jod: RangeAction/DBus"
"Action 0-50" : {
    "type": "RangeAction",
    "listener" : "file://path=status/%COMP_NAME%_State.txt",
    "executor" : "dbus://dbus_name=org.mpris.MediaPlayer2.vlc;dbus_obj_path=/org/mpris/MediaPlayer2;dbus_iface=org.mpris.MediaPlayer2.Player;dbus_method=PlayPause;dbus_method_params=%A_VAL%:Boolean, %A_VAL%:String;"
    "min": "0",
    "max": "50",
    "step": "5"
}
```
