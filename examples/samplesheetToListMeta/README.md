# Samplesheet conversion with meta

This example shows how to convert a samplesheet to a list that can be easily converted to a channel for usage in the pipeline. This example contains a meta map. This conversion uses the JSON schema available in [pipeline/assets/schema_input.json](pipeline/assets/schema_input.json) to determine the order of the fields and which keys should be in the meta map.

There's one example log file provided in this directory:
- `log.txt`: This log file shows the output when running the pipeline, which prints out the samplesheet converted to a channel from a list. This contains a meta map.