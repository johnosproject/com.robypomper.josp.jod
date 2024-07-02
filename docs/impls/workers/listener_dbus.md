# JOSP Object Daemon - Workers/Executor/DBus


This listener monitors ```dbus_prop``` property of the specified DBus Object,
then update the [JOD State](../../specs/pillars.md#states) with the received value.

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

### ```dbus_prop```

Simply the name of the property to monitor, it is defined into the DBus Object definition.

---

## Firmware Configs - Formatter

Once an DBus property send an update, the property value is parsed and
formatted depending on those Firmware Configs.

That means, the properties' values are parsed using the ```formatType``` format.
Then can be extracted part of the value, using the ```formatPathType``` and
```formatPath``` configs. Finally, only the extracted string is used as Pillar's
state.

To avoid any value alteration, you can use the ```TXT``` value (the default one) in
the ```formatType``` config. That will thread the property's value as raw text and,
all contents from the value is used as Pillar's state.

Those configs are defined and used by [FormatterInternal](/src/main/java/com/robypomper/josp/jod/executor/impls/http/FormatterInternal.java) class.

### ```formatType```

HTTP Response's body format. Default 'TXT'.

Please set this FirmwareConfigs according to the expected response type.<br/>
You can choose one of the following values:

* ```TXT```: raw text response, this format does not alter the response body.
* ```HTML```: for HTML responses that can be queried with XPath expression or with a TagName (Not Yet Implemented).
* ```XML```: for XML responses (Not Yet Implemented).
* ```JSON```: for JSON responses that can be queried with JSONPath expression.
* ```YML```: for YML responses (Not Yet Implemented).

Those values are coming from the [FormatterInternal::FormatType](/src/main/java/com/robypomper/josp/jod/executor/impls/http/FormatterInternal.java) enum.

This property is updated with [Pillar's Placeholder](../../specs/workers_placeholders.md#pillar)
on worker initialization.

### ```formatPath```

Path used to query the HTTP Response, this value depends on ```formatPathType```
Firmware Config. Default '/'.

Please set this FirmwareConfigs according to the expected response type.<br/>
Depending on the ```formatPathType``` value you can use different path syntax:

* ```XPATH```: an expression format to identify nodes in XML documents ([W3School tutorial](https://www.w3schools.com/xml/xpath_intro.asp), [XPather: Online XPath Tester](http://xpather.com/))
* ```TAG_NAME```: just write the tag name, then his content will be used as formatted response
* ```JSONPATH```: an expression format to identify nodes in JSON documents ([Jayway JsonPath](https://github.com/json-path/JsonPath), [JsonPath.com: Online JsonPath Tester](https://jsonpath.com/))

This property is updated with [Pillar's Placeholder](../../specs/workers_placeholders.md#pillar)
on worker initialization.

### ```formatPathType```

HTTP Response's body format type. Default ''.

Please set this FirmwareConfigs according to the expected response type.<br/>
You can choose one of the following values:

* ```XPATH```: to query HTTP Responses with HTML and XML (Not Yet Implemented) bodies.
* ```TAG_NAME```: to query HTTP Responses with HTML and XML bodies (Not Yet Implemented).
* ```JSONPATH```: to query HTTP Responses with JSON bodies.

Those values are coming from the [FormatterInternal](/src/main/java/com/robypomper/josp/jod/executor/impls/http/FormatterInternal.java) class.

This property is updated with [Pillar's Placeholder](../../specs/workers_placeholders.md#pillar)
on worker initialization.

---

## Firmware Configs - Evaluator

If you are not yet satisfy from the result after the HTTP Response body format,
you can continue customizing it within a configurable JavaScript script.

For example, if after response format, it still contains unwanted chars; or also
if you configured the worker for handle a raw response  (```formatType=TXT```).
Then you need to edit the obtained response before passing it as new Pillar's
state value.

To edit obtained result, after the response's body format, you must use the
```eval``` Firmware Config.<br/>
Default ```eval``` value, or an empty string, prevents any response alteration.

Those configs are defined and used by [EvaluatorInternal](/src/main/java/com/robypomper/josp/jod/executor/impls/http/EvaluatorInternal.java) class.

### ```eval```

JavaScript code to evaluate HTTP Response body after formatting it. Default
'{httpResult}'.

As an example the following string handle `httpResult` as a string and return
`TRUE` only if it contains the `Playing` string.

`httpResult.toLowerCase()=='playing'?'TRUE':'FALSE'`

This config accepts any JavaScript script and response the evaluation function
returns his output.

Custom JavaScript can use the ```httpResult``` string that contains the HTTP Response
body after formatting.<br/>
After alter this string, the script must print the desired result.

Script are executed as [Java ScriptEngine](https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngine.html).<br/>
More details on how to write JavaScript for the Java ScriptEngine at [Oracle: Java Scripting Programmer's Guide](https://docs.oracle.com/javase/7/docs/technotes/guides/scripting/programmer_guide/).

This property is updated with [Pillar's Placeholder](../../specs/workers_placeholders.md#pillar)
on worker initialization and with [State Placeholder](../../specs/workers_placeholders.md#state)
on pulling state.

---

## Examples

```json title="struct.jod: BoolenState/DBus @ JOD Struct DBus file"
"Play/Pause" : {
    "type": "BooleanAction",
    "listener" : "dbus://dbus_name=org.mpris.MediaPlayer2.vlc;dbus_obj_path=/org/mpris/MediaPlayer2;dbus_iface=org.mpris.MediaPlayer2.Player;dbus_prop=PlaybackStatus;init_data=1;",
    "executor" : "dbus://dbus_name=org.mpris.MediaPlayer2.vlc;dbus_obj_path=/org/mpris/MediaPlayer2;dbus_iface=org.mpris.MediaPlayer2.Player;dbus_method=PlayPause;dbus_method_params="
}
```

```json title="struct.jod: RangeState/DBus @ JOD Struct DBus file"
"RangeProp" : {
    "type": "RangeState",
    "listener" : "dbus://dbus_name=com.test.dbus;dbus_iface=com.test.dbus.ITest;dbus_prop=property1;init_data=1;",
    "min": "0",
    "max": "20",
    "step": "0.1"
}```
