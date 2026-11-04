---
title: Validate common data structures
description: Function to validate common data structures
---

# Validate common data structures

The `validate` function can be used to validate common data structures.

```groovy
include { validate } from 'plugin/nf-schema'

validate(<input>, <schema>)
```

This function takes two positional arguments:

1. The data that needs to be validated
2. The path to the schema used to validate the input data

The tested data structures are listed below, however more data structures might by supported too:

- Maps
- Lists
- String
- Integers
- Booleans

`validate` also has the following optional arguments:

- `exitOnError`: Exit the pipeline on validation failure and show the error message. The function will output the errors when this option is set to `false` (default: `true`)
  - `validate(<input>, <schema>, exitOnError:true|false)`
