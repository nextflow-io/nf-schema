package nextflow.validation

import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactoryV2

import nextflow.validation.help.HelpObserver

import groovy.transform.CompileDynamic

/**
 * An observer factory to create validation observers
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileDynamic
class ValidationObserverFactory implements TraceObserverFactoryV2 {

    @Override
    Collection<TraceObserverV2> create(Session session) {
        List<TraceObserverV2> observers = [ new ValidationObserver() ]
        // Only enable the help observer when a help message needs to be printed
        if (session.config.navigate('validation.help.enabled')) {
            observers.add(new HelpObserver())
        }
        return observers
    }

}
