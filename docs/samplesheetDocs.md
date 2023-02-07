# Samplesheet JSON schema

Example samplesheets can be found in the [references](references/) folder.

## Properties
All fields should be present in the `properties` section. These should be in the order you want for the output channel e.g. for this [schema](references/samplesheet_schema.json) the output channel will look like `[[id:sample, sample:sample], cram, crai, bed, ped]`.

### Global parameters
These schema specifications can be used on any type of input:

| Parameter | Description |
|-----------|-------------|
| meta | The current field will be considered a meta value when this parameter is present. This parameter should contain a comma-delimited list of the meta fields to use this value for. The default is no meta for each field. |
| unique | Whether or not the field should contain a unique value over the entire samplesheet. An array can also be given to this parameter. This array should contain other field names that should be unique in combination with the current field. The default is `false`. |
| type | The type of the input value. The input will be automatically converted to this type. All types have unique schema specifications that can be used validate further on these inputs. See the next parts for information on these specifications. The default is `string`. |
| enum | An array containing at least one element, where the input has to match exactly one of these elements. The elements can be any type. |
| deprecated | A boolean variable stating that the field is deprecated and will be removed in the nearby future. This will throw a warning to the user that the current field is deprecated. The default value is `false` |
| dependentRequired | An array containing names of other fields. The validator will check if these fields are filled in and throw an error if they aren't. When the current fields isn't supplied, this check will be skipped. |

### String inputs
String inputs need to have `"type": "string"` specified in its field.

| Parameter | Description |
|-----------|-------------|
| pattern | The regex pattern to check on the current string. The default is `^.*$`. |
| format | The format of the input value. To see all possibilities go to [Formats](#formats). The default is a plain string value. |
| maxLength | A non-negative integer stating the inclusive maximum length a string input can be. |
| minLength | A non-negative integer stating the inclusive minimum length a string input can be. |

#### Formats

Formats can be used to check string values for certain properties. The input will also be transformed to the correct type.

Following table shows all currently implemented formats with a description.

| Format | Description |
|-----------|-------------|
| file-path | Automatically checks if the file exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input |
| directory-path | Automatically checks if the directory exists and transforms the `String` type to a `Nextflow.File` type, which is usable in Nextflow processes as a `path` input. This is currently synonymous for `file-path`. |

### Integer and number

Integer inputs need to have `"type": "integer"` or `"type": "number"` specified in its field.

| Parameter | Description |
|-----------|-------------|
| multipleOf | An integer of which the input needs to be a multiple. This element needs to be greater than 0. |
| maximum | An integer stating the inclusive maximum a number must be. |
| minimum | An integer stating the inclusive minimum a number must be. |
### Boolean

Boolean inputs need to have `"type": "boolean"` specified in its field.

| Parameter | Description |
|-----------|-------------|


## Required fields
All names of the required fields should be specified as an array under `required`.
