package nextflow.validation.utils

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * A collection of functions for everything color related
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
public class Colors {

    //
    // ANSII Colours used for terminal logging
    //
    static Map getLogColors(Boolean monochromeLogs) {
        Map colorcodes = [:]

        // Reset / Meta
        colorcodes['reset']      = monochromeLogs ? '' : '\033[0m'
        colorcodes['bold']       = monochromeLogs ? '' : '\033[1m'
        colorcodes['dim']        = monochromeLogs ? '' : '\033[2m'
        colorcodes['underlined'] = monochromeLogs ? '' : '\033[4m'
        colorcodes['blink']      = monochromeLogs ? '' : '\033[5m'
        colorcodes['reverse']    = monochromeLogs ? '' : '\033[7m'
        colorcodes['hidden']     = monochromeLogs ? '' : '\033[8m'

        // Regular Colors
        colorcodes['black']      = monochromeLogs ? '' : '\033[0;30m'
        colorcodes['red']        = monochromeLogs ? '' : '\033[0;31m'
        colorcodes['green']      = monochromeLogs ? '' : '\033[0;32m'
        colorcodes['yellow']     = monochromeLogs ? '' : '\033[0;33m'
        colorcodes['blue']       = monochromeLogs ? '' : '\033[0;34m'
        colorcodes['purple']     = monochromeLogs ? '' : '\033[0;35m'
        colorcodes['cyan']       = monochromeLogs ? '' : '\033[0;36m'
        colorcodes['white']      = monochromeLogs ? '' : '\033[0;37m'

        // Bold
        colorcodes['bblack']     = monochromeLogs ? '' : '\033[1;30m'
        colorcodes['bred']       = monochromeLogs ? '' : '\033[1;31m'
        colorcodes['bgreen']     = monochromeLogs ? '' : '\033[1;32m'
        colorcodes['byellow']    = monochromeLogs ? '' : '\033[1;33m'
        colorcodes['bblue']      = monochromeLogs ? '' : '\033[1;34m'
        colorcodes['bpurple']    = monochromeLogs ? '' : '\033[1;35m'
        colorcodes['bcyan']      = monochromeLogs ? '' : '\033[1;36m'
        colorcodes['bwhite']     = monochromeLogs ? '' : '\033[1;37m'

        // Underline
        colorcodes['ublack']     = monochromeLogs ? '' : '\033[4;30m'
        colorcodes['ured']       = monochromeLogs ? '' : '\033[4;31m'
        colorcodes['ugreen']     = monochromeLogs ? '' : '\033[4;32m'
        colorcodes['uyellow']    = monochromeLogs ? '' : '\033[4;33m'
        colorcodes['ublue']      = monochromeLogs ? '' : '\033[4;34m'
        colorcodes['upurple']    = monochromeLogs ? '' : '\033[4;35m'
        colorcodes['ucyan']      = monochromeLogs ? '' : '\033[4;36m'
        colorcodes['uwhite']     = monochromeLogs ? '' : '\033[4;37m'

        // High Intensit
        colorcodes['iblack']     = monochromeLogs ? '' : '\033[0;90m'
        colorcodes['ired']       = monochromeLogs ? '' : '\033[0;91m'
        colorcodes['igreen']     = monochromeLogs ? '' : '\033[0;92m'
        colorcodes['iyellow']    = monochromeLogs ? '' : '\033[0;93m'
        colorcodes['iblue']      = monochromeLogs ? '' : '\033[0;94m'
        colorcodes['ipurple']    = monochromeLogs ? '' : '\033[0;95m'
        colorcodes['icyan']      = monochromeLogs ? '' : '\033[0;96m'
        colorcodes['iwhite']     = monochromeLogs ? '' : '\033[0;97m'

        // Bold High Intensity
        colorcodes['biblack']    = monochromeLogs ? '' : '\033[1;90m'
        colorcodes['bired']      = monochromeLogs ? '' : '\033[1;91m'
        colorcodes['bigreen']    = monochromeLogs ? '' : '\033[1;92m'
        colorcodes['biyellow']   = monochromeLogs ? '' : '\033[1;93m'
        colorcodes['biblue']     = monochromeLogs ? '' : '\033[1;94m'
        colorcodes['bipurple']   = monochromeLogs ? '' : '\033[1;95m'
        colorcodes['bicyan']     = monochromeLogs ? '' : '\033[1;96m'
        colorcodes['biwhite']    = monochromeLogs ? '' : '\033[1;97m'

        return colorcodes
    }

    //
    // Remove all ANSII colors from a piece of text
    //
    static String removeColors(String input) {
        if (!input) { return input }
        String output = input
        List colors = getLogColors(false)*.value
        colors.each { color ->
            output = output.replace(color, '')
        }
        return output
    }

}
