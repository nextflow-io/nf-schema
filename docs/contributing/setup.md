---
title: Contribution instructions
description: How to contribute to nf-schema
---

# Getting started with plugin development

## Compiling

To compile and run the tests use the following command:

```bash
./gradlew check
```

## Launch it with installed Nextflow

!!! warning

    This method will add the development version of the plugin to your Nextflow plugins
    Take care when using this method and make sure that you are never using a
    development version to run real pipelines.
    You can delete all `nf-schema` versions using this command:
    ```bash
    rm -rf ~/.nextflow/plugins/nf-schema*
    ```

- Install the current version of the plugin in your `.nextflow/plugins` folder

```bash
make install
```

- Update or add the nf-schema plugin with the installed version in your test pipeline

```groovy title="nextflow.config"
plugins {
    id 'nf-schema@x.y.z'
}
```

## Launch it with a local version of Nextflow

- Clone the Nextflow repo into a sibling directory

```bash
cd .. && git clone https://github.com/nextflow-io/nextflow
cd nextflow && ./gradlew exportClasspath
```

- Append to the `settings.gradle` in this project the following line:

```bash
includeBuild('../nextflow')
```

- Compile the plugin code

```bash
./gradlew compileGroovy
```

- Run nextflow with this command:

```bash
./launch.sh run -plugins nf-schema <script/pipeline name> [pipeline params]
```

## Change and preview the docs

The docs are generated using [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).
You can install the required packages as follows:

```bash
pip install mkdocs-material pymdown-extensions pillow cairosvg
```

To change the docs, edit the files in the [docs/](https://github.com/nextflow-io/nf-schema/tree/master/docs) folder and run the following command to generate the docs:

```bash
mkdocs serve
```

To preview the docs, open the URL provided by mkdocs in your browser.
