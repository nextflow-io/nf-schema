package nextflow.validation

import nextflow.Session
import nextflow.trace.TraceObserverV2
import nextflow.trace.TraceObserverFactoryV2

import groovy.transform.CompileStatic

/**
 * An observer factory to create validation observers
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileStatic
class ValidationObserverFactory implements TraceObserverFactoryV2 {

    @Override
    Collection<TraceObserverV2> create(Session session) {
        List<TraceObserverV2> observers = [ new ValidationObserver() ]
        return observers
    }

}
