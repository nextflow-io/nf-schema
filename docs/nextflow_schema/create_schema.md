---
title: Creating a Nextflow schema
description: How to create a Nextflow schema file for your pipeline
---

# Creating schema files

!!! warning

    It's **highly recommended** that you don't try to write the schema JSON file manually!

    The schema files can get large and complex and are difficult to debug.
    Don't be tempted to open in your code editor - instead use the provided tools!

## Requirements

To work with Nextflow schema files, you need the `nf-core` command-line tools package.
You can find full installation instructions in the [nf-core documentation](https://nf-co.re/tools/#installation),
but in brief, you install as with any other Python package:

```bash
pip install nf-core
# -- OR -- #
conda install nf-core # (1)!
```

1.  :warning: **Note**: Needs bioconda channels to be configured!
    See the [Bioconda usage docs](https://bioconda.github.io/index.html#usage).

!!! info

    Although these tools are currently within the nf-core tooling ecosystem, they should
    work with _any_ Nextflow pipeline: you don't have to be using the nf-core template for this.

!!! note

    We aim to extract this functionality into stand-alone tools at a future date,
    as we have done with the pipeline validation code in this plugin.

## Build a pipeline schema

Once you have nf-core/tools installed and have written your pipeline configuration,
go to the pipeline root and run the following:

```bash
nf-core schema build
```

!!! warning

    The current version of `nf-core` tools (v2.13.1) does not support the new schema draft used in `nf-schema`. Running this command after building the schema will convert the schema to the right draft:

    ```bash
    sed -i -e 's/http:\/\/json-schema.org\/draft-07\/schema/https:\/\/json-schema.org\/draft\/2020-12\/schema/g' -e 's/definitions/$defs/g' nextflow_schema.json
    ```
    A new version of the nf-core schema builder will be available soon. Keep an eye out!

The tool will run the `nextflow config` command to extract your pipeline's configuration
and compare the output to your `nextflow_schema.json` file (if it exists).
It will prompt you to update the schema file with any changes, then it will ask if you
wish to edit the schema using the web interface.

This web interface is where you should add detail to your schema, customising the various fields for each parameter.

!!! tip

    You can run the `nf-core schema build` command again and again, as many times as you like.
    It's designed both for initial creation but also future updates of the schema file.

    It's a good idea to "save little and often" by clicking `Finished` and saving your work locally,
    then running the command again to continue.

## Build a sample sheet schema

!!! danger

    There is currently no tooling to help you write sample sheet schema :anguished:

    You can find an example in [Example sample sheet schema](sample_sheet_schema_examples.md)

    Watch this space..
