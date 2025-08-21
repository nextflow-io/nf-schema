include { paramsSummaryMap } from 'plugin/nf-schema'

workflow {
    paramsMap = paramsSummaryMap(workflow)
    prettyPrint(paramsMap)
}


def prettyPrint(map, indent=0) {
    map.each { k, v ->
        if(v instanceof Map) {
            println("${'  ' * indent}${k}:")
            prettyPrint(v, indent+1)
        } else {
            println("${'  ' * indent}${k}:${v}")
        }
    }
}