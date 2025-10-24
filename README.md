# ![nf-schema](docs/images/nf-schema.png)

## 📚 Docs 👉🏻 <https://nextflow-io.github.io/nf-schema>

**A Nextflow plugin to work with validation of pipeline parameters and sample sheets.**

## Introduction

> [!IMPORTANT]
> nf-schema is the new version of the now deprecated [nf-validation](https://github.com/nextflow-io/nf-validation). Please follow the [migration guide](https://nextflow-io.github.io/nf-schema/latest/migration_guide/) to migrate your code to this new version.

This [Nextflow plugin](https://www.nextflow.io/docs/latest/plugins.html#plugins) provides functionality that can be used in a Nextflow pipeline to work with parameter and sample sheet schema. The added functionality is:

- 📖 Print usage instructions to the terminal (for use with `--help`)
- ✍️ Print log output showing parameters with non-default values
- ✅ Validate supplied parameters against the pipeline schema
- 📋 Validate the contents of supplied sample sheet files
- 🛠️ Create a type-casted list from a parsed sample sheet

Supported sample sheet formats are CSV, TSV, JSON and YAML.

## Quick Start

Declare the plugin in your Nextflow pipeline configuration file:

```groovy title="nextflow.config"
plugins {
  id 'nf-schema@2.6.0'
}
```

This is all that is needed - Nextflow will automatically fetch the plugin code at run time.

> [!NOTE]
> The snippet above will always try to install the specified version. We encourage always pinning the
> plugin version to make sure the used pipeline will keep working when a new version of `nf-schema`
> with breaking changes has been released.

You can now include the plugin helper functions into your Nextflow pipeline:

```groovy title="main.nf"
include { validateParameters; paramsSummaryLog; samplesheetToList } from 'plugin/nf-schema'

// Validate input parameters
validateParameters()

// Print summary of supplied parameters
log.info paramsSummaryLog(workflow)

// Create a new channel of metadata from a sample sheet passed to the pipeline through the --input parameter
ch_input = Channel.fromList(samplesheetToList(params.input, "assets/schema_input.json"))
```

Or enable the creation of the help message (using `--help`) in the configuration file:

```groovy title="nextflow.config"
validation {
  help {
    enabled = true
  }
}
```

## Dependencies

- Java 11 or later
- <https://github.com/harrel56/json-schema>
- Nextflow 24.10.0 or later

## Slack channel

There is a dedicated [nf-schema Slack channel](https://nfcore.slack.com/archives/C056RQB10LU) in the [Nextflow Slack workspace](https://nextflow.slack.com).

## Credits

This plugin was written based on code initially written within the nf-core community,
as part of the nf-core pipeline template.

We would like to thank the key contributors who include (but are not limited to):

- Júlia Mir Pedrol ([@mirpedrol](https://github.com/mirpedrol))
- Nicolas Vannieuwkerke ([@nvnieuwk](https://github.com/nvnieuwk))
- Kevin Menden ([@KevinMenden](https://github.com/KevinMenden))
- Phil Ewels ([@ewels](https://github.com/ewels))
- Arthur ([@awgymer](https://github.com/awgymer))
