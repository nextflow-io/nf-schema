package nextflow.validation

import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory

class ValidationObserverFactory implements TraceObserverFactory {

    @Override
    Collection<TraceObserver> create(Session session) {
        // Only enable the trace observer when a help message needs to be printed
        final enabled = session.config.navigate('validation.help.enabled')
        return enabled ? [ new ValidationObserver() ] : []
    }
}