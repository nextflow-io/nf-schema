package nextflow.validation.help

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

import nextflow.trace.TraceObserverV2
import nextflow.Session

import nextflow.validation.config.ValidationConfig

/**
 * An observer class to print the help message at the start of the pipeline
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class HelpObserver implements TraceObserverV2 {

    @Override
    void onFlowCreate(Session session) {
        // Help message logic
        Map params = (Map)session.params ?: [:]
        ValidationConfig config = new ValidationConfig(session?.config?.navigate('validation') as Map, session)
        Boolean containsFullParameter = params.containsKey(config.help.fullParameter) &&
            params[config.help.fullParameter]
        Boolean containsShortParameter = params.containsKey(config.help.shortParameter) &&
            params[config.help.shortParameter]
        if (config.help.enabled && (containsFullParameter || containsShortParameter)) {
            String help = ''
            HelpMessageCreator helpCreator = new HelpMessageCreator(config, session)
            help += helpCreator.beforeText
            if (containsFullParameter) {
                log.debug('Printing out the full help message')
                help += helpCreator.fullMessage
            } else if (containsShortParameter) {
                log.debug('Printing out the short help message')
                String paramValue = params.get(config.help.shortParameter)
                help += helpCreator.getShortMessage(paramValue in String ? paramValue : '')
            }
            help += helpCreator.afterText
            log.info(help)
            session.cancel()
        }
    }

}

