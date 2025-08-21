include { validateParameters } from 'plugin/nf-schema'

workflow {
    validateParameters()

    println "Hello World!"
}