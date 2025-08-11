include { paramsHelp } from 'plugin/nf-schema'

workflow {
    if (params.help) {
        log.info paramsHelp(
            beforeText: "This is a help message for the pipeline.",
            afterText: "Done with the help message.",
            command: "nextflow run main.nf",
        )
        exit 0
    }
}
