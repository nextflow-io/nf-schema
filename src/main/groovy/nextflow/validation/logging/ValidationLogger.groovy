package nextflow.validation.logging

import static nextflow.validation.utils.Colors.getLogColors

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import nextflow.validation.exceptions.SchemaValidationException

/**
 * This class is used to define logging logic that can be configured via configuration options.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
class ValidationLogger {

    final private static List<String> LEVELS = ['skip', 'debug', 'info', 'warn', 'error']

    final private String level = 'info'
    final private Map colors

    ValidationLogger(String level, Boolean monochromeLogs = false) {
        level = level?.toLowerCase()
        if (level in LEVELS) {
            this.level = level
        } else {
            log.error("Invalid log level '${level}' specified, defaulting to 'info'")
        }
        this.colors = getLogColors(monochromeLogs)
    }

    static ValidationLogger of(String label, Object level, ValidationLogger fallback, Boolean monochromeLogs) {
        if (level == null) {
            return fallback
        }
        if (LEVELS.contains(level.toString())) {
            log.debug("Set `${label}` to ${level}")
            return new ValidationLogger(level.toString(), monochromeLogs)
        }
        /* groovylint-disable-next-line LineLength */
        log.warn("Incorrect value detected for `${label}`, one of (${LEVELS.join(', ')}) is expected. Defaulting to `${fallback.level}`")
        return fallback
    }

    void log(String message) {
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
                throw new SchemaValidationException("${colors.red}${message}${colors.reset}" as String)
            default:
                log.info(message) // Fallback to info if something goes wrong
        }
    }

}
