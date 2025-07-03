---
title: Configuration
description: Description of all possible configuration options for nf-schema
---

# Configuration

The plugin can be configured using several configuration options. These options have to be in the `validation` scope which means you can write them in two ways:

```groovy
validation.<option> = <value>
```

OR

```groovy
validation {
    <option1> = <value1>
    <option2> = <value2>
}
```

## parametersSchema

This option can be used to set the parameters JSON schema to be used by the plugin. This will affect parameter validation (`validateParameters()`), the summary logs (`paramsSummaryLog()` and `paramsSummaryMap()`) and the creation of the help messages.

```groovy
validation.parametersSchema = "path/to/schema.json" // default "nextflow_schema.json"
```

This option can either be a path relative to the root of the pipeline directory or a full path to the JSON schema (Be wary to not use hardcoded local paths to ensure your pipeline will keep working on other systems)

## monochromeLogs

This option can be used to turn of the colored logs from nf-schema. This can be useful if you run a Nextflow pipeline in an environment that doesn't support colored logging. This option will automatically be set to `true` when ANSI logging is disabled in Nextflow.

```groovy
validation.monochromeLogs = <true|false> // default: false
```

## lenientMode

This option can be used to make the type validation more lenient. In normal cases a value of `"12"` will fail if the type is an `integer`. This will succeed in lenient mode since that string can be cast to an `integer`.

```groovy
validation.lenientMode = <true|false> // default: false
```

## failUnrecognisedParams

By default the `validateParameters()` function will only give a warning if an unrecognised parameter has been given. This usually indicates that a typo has been made and can be easily overlooked when the plugin only emits a warning. You can turn this warning into an error with the `failUnrecognisedParams` option.

```groovy
validation.failUnrecognisedParams = <true|false> // default: false
```

## failUnrecognisedHeaders

By default the `samplesheetToList()` function will only give a warning if an unrecognised header is present in the samplesheet. This usually indicates that a typo has been made and can be easily overlooked when the plugin only emits a warning. You can turn this warning into an error with the `failUnrecognisedHeaders` option.

```groovy
validation.failUnrecognisedHeaders = <true|false> // default: false
```

## showHiddenParams

!!! deprecated

    This configuration option has been <b>deprecated</b> since v2.1.0. Please use `validation.help.showHidden` instead.

By default the parameters, that have the `"hidden": true` annotation in the JSON schema, will not be shown in the help message. Turning on this option will make sure the hidden parameters are also shown.

```groovy
validation.showHiddenParams = <true|false> // default: false
```

## ignoreParams

This option can be used to turn off the validation for certain parameters. It takes a list of parameter names as input.
Currently, the parameter `nf_test_output` is added to `ignoreParams` by default.

```groovy
validation.ignoreParams = ["param1", "param2"] // default: []
```

## defaultIgnoreParams

!!! warning

    This option should only be used by pipeline developers

This option does exactly the same as `validation.ignoreParams`, but provides pipeline developers with a way to set the default parameters that should be ignored. This way the pipeline users don't have to re-specify the default ignored parameters when using the `validation.ignoreParams` option.

```groovy
validation.defaultIgnoreParams = ["param1", "param2"] // default: []
```

## maxErrValSize

Configure the maximum characters of a value that may be shown in an error message. It takes a whole number above or equal to `1`. A value will be truncated when it goes over the maximum amount of characters.

Setting this option to `-1` will allow any amount of characters for values.

See the below example where the limit is to 20 characters:

```
* --test (abcdefghij...qrstuvwxyz): Value is [string] but should be [integer]
```

```groovy
validation.maxErrValSize = 100 // default: 150
```

## help

The `validation.help` config scope can be used to configure the creation of the help message.

This scope contains the following options:

### enabled

This option is used to enable the creation of the help message when the help parameters are used in the `nextflow run` command.

```groovy
validation.help.enabled = true // default: false
```

### shortParameter

This option can be used to change the `--help` parameter to another parameter. This parameter will print out the help message with all top level parameters.

```groovy
validation.help.shortParameter = "giveMeHelp" // default: "help"
```

`--giveMeHelp` will now display the help message instead of `--help` for this example. This parameter will print out the help message.

### fullParameter

This option can be used to change the `--helpFull` parameter to another parameter.

```groovy
validation.help.shortParameter = "giveMeHelpFull" // default: "helpFull"
```

`--giveMeHelpFull` will now display the expanded help message instead of `--helpFull` for this example.

### showHiddenParameter

This option can be used to change the `--showHidden` parameter to another parameter. This parameter tells the plugin to also include the hidden parameters into the help message.

```groovy
validation.help.showHiddenParameter = "showMeThoseHiddenParams" // default: "showHidden"
```

`--showMeThoseHiddenParams ` will now make sure hidden parameters will be shown instead of `--showHidden` for this example.

### showHidden

By default the parameters, that have the `"hidden": true` annotation in the JSON schema, will not be shown in the help message. Turning on this option will make sure the hidden parameters are also shown.

```groovy
validation.help.showHidden = <true|false> // default: false
```

### beforeText

!!! example "This option does not affect the help message created by the `paramsHelp()` function"

Any string provided to this option will printed before the help message.

```groovy
validation.help.beforeText = "Running pipeline version 1.0" // default: ""
```

!!! info

    All color values (like `\033[0;31m`, which means the color red) will be filtered out when `validation.monochromeLogs` is set to `true`

### command

!!! example "This option does not affect the help message created by the `paramsHelp()` function"

This option can be used to add an example command to the help message. This will be printed after the `beforeText` and before the help message.

```groovy
validation.help.command = "nextflow run main.nf --input samplesheet.csv --outdir output" // default: ""
```

This example will print the following message:

```bash
Typical pipeline command:

  nextflow run main.nf --input samplesheet.csv --outdir output
```

!!! info

    All color values (like `\033[0;31m`, which means the color red) will be filtered out when `validation.monochromeLogs` is set to `true`

### afterText

!!! example "This option does not affect the help message created by the `paramsHelp()` function"

Any string provided to this option will be printed after the help message.

```groovy
validation.help.afterText = "Please cite the pipeline owners when using this pipeline" // default: ""
```

!!! info

    All color values (like `\033[0;31m`, which means the color red) will be filtered out when `validation.monochromeLogs` is set to `true`

### enumLength

This option can be used to set the maximum length of enum values in the help message. This is useful when the enum values are very long and would make the help message hard to read.

The default value is set to the value in the `COLUMNS` environment variable or 100 characters if that variable isn't set. Set this option to `-1` to disable the length limit.

```groovy
validation.help.enumLength = <integer>
```

## Summary

The `validation.summary` config scope can be used to configure the output of the `paramsSummaryLog()` function.

This scope contains the following options:

### beforeText

Any string provided to this option will printed before the parameters log message.

```groovy
validation.summary.beforeText = "Running pipeline version 1.0" // default: ""
```

!!! info

    All color values (like `\033[0;31m`, which means the color red) will be filtered out when `validation.monochromeLogs` is set to `true`

### afterText

Any string provided to this option will be printed after the parameters log message.

```groovy
validation.summary.afterText = "Please cite the pipeline owners when using this pipeline" // default: ""
```

!!! info

    All color values (like `\033[0;31m`, which means the color red) will be filtered out when `validation.monochromeLogs` is set to `true`

### hideParams

Takes a list of parameter names to exclude from the parameters summary created by `paramsSummaryMap()` and `paramsSummaryLog()`

```groovy
validation.summary.hideParams = ["param1", "nested.param"] // default: []
```
