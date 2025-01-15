package nextflow.validation.help

import groovy.util.logging.Slf4j

import nextflow.processor.TaskHandler
import nextflow.trace.TraceObserver
import nextflow.trace.TraceRecord
import nextflow.Session

import nextflow.validation.help.HelpMessage
import nextflow.validation.config.ValidationConfig

@Slf4j
class HelpObserver implements TraceObserver {
    
    @Override
    void onFlowCreate(Session session) {
        // Help message logic
        def Map params = (Map)session.params ?: [:]
        def ValidationConfig config = new ValidationConfig(session?.config?.navigate('validation') as Map, params)
        def Boolean containsFullParameter = params.containsKey(config.help.fullParameter) && params[config.help.fullParameter]
        def Boolean containsShortParameter = params.containsKey(config.help.shortParameter) && params[config.help.shortParameter]
        if (config.help.enabled && (containsFullParameter || containsShortParameter)) {
            def String help = ""
            def HelpMessage helpMessage = new HelpMessage(config, session)
            help += helpMessage.getBeforeText()
            if (containsFullParameter) {
                log.debug("Printing out the full help message")
                help += helpMessage.getFullHelpMessage()
            } else if (containsShortParameter) {
                log.debug("Printing out the short help message")
                def paramValue = params.get(config.help.shortParameter)
                help += helpMessage.getShortHelpMessage(paramValue instanceof String ? paramValue : "")
            }
            help += helpMessage.getAfterText()
            log.info(help)
            System.exit(0)
        }
    }
}