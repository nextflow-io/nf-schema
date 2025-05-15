include { samplesheetToList } from 'plugin/nf-schema'

workflow {
    ch_input = Channel.fromList(samplesheetToList(params.input, "assets/schema_input.json"))

    ch_input.view()
}

