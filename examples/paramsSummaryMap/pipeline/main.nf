include { paramsSummaryMap } from 'plugin/nf-schema'

workflow {
    paramsMap = paramsSummaryMap(workflow)
    // Remove unstable keys from the paramsMap
    paramsMap["Core Nextflow options"] = paramsMap["Core Nextflow options"] - paramsMap["Core Nextflow options"].subMap("runName", "launchDir", "workDir", "projectDir", "userName", "configFiles")
    println(paramsMap)
}
