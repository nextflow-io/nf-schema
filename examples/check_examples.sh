script_dir=$(dirname "$0")
plugin_version=$(grep 'version =' "$script_dir/../build.gradle" | awk -F "'" '{print $2}')

echo "Testing examples with nf-schema version $plugin_version"

failures=0
ignore_values='runName|launchDir|workDir|Check script|projectDir|userName|configFiles'

run_example() {
    echo "Running example command: $command"
    output=$(eval $command 2>&1 | tail -n +5 | grep -vE "$ignore_values")

    expected_text=$(cat $expected_output | tail -n +5 | grep -vE "$ignore_values")

    echo "Comparing with expected output: $expected_output"
    if [ "$output" == "$expected_text" ]; then
        echo "Test passed!"
    else
        failures=$((failures + 1))
        echo "Test failed!"
        diff <(echo "$output") <(echo "$expected_text")
    fi
}

for example in $script_dir/*/pipeline/main.nf
do
    example_dir=$(dirname "$example")
    if compgen -G "$example_dir/../options*.txt" > /dev/null; then
        for option in $example_dir/../options*.txt
        do
            command="COLUMNS=1000 nextflow run $example -plugins nf-schema@$plugin_version $(cat $option)"
            expected_output=$(echo $option | sed -e 's/options/log/')
            run_example
        done
    else
        command="COLUMNS=1000 nextflow run $example -plugins nf-schema@$plugin_version"
        expected_output="$(dirname $example)/../log.txt"
        run_example
    fi
done

if [ $failures -eq 0 ]; then
    echo "All tests passed!"
else
    echo "$failures test(s) failed."
    exit 1
fi