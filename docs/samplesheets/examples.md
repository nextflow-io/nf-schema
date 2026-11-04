---
title: Channel usage examples
description: Examples of advanced sample sheet creation techniques.
---

# Sample sheet channel manipulation examples

## Introduction

Understanding channel structure and manipulation is critical for getting the most out of Nextflow. nf-schema helps initialise your channels from the text inputs to get you started, but further work might be required to fit your exact use case. In this page we run through some common cases for transforming the output of `samplesheetToList()`.

### Glossary

- A channel is the Nextflow object, referenced in the code
- An item is each thing passing through the channel, equivalent to one row in the sample sheet
- An element is each thing in the item, e.g., the meta value, fastq_1 etc. It may be a file or value

## Default mode

Each item in the list emitted by `samplesheetToList()` is a tuple, corresponding with each row of the sample sheet. Each item will be composed of a meta value (if present) and any additional elements from columns in the sample sheet, e.g.:

```csv
sample,fastq_1,fastq_2,bed
sample1,fastq1.R1.fq.gz,fastq1.R2.fq.gz,sample1.bed
sample2,fastq2.R1.fq.gz,fastq2.R2.fq.gz,
```

Might create a list where each element consists of 4 items, a map value followed by three files:

```groovy
// Columns:
[ val([ sample: sample ]), file(fastq1), file(fastq2), file(bed) ]

// Resulting in:
[ [ id: "sample" ], fastq1.R1.fq.gz, fastq1.R2.fq.gz, sample1.bed]
[ [ id: "sample2" ], fastq2.R1.fq.gz, fastq2.R2.fq.gz, [] ] // A missing value from the sample sheet is an empty list
```

This list can be converted to a channel that can be used as input of a process where the input declaration is:

```nextflow
tuple val(meta), path(fastq_1), path(fastq_2), path(bed)
```

It may be necessary to manipulate this channel to fit your process inputs. For more documentation, check out the [Nextflow operator docs](https://www.nextflow.io/docs/latest/operator.html), however here are some common use cases with `samplesheetToList()`.

## Using a sample sheet with no headers

Sometimes you only have one possible input in the pipeline sample sheet. In this case it doesn't make sense to have a header in the sample sheet. This can be done by removing the `properties` section from the sample sheet and changing the type of the element from `object` the desired type:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "description": "Schema for the file provided with params.input",
  "type": "array",
  "items": {
    "type": "string"
  }
}
```

When using samplesheets like this CSV file:

```csv
test_1
test_2
```

or this YAML file:

```yaml
- test_1
- test_2
```

The output of `samplesheetToList()` will look like this:

```bash
test_1
test_2
```

## Changing the structure of channel items

Each item in the list will be a tuple, but some processes will use multiple files as a list in their input channel, this is common in nf-core modules. For example, consider the following input declaration in a process, where FASTQ could be > 1 file:

```groovy
process ZCAT_FASTQS {
    input:
        tuple val(meta), path(fastq)

    """
    zcat $fastq
    """
}
```

The output of `samplesheetToList()` (converted to a channel) can be used by default with a process with the following input declaration:

```groovy
val(meta), path(fastq_1), path(fastq_2)
```

To manipulate each item within a channel, you should use the [Nextflow `.map()` operator](https://www.nextflow.io/docs/latest/operator.html#map). This will apply a function to each element of the channel in turn. Here, we convert the flat tuple into a tuple composed of a meta and a list of FASTQ files:

```groovy
Channel.fromList(samplesheetToList(params.input, "path/to/json/schema"))
    .map { meta, fastq_1, fastq_2 -> tuple(meta, [ fastq_1, fastq_2 ]) }
    .set { input }

input.view() // Channel has 2 elements: meta, fastqs
```

This is now compatible with the process defined above and will not raise a warning about input cardinality:

```groovy
ZCAT_FASTQS(input)
```

## Removing elements in channel items

For example, to remove the BED file from the channel created above, we could not return it from the map. Note the absence of the `bed` item in the return of the closure below:

```groovy
Channel.fromList(samplesheetToList(params.input, "path/to/json/schema"))
    .map { meta, fastq_1, fastq_2, bed -> tuple(meta, fastq_1, fastq_2) }
    .set { input }

input.view() // Channel has 3 elements: meta, fastq_1, fastq_2
```

In this way you can drop items from a channel.

## Separating channel items

We could perform this twice to create one channel containing the FASTQs and one containing the BED files, however Nextflow has a native operator to separate channels called [`.multiMap()`](https://www.nextflow.io/docs/latest/operator.html#multimap). Here, we separate the FASTQs and BEDs into two separate channels using `multiMap`. Note, the channels are both contained in `input` and accessed as an attribute using dot notation:

```groovy
Channel.fromList(samplesheetToList(params.input, "path/to/json/schema"))
    .multiMap { meta, fastq_1, fastq_2, bed ->
        fastq: tuple(meta, fastq_1, fastq_2)
        bed:   tuple(meta, bed)
    }
    .set { input }
```

The channel has two attributes, `fastq` and `bed`, which can be accessed separately.

```groovy
input.fastq.view() // Channel has 3 elements: meta, fastq_1, fastq_2
input.bed.view()   // Channel has 2 elements: meta, bed
```

Importantly, `multiMap` applies to every item in the channel and returns an item to both channels for every input, i.e. `input`, `input.fastq` and `input.bed` all contain the same number of items, however each item will be different.

## Separate items based on a condition

You can use the [`.branch()` operator](https://www.nextflow.io/docs/latest/operator.html#branch) to separate the channel entries based on a condition. This is especially useful when you can get multiple types of input data.

This example shows a channel which can have entries for WES or WGS data. WES data includes a BED file denoting the target regions, but WGS data does not. These analysis are different so we want to separate the WES and WGS entries from each other. We can separate the two using `.branch` based on the presence of the BED file:

```groovy
// Channel with four elements - see docs for examples
params.input = "samplesheet.csv"

Channel.fromList(samplesheetToList(params.input, "path/to/json/schema"))
    .branch { meta, fastq_1, fastq_2, bed ->
        // If BED does not exist
        WGS: !bed
            return [meta, fastq_1, fastq_2]
        // If BED exists
        WES: bed
            // The original channel structure will be used when no return statement is used.
    }
    .set { input }

input.WGS.view() // Channel has 3 elements: meta, fastq_1, fastq_2
input.WES.view() // Channel has 4 elements: meta, fastq_1, fastq_2, bed
```

Unlike `.multiMap()`, the outputs of `.branch()` will contain a different number of items.

## Combining a channel

After splitting the channel, it may be necessary to rejoin the channel. There are many ways to join a channel, but here we will demonstrate the simplest which uses the [Nextflow join operator](https://www.nextflow.io/docs/latest/operator.html#join) to rejoin any of the channels from above based on the first element in each item, the `meta` value.

```groovy
input.fastq.view() // Channel has 3 elements: meta, fastq_1, fastq_2
input.bed.view()   // Channel has 2 elements: meta, bed

input.fastq
    .join( input.bed )
    .set { input_joined }

input_joined.view() // Channel has 4 elements: meta, fastq_1, fastq_2, bed
```

## Count items with a common value

This example is based on this [code](https://github.com/mribeirodantas/NextflowSnippets/blob/main/snippets/countBy.md) from [Marcel Ribeiro-Dantas](https://github.com/mribeirodantas).

It's useful to determine the count of channel entries with similar values when you want to merge them later on (to prevent pipeline bottlenecks with `.groupTuple()`).

This example contains a channel where multiple samples can be in the same family. Later on in the pipeline we want to merge the analyzed files so one file gets created for each family. The result will be a channel with an extra meta field containing the count of channel entries with the same family name.

```groovy
// channel created with samplesheetToList() previous to modification:
// [[id:example1, family:family1], example1.txt]
// [[id:example2, family:family1], example2.txt]
// [[id:example3, family:family2], example3.txt]

params.input = "sample sheet.csv"

Channel.fromList(samplesheetToList(params.input, "path/to/json/schema"))
    .tap { ch_raw }                       // Create a copy of the original channel
    .map { meta, txt -> [ meta.family ] } // Isolate the value to count on
    .reduce([:]) { counts, family ->      // Creates a map like this: [family1:2, family2:1]
        counts[family] = (counts[family] ?: 0) + 1
        counts
    }
    .combine(ch_raw)                     // Add the count map to the original channel
    .map { counts, meta, txt ->          // Add the counts of the current family to the meta
        new_meta = meta + [count:counts[meta.family]]
        [ new_meta, txt ]
    }
    .set { input }

input.view()
// [[id:example1, family:family1, count:2], example1.txt]
// [[id:example2, family:family1, count:2], example2.txt]
// [[id:example3, family:family2, count:1], example3.txt]
```
