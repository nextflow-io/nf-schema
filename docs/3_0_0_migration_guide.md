---
title: Migration guide for 2.x to 3.x
description: Guide to migrate pipelines from nf-schema versions 2.x to 3.x
hide:
  - toc
---

# Version 3.0.0 migration guide

## Changes to automatic help messages

Version 3.0.0 removed support for help messages to be automatically generated when `validation.help.enabled` has been set to `true`. The main reason for this was that the help message would only get generated at the start of the workflow, which was really flaky and prone to throw errors before the help message ever got printed.

The only way to print help messages now is to use the [`paramsHelp()`](parameters/help_text.md) function. Ideally this function gets executed as early as possible in the workflow code.

The `validation.help.enabled` configuration option has been removed from nf-schema and does no longer serve any purpose. This can be safely removed from all configs.

## Changes to `paramsHelp()`

The `paramsHelp()` function no longer takes an optional positional argument. Please use the new `parameter` option of the function to provide a specific parameter to print the help message of.

=== "2.x"

    ```groovy
    log.info paramsHelp('input')
    ```

=== "3.x"

    ```groovy
    log.info paramsHelp(parameter: 'input')
    ```

## Changes to `paramsSummaryMap` and `paramsSummaryLog`

The `paramsSummaryMap` and `paramsSummaryLog` function no longer take the workflow object as their input. This object is now automatically fetched from the running session.

=== "2.x"

    ```groovy
    paramsSummaryMap(workflow)
    paramsSummaryLog(workflow)
    ```

=== "3.x"

    ```groovy
    paramsSummaryMap()
    paramsSummaryLog()
    ```

## Removal of `validation.failUnrecognisedParams` and `validation.failUnrecognisedHeaders`

These options were already deprecated since version 2.5.0 and have now been fully removed. The logic behind it will thus no longer work. You can migrate to the new [`validation.logging`](./configuration/configuration.md#logging) configuration options.

=== "2.x"

    ```groovy
    validation {
        failUnrecognisedHeaders = true
        failUnrecognisedParams = true
    }
    ```

=== "3.x"

    ```groovy
    validation {
        logging {
            unrecognisedHeaders = 'error'
            unrecognisedParams = 'error'
        }
    }
    ```