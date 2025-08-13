# nextflow-io/nf-schema: Changelog

# Version 2.5.0

## New features

1. Added a new configuration option: `validation.help.enumLength` which sets the maximum length of enum values in the help message. The default is set to the value in the `COLUMNS` environment variable or 100 character if that variable isn't set.
2. Added top-level schema description for detailed help `--help <args>`. If `<args>` is associated with a schema, then the top-level fields will be shown similarly to `--help` for the main schema (#146).

## Changes

1. The plugin now properly validates cloud storage files instead of skipping them. Exotic errors will be added to the error messages instead of failing the validation outright.
2. Migrated to the new observer class in Nextflow 25.04.0
3. Rework the deprecated `paramsHelp()` function to allow pipeline developers a fully fledged alternative to the help messages created via the configuration options. This change enables the use of dynamic help message with the strict configuration syntax introduced in Nextflow 25.04.0.
4. The ANSI log setting of Nextflow is now used to determine whether or not the log should be monochrome. This setting will take priority over the `validation.monochromeLogs` configuration option.

## Bug fixes

1. CSV and TSV files with trailing commas or tabs will now be properly sanitized. This fixes issues with CSV and TSV files that contained empty header columns.
2. Unidentified parameters are no longer printed out on failure of the parameter validation. This is to prevent a bug where all parameters would be printed out on failure.
3. Fixed an issue where default values of `null` weren't being set correctly in `samplesheetToList()`.

## Logging configuration

This update contains a rework of the logging configuration. The `valdiation.logging` configuration scope has been added with options to replace and expand the current logging configuration options. These options can all take the following values:

- `skip`: Skip logging
- `debug`: Log debug messages (only printed in the `.nextflow.log` file)
- `info`: Log info messages (also printed in the terminal)
- `warn`: Log warning messages (also printed in the terminal, but in yellow)
- `error`: Fail the pipeline and print the error message

| Old option                           | New option                               | Description                                                 |
| ------------------------------------ | ---------------------------------------- | ----------------------------------------------------------- |
| `validation.failUnrecognisedParams`  | `validation.logging.unrecognisedParams`  | The logging level for unrecognised parameters.              |
| `validation.failUnrecognisedHeaders` | `validation.logging.unrecognisedHeaders` | The logging level for unrecognised headers in samplesheets. |

# Version 2.4.2

## Bug fixes

1. `validateParameters` should now correctly ignore nested parameters given in the `validation.ignoreParams` and the `validation.defaultIgnoreParams` configuration options.

# Version 2.4.1

## Bug fixes

1. Allow `GString` values for all configuration options that allow `String` values.

# Version 2.4.0

## New features

1. Added a new configuration option: `validation.maxErrValSize` which sets the maximum length that a value in an error message can be. The default is set to 150 characters.
2. Added a new function: `validate()` that can be used to validate any data structure using a JSON schema.

## Bug fixes

1. Move the unpinned version check to an observer. This makes sure the warning is always shown and not only when importing a function.
2. Added a missing inherited method to the observer to fix issues with workflow output publishing
3. Fixed unexpected failures with samplesheet schemas using `anyOf`, `allOf` and `oneOf`
4. Fixed an error with help messages when the `type` keyword was missing
5. Fix compilation errors in Java 21

## Improvements

1. Slow uniqueness check (> 2hrs for 100k samples) made 400x faster by switching from `findAll` to a `subMap` for isolating the required unique fields.
2. `patternProperties` now has greater support, with no warnings about invalid parameters which actually match a pattern
3. Added better error handling and debug messages to the configuration parser.

## Changes

1. Refactored the whole codebase to make future development easier
2. Bumped the minimal Nextflow version to `24.10.0`

# Version 2.3.0 - Hakodate

## Bug fixes

1. The help message will now also be printed out when no functions of the plugin get included in the pipeline.
2. JSON and YAML files that are not a list of values should now also be validated correctly. (Mind that samplesheets always have to be a list of values to work with `samplesheetToList`)

# Version 2.2.1

## Bug fixes

1. Fixed a bug in `paramsSummaryMap()` related to the processing of workflow config files.
2. Fixed a bug where `validation.defaultIgnoreParams` and `validation.ignoreParams` would not actually ignore the parameter validation.

# Version 2.2.0 - Kitakata

## New features

1. Added a new configuration option `validation.failUnrecognisedHeaders`. This is the analogue to `failUnrecognisedParams`, but for samplesheet headers. The default is `false` which means that unrecognized headers throw a warning instead of an error.
2. Added a new configuration option `validation.summary.hideParams`. This option takes a list of parameter names to hide from the parameters summary created by `paramsSummaryMap()` and `paramsSummaryLog()`

## Bug fixes

1. Fixed a bug in `samplesheetToList` that caused output mixing when the function was used more than once in channel operators.
2. Added a missing depencency for email format validation.
3. All path formats (with exception to `file-path-pattern`) will now give a proper error message when a `file-path-pattern` has been used.

## Improvements

1. Improved the `exists` keyword documentation with a warning about an edge case.
2. Updated the error messages. Custom error messages provided in the JSON schema will now be appended to the original error messages instead of overwriting them.

# Version 2.1.2

## Bug fixes

1. The directory `nf_test_output` is now an ignored parameter during validation to support use of both `nf_test` and `nf_schema`.
2. `uniqueEntries` will now skip unique checks when all values in the requested array properties are empty. This had to be implemented to allow optional values to work with the `uniqueEntries` check. Partially filled in array properties will still fail (and that's how it's meant to be). Be sure to use `oneOf` to properly configure all possible combinations in case this causes some issues.
3. Improved the error messages produced by `uniqueEntries`.

## Documentation

1. Fix some faults in the docs

# Version 2.1.1

## Bug fixes

1. The help parameters are now no longer unexpected parameters when validating parameters.
2. Fixed a typo in the docs
3. Added a URL to the help message migration docs to the `paramsHelp()` deprecation message
4. The old `validation.showHiddenParams` config option works again to ensure backwards compatibility. Using `validation.help.showHidden` is still preffered and the old option will emit a deprecation message.
5. Resolved an issue where the UniqueEntriesEvaluator did not correctly detect non-unique combinations.

# Version 2.1.0 - Tantanmen

## Breaking changes

1. The minimum supported Nextflow version is now `23.10.0` instead of `22.10.0`

## New features

1. The plugin now fully supports nested parameters!
2. Added a config option `validation.parametersSchema` which can be used to set the parameters JSON schema in a config file. The default is `nextflow_schema.json`
3. The parameter summary log will now automatically show nested parameters.
4. Added two new configuration options: `validation.summary.beforeText` and `validation.summary.afterText` to automatically add some text before and after the output of the `paramsSummaryLog()` function. The colors from these texts will be automatically filtered out if `validation.monochromeLogs` is set to `true`.

## Help message changes

1. The use of the `paramsHelp()` function has now been deprecated in favor of a new built-in help message functionality. `paramsHelp()` has been updated to use the reworked help message creator. If you still want to use `paramsHelp()` for some reason in your pipeline, please add the `hideWarning:true` option to it to make sure the deprecation warning will not be shown.
2. Added new configuration values to support the new help message functionality:
   - `validation.help.enabled`: Enables the checker for the help message parameters. The plugin will automatically show the help message when one of these parameters have been given and exit the pipeline. Default = `false`
   - `validation.help.shortParameter`: The parameter to use for the compact help message. This help message will only contain top level parameters. Default = `help`
   - `validation.help.fullParameter`: The parameter to use for the expanded help message. This help message will show all parameters no matter how deeply nested they are. Default = `helpFull`
   - `validation.help.showHiddenParameter`: The parameter to use to also show all parameters with the `hidden: true` keyword in the schema. Default = `showHidden`
   - `validation.help.showHidden`: Set this to `true` to show hidden parameters by default. This configuration option is overwritten by the value supplied to the parameter in `validation.help.showHiddenParameter`. Default = `false`
   - `validation.help.beforeText`: Some custom text to add before the help message. The colors from this text will be automatically filtered out if `validation.monochromeLogs` is set to `true`.
   - `validation.help.afterText`: Some custom text to add after the help message. The colors from this text will be automatically filtered out if `validation.monochromeLogs` is set to `true`.
   - `validation.help.command`: An example command to add to the top of the help message. The colors from this text will be automatically filtered out if `validation.monochromeLogs` is set to `true`.
3. Added support for nested parameters to the help message. A detailed help message using `--help <parameter>` will now also contain all nested parameters. The parameter supplied to `--help` can be a nested parameter too (e.g. `--help top_parameter.nested_parameter.deeper_parameter`)
4. The help message now won't show empty parameter groups.
5. The help message will now automatically contain the three parameters used to get help messages.

## JSON schema fixes

1. The `defs` keyword is now deprecated in favor of the `$defs` keyword. This to follow the JSON schema guidelines. We will continue supporting `defs` for backwards compatibility.

# Version 2.0.1 - Tsukemen

## Vulnerability fix

1. Updated the org.json package to version `20240303`.

# Version 2.0.0 - Kagoshima

To migrate from nf-validation please follow the [migration guide](https://nextflow-io.github.io/nf-schema/latest/migration_guide/)

## New features

- Added the `uniqueEntries` keyword. This keyword takes a list of strings corresponding to names of fields that need to be a unique combination. e.g. `uniqueEntries: ['sample', 'replicate']` will make sure that the combination of the `sample` and `replicate` fields is unique. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Added `samplesheetToList` which is the function equivalent of `.fromSamplesheet` [#3](https://github.com/nextflow-io/nf-schema/pull/3)
- Added a warning if the `nf-schema` version is unpinned. Let's hope this prevents future disasters like the release of `nf-validation` v2.0 :grin:

## Changes

- Changed the used draft for the schema from `draft-07` to `draft-2020-12`. See the [2019-09](https://json-schema.org/draft/2019-09/release-notes) and [2020-12](https://json-schema.org/draft/2020-12/release-notes) release notes for all changes ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed the `fromSamplesheet` channel operator and added a `samplesheetToList` function instead. This function validates the samplesheet and returns a list of it. [#3](https://github.com/nextflow-io/nf-schema/pull/3)
- Removed the `unique` keyword from the samplesheet schema. You should now use [`uniqueItems`](https://json-schema.org/understanding-json-schema/reference/array#uniqueItems) or `uniqueEntries` instead ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed the `skip_duplicate_check` option from the `samplesheetToList()` function and the `--validationSkipDuplicateCheck` parameter. You should now use the `uniqueEntries` or [`uniqueItems`](https://json-schema.org/understanding-json-schema/reference/array#uniqueItems) keywords in the schema instead ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `samplesheetToList()` now does dynamic typecasting instead of using the `type` fields in the JSON schema. This is done due to the complexity of `draft-2020-12` JSON schemas. This should not have that much impact but keep in mind that some types can be different between this version and older versions in nf-validation ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `samplesheetToList()` will now set all missing values as `[]` instead of the type specific defaults (because of the changes in the previous point). This should not change that much as this will also result in `false` when used in conditions. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Removed the configuration parameters and added configuration options instead. For a full list of these new options, please have a look at the [configuration docs](https://nextflow-io.github.io/nf-schema/latest/configuration/)
- Ignore validation of Azure and GCP hosted blob storage files in addition to AWS S3 hosted files. This is because they are not true POSIX compliant files and would incorrectly fail validation ([#29](https://github.com/nextflow-io/nf-schema/pull/29))

## Improvements

- Setting the `exists` keyword to `false` will now check if the path does not exist ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- The `schema` keyword will now work in all schemas. ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- Improved the error messages ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
- `.fromSamplesheet()` now supports deeply nested samplesheets ([#141](https://github.com/nextflow-io/nf-validation/pull/141))
