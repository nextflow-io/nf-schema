package nextflow.validation

import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceObserverFactory

import nextflow.validation.help.HelpObserver

class ValidationObserverFactory implements TraceObserverFactory {

    @Override
    Collection<TraceObserver> create(Session session) {
        def List observers = []
        // Only enable the help observer when a help message needs to be printed
        if(session.config.navigate('validation.help.enabled')) {
            observers.add(new HelpObserver())
        }
        return observers
    }
}