include { paramsSummaryLog } from 'plugin/nf-schema'

workflow {
    log.info paramsSummaryLog(workflow)
}
