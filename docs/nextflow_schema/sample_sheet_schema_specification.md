---
description: Schema specification for sample sheet validation
---

# Sample sheet schema specification

Sample sheet schema files are used by the nf-schema plugin for validation of sample sheet contents and type conversion / channel generation.

The Nextflow schema syntax is based on the JSON schema standard.
You can find more information about JSON Schema here:

- Official docs: <https://json-schema.org>
- Excellent "Understanding JSON Schema" docs: <https://json-schema.org/understanding-json-schema>

## Schema structure

Validation by the plugin works by parsing the supplied file contents into a groovy object,
then passing this to the JSON schema validation library.
As such, the structure of the schema must match the structure of the parsed file.

Typically, samplesheets are CSV files, with fields represented as columns and samples as rows. TSV, JSON and YAML samplesheets are also supported by this plugin

In this case, the parsed object will be an `array` (see [JSON schema docs](https://json-schema.org/understanding-json-schema/reference/array.html#items)).
The array type is associated with an `items` key which in our case contains a single `object`.
The object has `properties`, where the keys must match the headers of the CSV file.

So, for CSV samplesheets, the top-level schema should look something like this:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "field_1": { "type": "string" },
      "field_2": { "type": "string" }
    }
  }
}
```

If your sample sheet has a different format (for example, a nested YAML file),
you will need to build your schema to match the parsed structure.

## Properties

Every array object will contain keys for each field.
Each field should be described as an element in the object `properties` section.

The keys of each property must match the header text used in the sample sheet.

Fields that are present in the sample sheet, but not in the schema will be ignored and produce a warning.

!!! tip

    The order of columns in the _sample sheet_ is not relevant, as long as the header text matches.

!!! warning

    The order of properties in the _schema_ **is** important.
    This order defines the order of output channel properties when using the `samplesheetToList()` function.

## Common keys

The majority of schema keys for sample sheet schema validation are identical to the Nextflow schema.
For example: `type`, `pattern`, `format`, `errorMessage`, `exists` and so on.

Please refer to the [Nextflow schema specification](../nextflow_schema/nextflow_schema_specification.md) docs for details.

## Sample sheet keys

Below are the properties that are specific to sample sheet schema.
These exist in addition to those described in the [Nextflow schema specification](../nextflow_schema/nextflow_schema_specification.md).

### `meta`

Type: `List` or `String`

The current field will be considered a meta value when this parameter is present. This parameter should contain a list of the meta fields or a string stating a single meta field to assign this value to. The default is no meta for each field.

For example:

```json
{
  "meta": "id"
}
```

will convert the `field` value to a meta value, resulting in the channel `[[id:value]...]`
See [here](https://github.com/nextflow-io/nf-schema/blob/master/src/testResources/schema_input.json#L10-L25) for an example in the sample sheet.
