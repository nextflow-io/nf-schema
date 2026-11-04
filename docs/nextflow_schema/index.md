---
title: Introduction
description: Introduction to the nf-schema plugin
---

# Nextflow schema for parameters

The functionality of the nf-schema plugin centres on a pipeline _schema_ file.
By convention, this file is stored in the workflow root directory and called `nextflow_schema.json`.

## What it does

The schema file provides a place to describe the pipeline configuration.
It is based on the [JSON Schema format](https://json-schema.org/) standard.

In brief, it includes information for each parameter about:

- Name (the `params.name`)
- Description and help texts
- Variable type (string, integer, boolean, etc)
- Validation rules (string regexes, numeric min / max, enum, etc)

..and more. See the [full specification](nextflow_schema_specification.md) for details.

!!! warning

    Although it's _based_ on JSON Schema - there are some differences.
    We use a few non-standard keys and impose one or two limitations
    that are not present in the standard specification.

!!! tip

    It's **highly recommended** that you don't try to write the schema
    JSON file manually. Instead, use the provided tooling - see
    [Creating schema](create_schema.md) for details.

## How it's used

The `nextflow_schema.json` file and format have been in use for a few years now
and are widely used in the community. Some specific examples of usage are:

- Validation & help texts within pipelines _(this plugin)_
- Generation of documentation pages _(nf-core pipeline pages)_
- Generation of pipeline launch user interfaces _(nf-core launch, Nextflow Tower and more)_

## Looking to the future

The pipeline schema has been developed to provide additional functionality not present in core Nextflow.
It's our hope that at some point this functionality will be added to core Nextflow, making schema files redundant.

See the GitHub issue [_Evolution of Nextflow configuration file_ (`nextflow-io/nextflow#2723`)](https://github.com/nextflow-io/nextflow/issues/2723) on the Nextflow repo for discussion about potential new configuration file formats, which could potentially include the kind of information that we have within schema.
