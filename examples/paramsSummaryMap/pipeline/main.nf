include { paramsSummaryMap } from 'plugin/nf-schema'

workflow {
    println paramsSummaryMap(workflow)
}
