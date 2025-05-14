package nextflow.validation

import nextflow.Session
import nextflow.trace.TraceObserverV2

import groovy.util.logging.Slf4j

/**
 * An observer for initial checks that always need to be run at the start of the pipeline 
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */


@Slf4j
class ValidationObserver implements TraceObserverV2 {
    
    @Override
    void onFlowCreate(Session session) {
        def plugins = session?.config?.navigate("plugins") as ArrayList
        if(plugins?.contains("nf-schema")) {
            log.warn("""
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!                                                                 !
!                            WARNING!                             !
!                                                                 !
!                You just entered the danger zone!                !
!         Please pin the nf-schema version in your config!        !
!   Not pinning your version can't guarantee the reproducibility  !
!       and the functionality of this pipeline in the future      !
!                                                                 !
!                    plugins {                                    !
!                        id "nf-schema@<version>"                 !
!                    }                                            !
!                                                                 !
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            """)
        }
    }
}