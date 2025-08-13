package nextflow.validation.logging

import groovy.util.logging.Slf4j

import nextflow.validation.exceptions.SchemaValidationException
import static nextflow.validation.utils.Colors.getLogColors


/**
 * This class is used to define logging logic that can be configured via configuration options.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class ValidationLogger {
    final private String level = 'info'
    final private Map colors

    ValidationLogger(String level, Boolean monochromeLogs = false) {
        level = level?.toLowerCase()
        if (level in ['skip', 'debug', 'info', 'warn', 'error']) {
            this.level = level
        } else {
            log.error("Invalid log level '${level}' specified, defaulting to 'info'")
            System.exit 1
        }
        this.colors = getLogColors(monochromeLogs)
    }

    public void log(String message) {
        switch (level) {
            case 'skip':
                // Do nothing, skip logging
                break
            case 'debug':
                log.debug(message)
                break
            case 'info':
                log.info(message)
                break
            case 'warn':
                log.warn(message)
                break
            case 'error':
                throw new SchemaValidationException(colors.red + message + colors.reset)
                break
            default:
                log.info(message) // Fallback to info if something goes wrong
        }
    }
}