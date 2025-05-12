# nf-schema examples

This directory contains example files for the nf-schema project.

1. [Help Message](./helpMessage/) contains a pipeline that generates a help message for a given schema.
2. [Parameter summary log](./paramSummaryLog/) contains a pipeline that generates a summary log of the parameters used in a given schema.
3. [Parameter summary map](./paramSummaryMap/) contains a pipeline that generates a summary map of the parameters used in a given schema. The difference between with the previous example is that this one produces a machine readable data structure instead of a human readable output string.
4. [Basic samplesheet conversion](./samplesheetToListBasic/) contains a pipeline that converts a samplesheet to a list that can be easily converted to a channel for usage in the pipeline.