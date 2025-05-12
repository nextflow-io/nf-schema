---
description: Example JSON Schema for a Nextflow pipeline `nextflow_schema.json` file
---

# Example sample sheet schema

## nf-core/rnaseq example

The nf-core/rnaseq pipeline was one of the first to have a sample sheet schema.
You can see this, used for validating sample sheets with `--input` here: [`assets/schema_input.json`](https://github.com/nf-core/rnaseq/blob/5671b65af97fe78a2f9b4d05d850304918b1b86e/assets/schema_input.json).

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://raw.githubusercontent.com/nf-core/rnaseq/master/assets/schema_input.json",
  "title": "nf-core/rnaseq pipeline - params.input schema",
  "description": "Schema for the file provided with params.input",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "sample": {
        "type": "string",
        "pattern": "^\\S+$",
        "errorMessage": "Sample name must be provided and cannot contain spaces",
        "meta": ["my_sample"]
      },
      "fastq_1": {
        "type": "string",
        "pattern": "^\\S+\\.f(ast)?q\\.gz$",
        "format": "file-path",
        "errorMessage": "FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'"
      },
      "fastq_2": {
        "errorMessage": "FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz'",
        "type": "string",
        "pattern": "^\\S+\\.f(ast)?q\\.gz$",
        "format": "file-path"
      },
      "strandedness": {
        "type": "string",
        "errorMessage": "Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded'",
        "enum": ["forward", "reverse", "unstranded"],
        "meta": ["my_strandedness"]
      }
    },
    "required": ["sample", "fastq_1", "strandedness"]
  }
}
```

## nf-schema test case

You can see a very feature-complete example JSON Schema for a sample sheet schema file below.

It is used as a test fixture in the nf-schema package [here](https://github.com/nextflow-io/nf-schema/blob/master/plugins/nf-schema/src/testResources/schema_input.json).

!!! note

    More examples can be found in the plugin [`testResources` directory](https://github.com/nextflow-io/nf-schema/blob/master/plugins/nf-schema/src/testResources/).

```json
--8<-- "plugins/nf-schema/src/testResources/schema_input.json"
```

Even more examples can be found in the plugin [`examples` directory](../../examples) in the GitHub repository.
