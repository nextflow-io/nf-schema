---
description: Return a summary of the parameters being set.
---

# Summary log

## `paramsSummaryLog()`

This function returns a string that can be logged to the terminal, summarizing the parameters provided to the pipeline.

!!! note

    The summary prioritizes displaying only the parameters that are **different** than the default schema values.
    Parameters which don't have a default in the JSON Schema and which have a value of `null`, `""`, `false` or `'false'` won't be returned in the map.
    This is to streamline the extensive parameter lists often associated with pipelines, and highlight the customized elements.
    This feature is essential for users to verify their configurations, like checking for typos or confirming proper resolution,
    without wading through an array of default settings.

The function takes one required argument:

- The `WorkflowMetadata` object, `workflow` (required)

And optional arguments:

- `parameters_schema`: File name of a schema file (optional, default: `nextflow_schema.json`).
- `monochrome_logs`: Boolean to disable coloured logs (optional, default: `false`).
- `beforeText`: String to add before the summary log (optional, default: `''`).
- `afterText`: String to add after the summary log (optional, default: `''`).
- `mask`: Mask value, when replacing bucket names or subpaths (optional, default: `[** masked **]`).
- `maskSubpaths`: A list of subpaths to mask from path values (optional, default: `[]`).

Typical usage:

=== "main.nf"

    ```groovy
    --8<-- "examples/paramsSummaryLog/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/paramsSummaryLog/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/paramsSummaryLog/pipeline/nextflow_schema.json"
    ```

Output:

```
--8<-- "examples/paramsSummaryLog/log.txt"
```

## Coloured logs

By default, the summary output is coloured using ANSI escape codes.

If you prefer, you can disable these by using the argument monochrome_logs, e.g. `paramsHelp(monochrome_logs: true)`. Alternatively this can be set at a global level via parameter `--monochrome_logs` or adding `params.monochrome_logs = true` to a configuration file. Not `--monochromeLogs` or `params.monochromeLogs` is also supported.

=== "Default (coloured)"

    ![Default summary logs](../images/summary_not_monochrome_logs.png)

=== "Monochrome logs"

    ![Default summary logs](../images/summary_monochrome_logs.png)

## `paramsSummaryMap()`

This function returns a Groovy Map summarizing parameters/workflow options used by the pipeline.
As above, it **only** returns the provided parameters that are **different** to the default values.

This function takes the same arguments as `paramsSummaryLog()`: the `workflow` object and an optional schema file path.

!!! note

    Parameters which don't have a default in the JSON Schema and which have a value of `null`, `""`, `false` or `'false'` won't be returned in the map.

Typical usage:

=== "main.nf"

    ```groovy
    --8<-- "examples/paramsSummaryMap/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/paramsSummaryMap/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/paramsSummaryMap/pipeline/nextflow_schema.json"
    ```

Output:

```
--8<-- "examples/paramsSummaryMap/log.txt"
```
