# Fail unrecognised parameters

This example demonstrates how the nf-schema plugin logging, when unrecognized parameters are given to a pipeline by a user, can be configured. In this case the validation will fail if there are any unrecognized parameters.
See the [logging configuration](https://nextflow-io.github.io/nf-schema/latest/configuration/configuration/#logging) documentation for more information.

There's one example log file provided in this directory:
- `log.txt`: This log file shows the output when running the pipeline with unrecognized parameters, which results in an error message.
