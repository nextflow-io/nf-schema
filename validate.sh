#!/bin/bash

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "\033[31mError: Docker is not installed\033[0m"
    exit 1
fi

# Pull the validator image first
echo -e "\033[36mPulling JSON Schema validator...\033[0m"
if ! docker pull python:3-slim; then
    echo -e "\033[31mError: Failed to pull validator image\033[0m"
    exit 1
fi

echo -e "\033[36mRunning validation for all test schemas...\033[0m"
echo -e "\033[36m----------------------------------------\033[0m"

# Run validation for all test schemas
echo "Starting validation container..."
docker run -it --rm -v $(pwd):/workspace --workdir /workspace python:3-slim bash -c '
  echo "Installing JSON Schema validator..."
  pip install check-jsonschema > /dev/null

  echo "Validating meta-schema..."
  check-jsonschema --schemafile https://json-schema.org/draft/2020-12/schema parameters_meta_schema.json

  echo -e "\nValidating test schemas..."
  failed=0
  for schema in test_schemas/*.json; do
    echo "Testing $schema:"
    if check-jsonschema --schemafile parameters_meta_schema.json "$schema"; then
      printf "\033[32m✓ Valid\033[0m\n"
    else
      printf "\033[31m✗ Invalid\033[0m\n"
      failed=1
    fi
    echo
  done
  exit $failed
'