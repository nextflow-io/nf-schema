# Troubleshooting

This section of the documentation contains a couple of common errors encountered while implementing nf-schema or using pipelines with the plugin and how to solve them.

## An integer value is failing because a string value was expected

Pipelines often use some kind of identifier field in their sample sheet, that usually gets `string` as the expected input. This can cause some issues when using CSV and TSV sample sheets since the types of all values are automatically inferred for these file types. Meaning that a value in the `sample` field that has `123` as the value would result in an integer instead of a string. Following error would be shown in this case:

```
-> Entry 1: Error for field 'sample' (123): Value is [integer] but should be [string]
```

There are multiple ways to solve this issue as pipeline developers and as pipeline users. The solutions will be listed from best options to worst:

### Pipeline user

- Option 1: Use a YAML or JSON file as sample sheet if that is allowed by the pipeline and make sure to quote the value. (`'123'` in the case of the example above)
- Option 2: Activate [lenient validation mode](../configuration/configuration.md#lenientmode). Keep in mind that this will enable lenient validation for **ALL** validations in the pipeline.

### Pipeline developer

- Option 1: Modify `"type": "string"` in the affected JSON schema to `"type": ["string", "integer"]`. Make sure that integer values don't break anything in your pipeline. The easiest way to ensure compatibility is to cast the value to a string after converting the sample sheet to a list.
- Option 2: Enforce usage of JSON and YAML sample sheets by only allowing files ending in `.yml`, `.yaml` or `.json` to be used.
