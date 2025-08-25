## Configure help message

Add the following configuration to your configuration files to enable the creation of help messages:

```groovy title="nextflow.config"
validation {
    help {
        enabled = true
    }
}
```

That's it! Every time the pipeline user passes the `--help` and `--helpFull` parameters to the pipeline, the help message will be created!

The help message can be customized with a series of different options. See [help configuration](../configuration/configuration.md#help) docs for a list of all options.

## Help message

Following example shows a snippet of a JSON schema which can be used to perfect visualize the differences between the different help messages. This schema contains one group of parameters called `Input parameters` that contains two parameters: `--input` and `--outdir`. There are also two ungrouped parameters in this schema: `--reference` and `--type`. `--reference` is a nested parameter that contains the `.fasta`, `.fai` and `.aligners` subparameters. `.aligners` also contains two subparameters: `.bwa` and `.bowtie`.

There are three different help messages:

1. Using `--help` will only show the top level parameters (`--input`, `--outdir`, `--reference` and `--type` in the example). The type, description, possible options and defaults of these parameters will also be added to the message if they are present in the JSON schema.
2. Using `--helpFull` will print all parameters (no matter how deeply nested they are) (`--input`, `--outdir`, `--reference.fasta`, `--reference.fai`, `--reference.aligners.bwa`, `--reference.aligners.bowtie` and `--type` in the example)
3. `--help` can also be used with a parameter given to it. This will print out a detailed help message of the parameter. This will also show the subparameters present for the parameter.

=== "JSON schema"

    ```json
    ...
    "$defs": { // A section to define several definition in the JSON schema
        "Input parameters": { // A group called "Input parameters"
            "properties": { // All properties (=parameters) in this group
                "input": {
                    "type": "string",
                    "description": "The input samplesheet",
                    "format": "file-path",
                    "pattern": "^.$\.csv$",
                    "help_text": "This file needs to contain all input samples",
                    "exists": true
                },
                "outdir": {
                    "type": "string",
                    "description": "The output directory",
                    "format": "directory-path",
                    "default": "results"
                }
            }
        }
    },
    "properties": { // Ungrouped parameters go here
        "reference": {
            "type": "object", // A parameter that contains nested parameters is always an "object"
            "description": "A group of parameters to configure the reference sets",
            "properties": { // All parameters nested in the --reference parameter
                "fasta": {
                    "type": "string",
                    "description": "The FASTA file"
                },
                "fai": {
                    "type": "string",
                    "description": "The FAI file"
                },
                "aligners": {
                    "type": "object",
                    "description": "A group of parameters specifying the aligner indices",
                    "properties": { // All parameters nested in the --reference.aligners parameter
                        "bwa": {
                            "type": "string",
                            "description": "The BWA index"
                        },
                        "bowtie": {
                            "type": "string",
                            "description": "The BOWTIE index"
                        }
                    }
                }
            }
        },
        "type": {
            "type": "string",
            "description": "The analysis type",
            "enum": ["WES","WGS"]
        }
    }
    ...
    ```

=== "`--help`"

    ```bash
    --reference  [object]          A group of parameters to configure the reference sets
    --type       [string]          The analysis type (accepted: WES, WGS)
    --help       [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full help message of that parameter will be printed.
    --helpFull   [boolean]         Show the help message for all non-hidden parameters.
    --showHidden [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` or `--helpFull`.

    Input parameters
        --input  [string] The input samplesheet
        --outdir [string] The output directory [default: results]
    ```

=== "`--helpFull`"

    ```bash
    --reference.fasta           [string]          The FASTA file
    --reference.fai             [string]          The FAI file
    --reference.aligners.bwa    [string]          The BWA index
    --reference.aligners.bowtie [string]          The BOWTIE index
    --type                      [string]          The analysis type (accepted: WES, WGS)
    --help                      [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full help message of that parameter will be printed.
    --helpFull                  [boolean]         Show the help message for all non-hidden parameters.
    --showHidden                [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` or `--helpFull`.

    Input parameters
        --input                 [string] The input samplesheet
        --outdir                [string] The output directory [default: results]
    ```

=== "`--help input`"

    ```bash
    --input
        type       : string
        description: The input samplesheet
        format     : file-path
        pattern    : ^.$\.csv$
        help_text  : This file needs to contain all input samples
        exists     : true
    ```

=== "`--help reference.aligners`"

    ```bash
    --reference.aligners
        type       : object
        description: A group of parameters specifying the aligner indices
        options    :
            --reference.aligners.bwa    [string] The BWA index
            --reference.aligners.bowtie [string] The BOWTIE index
    ```

The help message will always show the ungrouped parameters first. `--help`, `--helpFull` and `--showHidden` will always be automatically added to the help message. These defaults can be overwritten by adding them as ungrouped parameters to the JSON schema.

After the ungrouped parameters, the grouped parameters will be printed.

## Hidden parameters

Params that are set as `hidden` in the JSON Schema are not shown in the help message.
To show these parameters, pass the `--showHidden` parameter to the nextflow command.

## Coloured logs

By default, the help output is coloured using ANSI escape codes.

If you prefer, you can disable these by setting the `validation.monochromeLogs` configuration option to `true`

=== "Default (coloured)"

    ![Default help output](../images/help_not_monochrome_logs.png)

=== "Monochrome logs"

    ![Default help output](../images/help_monochrome_logs.png)

## `paramsHelp()`

This function returns a help message with the command to run a pipeline and the available parameters.
Pass it to `log.info` to print in the terminal.

The function takes one optional positional argument, which can be a parameter name to get the help message for. Additionally, it accepts the following options to customize the help message:

- `parametersSchema`: Path to the JSON schema to get the help message from. Defaults to the schema defined in the `validation.parametersSchema` configuration option (which is `nextflow_schema.json` by default).
- `beforeText`: Text to print before the help message. Defaults to the `validation.help.beforeText` configuration option.
- `afterText`: Text to print after the help message. Defaults to the `validation.help.afterText` configuration option.
- `command`: Command to print before the help message. Defaults to the `validation.help.command` configuration option.
- `showHidden`: Whether to show hidden parameters in the help message. Defaults to `false`.
- `fullHelp`: Whether to show the full help message (including all parameters, even those that are not top-level). Defaults to `false`.

!!! Note

    `paramsHelp()` doesn't stop pipeline execution after running.
    You must add this into your pipeline code if it's the desired functionality.

Typical usage:

=== "main.nf"

    ```groovy
    --8<-- "examples/paramsHelp/pipeline/main.nf"
    ```

=== "nextflow.config"

    ```groovy
    --8<-- "examples/paramsHelp/pipeline/nextflow.config"
    ```

=== "nextflow_schema.json"

    ```json
    --8<-- "examples/paramsHelp/pipeline/nextflow_schema.json"
    ```

Output with `--help`:

```
--8<-- "examples/paramsHelp/log.txt"
```

Output with `--help outdir`:

```
--8<-- "examples/paramsHelp/log_outdir.txt"
```

!!! warning

    We shouldn't be using `exit` as it kills the Nextflow head job in a way that is difficult to handle by systems that may be running it externally, but at the time of writing there is no good alternative.
    See [`nextflow-io/nextflow#3984`](https://github.com/nextflow-io/nextflow/issues/3984).
