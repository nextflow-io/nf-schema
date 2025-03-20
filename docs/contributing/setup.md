---
title: Contribution instructions
description: How to contribute to nf-schema
---

# Getting started with plugin development

## Tests

To run the tests use the following command:

```bash
make test
```

## Install and use in pipelines

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

## Release and publish the plugin

1. In `build.gradle` make sure that:
   - `version` matches the desired release version,
   - `github.repository` matches the repository of the plugin,
   - `github.indexUrl` points to your fork of the plugins index repository.
2. Create a file named `$HOME/.gradle/gradle.properties`, where `$HOME` is your home directory. Add the following properties:
   - `github_username`: The GitHub username granting access to the plugin repository.
   - `github_access_token`: The GitHub access token required to upload and commit changes to the plugin repository.
   - `github_commit_email`: The email address associated with your GitHub account.
3. Update the [changelog](./CHANGELOG.md).
4. Build and publish the plugin to your GitHub repository:
   ```bash
   make release
   ```
5. Create a pull request against the [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) repository to make the plugin publicly accessible.
