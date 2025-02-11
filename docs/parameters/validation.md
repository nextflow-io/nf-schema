---
title: Validation
description: Validation of pipeline parameters at run time.
---

# Validation of pipeline parameters

## `validateParameters()`

This function takes all pipeline parameters and checks that they adhere to the specifications defined in the JSON Schema.

- It does not return anything, but logs errors or warnings indicating the parameters that failed to the command line.
- If any parameter validation has failed, it throws a `SchemaValidationException` exception to stop the pipeline.
- If any parameters in the schema reference a sample sheet schema with `schema`, that file is loaded and validated.

The function takes two optional arguments:

- The filename of a JSON Schema file (optional, default: `nextflow_schema.json`). File paths should be relative to the root of the pipeline directory.
- A boolean to disable coloured outputs (optional, default: `false`). The output is coloured using ANSI escape codes by default.

You can provide the parameters as follows:

```nextflow
validateParameters(parameters_schema: 'custom_nextflow_parameters.json', monochrome_logs: true)
```

Monochrome logs can also be set globally providing the parameter `--monochrome_logs` or adding `params.monochrome_logs = true` to a configuration file. The form `--monochromeLogs` is also supported.

!!! tip

    As much of the Nextflow ecosystem assumes the `nextflow_schema.json` filename, it's recommended to stick with the default, if possible.

See the [Schema specification](../nextflow_schema/nextflow_schema_specification.md) for information about what validation data you can encode within the schema for each parameter.

### Example

The example below has a deliberate typo in `params.input` (`.txt` instead of `.csv`).
The validation function catches this for two reasons:

- The filename doesn't match the expected pattern (here checking file extensions)
- The supplied file doesn't exist

The function causes Nextflow to exit immediately with an error.

=== ":material-close-thick: Output"

    ```
    --8<-- "examples/validateParameters/log.txt"
    ```

=== "main.nf"

    ```groovy
    --8<-- "examples/validateParameters/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy hl_lines="6"
    --8<-- "examples/validateParameters/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json linenums="1" hl_lines="17 20"
    --8<-- "examples/validateParameters/pipeline/nextflow_schema.json"
    ```

## Ignoring Parameters

Users can turn off validation for specific parameters using `validation.ignoreParams`, which accepts a list of parameter names.

## Failing for unrecognized parameters

When parameters which are not specified in the JSON Schema are provided, the parameter validation function returns a `WARNING`.
This is because user-specific institutional configuration profiles may make use of params that are unknown to the pipeline.

The down-side of this is that warnings about typos in parameters can go unnoticed.

To force the pipeline execution to fail with an error instead, you can provide the `validation.failUnrecognisedParams = true` configuration option:

=== ":material-check: Default"

    === "Output"

        ```
        --8<-- "examples/validationWarnUnrecognisedParams/log.txt"
        ```

    === "nextflow.config"

        ```groovy
        --8<-- "examples/validationWarnUnrecognisedParams/pipeline/nextflow.config"
        ```
    === "main.nf"

        ```groovy
        --8<-- "examples/validationWarnUnrecognisedParams/pipeline/main.nf"
        ```

=== ":material-close-thick: Fail unrecognised params"

    === "Output"

        ```
        --8<-- "examples/validationFailUnrecognisedParams/log.txt"
        ```

    === "nextflow.config"

        ```groovy hl_lines="5"
        --8<-- "examples/validationFailUnrecognisedParams/pipeline/nextflow.config"
        ```
    === "main.nf"

        ```groovy
        --8<-- "examples/validationFailUnrecognisedParams/pipeline/main.nf"
        ```

## Ignoring unrecognized parameters

Sometimes, a parameter that you want to set may not be described in the pipeline schema for a good reason.
Maybe it's something you're using in your Nextflow configuration setup for your compute environment,
or it's a complex parameter that cannot be handled in the schema, such as [nested parameters](../nextflow_schema/nextflow_schema_specification.md#nested-parameters).

In these cases, to avoid getting warnings when an unrecognised parameter is set,
you can use `--validationSchemaIgnoreParams` / `params.validationSchemaIgnoreParams`.

This should be a comma-separated list of strings that correspond to parameter names.

## Variable type checking

By default, `validateParameters()` is strict about expecting parameters to adhere to their expected type.
If the schema says that `params.foo` should be an `integer` and the user sets `params.foo = "12"` (a string with a number), it will fail.

If this causes problems, the user can run validation in "lenient mode", whereby the JSON Schema validation tries to _cast_ parameters to their correct type.
For example, providing an integer as a string will no longer fail validation.

!!! note

    The validation does not affect the parameter variable types in your pipeline.
    It attempts to cast a temporary copy of the params only, during the validation step.

To enable lenient validation mode, set `validation.lenientMode = true` in your configuration file.
