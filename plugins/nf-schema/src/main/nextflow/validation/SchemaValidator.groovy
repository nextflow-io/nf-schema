package nextflow.validation

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import nextflow.extension.CH
import nextflow.extension.DataflowHelper
import nextflow.Channel
import nextflow.Global
import nextflow.Nextflow
import nextflow.plugin.extension.Operator
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.script.WorkflowMetadata
import nextflow.Session
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import nextflow.config.ConfigMap
import org.json.JSONException
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.yaml.snakeyaml.Yaml

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileStatic
class SchemaValidator extends PluginExtensionPoint {

    static final List<String> NF_OPTIONS = [
            // Options for base `nextflow` command
            'bg',
            'c',
            'C',
            'config',
            'd',
            'D',
            'dockerize',
            'h',
            'log',
            'q',
            'quiet',
            'syslog',
            'v',

            // Options for `nextflow run` command
            'ansi',
            'ansi-log',
            'bg',
            'bucket-dir',
            'c',
            'cache',
            'config',
            'dsl2',
            'dump-channels',
            'dump-hashes',
            'E',
            'entry',
            'latest',
            'lib',
            'main-script',
            'N',
            'name',
            'offline',
            'params-file',
            'pi',
            'plugins',
            'poll-interval',
            'pool-size',
            'profile',
            'ps',
            'qs',
            'queue-size',
            'r',
            'resume',
            'revision',
            'stdin',
            'stub',
            'stub-run',
            'test',
            'w',
            'with-charliecloud',
            'with-conda',
            'with-dag',
            'with-docker',
            'with-mpi',
            'with-notification',
            'with-podman',
            'with-report',
            'with-singularity',
            'with-timeline',
            'with-tower',
            'with-trace',
            'with-weblog',
            'without-docker',
            'without-podman',
            'work-dir'
    ]

    private List<String> errors = []
    private List<String> warnings = []

    // The amount of parameters hidden (for help messages)
    private Integer hiddenParametersCount = 0

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100

    @Override
    protected void init(Session session) {
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

    Session getSession(){
        Global.getSession() as Session
    }  

    ValidationConfig getConfig() {
        new ValidationConfig(session.config.navigate('validation') as Map)
    }

    boolean hasErrors() { errors.size()>0 }
    List<String> getErrors() { errors }

    boolean hasWarnings() { warnings.size()>0 }
    List<String> getWarnings() { warnings }

    //
    // Find a value in a nested map
    //
    def findDeep(Map m, String key) {
        if (m.containsKey(key)) return m[key]
        m.findResult { k, v -> v instanceof Map ? findDeep(v, key) : null }
    }

    @Function
    public List samplesheetToList(
        final CharSequence samplesheet,
        final CharSequence schema,
        final Map options = null
    ) {
        def Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema, options)
    }

    @Function
    public List samplesheetToList(
        final Path samplesheet,
        final CharSequence schema,
        final Map options = null
    ) {
        def String fullPathSchema = Utils.getSchemaPath(session.baseDir.toString(), schema as String)
        def Path schemaFile = Nextflow.file(fullPathSchema) as Path
        return samplesheetToList(samplesheet, schemaFile, options)
    }

    @Function
    public List samplesheetToList(
        final CharSequence samplesheet,
        final Path schema,
        final Map options = null
    ) {
        def Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema, options)
    }

    @Function
    public List samplesheetToList(
        final Path samplesheet,
        final Path schema,
        final Map options = null
    ) {
        def SamplesheetConverter converter = new SamplesheetConverter(samplesheet, schema, config, options)
        return converter.validateAndConvertToList()
    }

    //
    // Initialise expected params if not present
    //
    Map initialiseExpectedParams(Map params) {
        if( !params.containsKey("help") ) {
            params.help = false
        }
        return params
    }


    //
    // Add expected params
    //
    List addExpectedParams() {
        def List expectedParams = [
            "help"
        ]

        return expectedParams
    }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {

        def Map params = initialiseExpectedParams(session.params)
        def String baseDir = session.baseDir.toString()
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        log.debug "Starting parameters validation"

        // Clean the parameters
        def cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(Utils.getSchemaPath(baseDir, schemaFilename)) )
        // $defs is the adviced keyword for definitions. Keeping defs in for backwards compatibility
        def Map schemaParams = (Map) (parsed.get('$defs') ?: parsed.get("defs"))
        def specifiedParamKeys = params.keySet()

        // Collect expected parameters from the schema
        def enumsTuple = collectEnums(schemaParams)
        def List expectedParams = (List) enumsTuple[0] + addExpectedParams()
        def Map enums = (Map) enumsTuple[1]
        // Collect expected parameters from the schema when parameters are specified outside of "$defs"
        if (parsed.containsKey('properties')) {
            def enumsTupleTopLevel = collectEnums(['top_level': ['properties': parsed.get('properties')]])
            expectedParams += (List) enumsTupleTopLevel[0]
            enums += (Map) enumsTupleTopLevel[1]
        }

        //=====================================================================//
        for (String specifiedParam in specifiedParamKeys) {
            // nextflow params
            if (NF_OPTIONS.contains(specifiedParam)) {
                errors << "You used a core Nextflow option with two hyphens: '--${specifiedParam}'. Please resubmit with '-${specifiedParam}'".toString()
            }
            // unexpected params
            def expectedParamsLowerCase = expectedParams.collect{ it -> 
                def String p = it
                p.replace("-", "").toLowerCase() 
            }
            def specifiedParamLowerCase = specifiedParam.replace("-", "").toLowerCase()
            def isCamelCaseBug = (specifiedParam.contains("-") && !expectedParams.contains(specifiedParam) && expectedParamsLowerCase.contains(specifiedParamLowerCase))
            if (!expectedParams.contains(specifiedParam) && !config.ignoreParams.contains(specifiedParam) && !isCamelCaseBug) {
                if (config.failUnrecognisedParams) {
                    errors << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                } else {
                    warnings << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                }
            }
        }

        //=====================================================================//
        // Validate parameters against the schema
        def String schema_string = Files.readString( Path.of(Utils.getSchemaPath(baseDir, schemaFilename)) )
        def validator = new JsonSchemaValidator(config)

        // check for warnings
        if( this.hasWarnings() ) {
            def msg = "The following invalid input values have been detected:\n\n" + this.getWarnings().join('\n').trim() + "\n\n"
            log.warn(msg)
        }

        // Colors
        def colors = Utils.logColours(config.monochromeLogs)

        // Validate
        List<String> validationErrors = validator.validate(paramsJSON, schema_string)
        this.errors.addAll(validationErrors)
        if (this.hasErrors()) {
            def msg = "${colors.red}The following invalid input values have been detected:\n\n" + errors.join('\n').trim() + "\n${colors.reset}\n"
            log.error("Validation of pipeline parameters failed!")
            throw new SchemaValidationException(msg, this.getErrors())
        }

        log.debug "Finishing parameters validation"
    }

    //
    // Function to collect enums (options) of a parameter and expected parameters (present in the schema)
    //
    Tuple collectEnums(Map schemaParams) {
        def expectedParams = []
        def enums = [:]
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                def String key = (String) p.key
                expectedParams.push(key)
                def Map property = properties[key] as Map
                if (property.containsKey('enum')) {
                    enums[key] = property['enum']
                }
            }
        }
        return new Tuple (expectedParams, enums)
    }


    //
    // Wrap too long text
    //
    String wrapText(String text) {
        List olines = []
        String oline = ""
        text.split(" ").each() { wrd ->
            if ((oline.size() + wrd.size()) <= terminalLength) {
                oline += wrd + " "
            } else {
                olines += oline
                oline = wrd + " "
            }
        }
        olines += oline
        return olines.join("\n")
    }

    //
    // Beautify parameters for --help
    //
    @Function
    String paramsHelp(
        Map options = null,
        String command
    ) {
        def Map params = initialiseExpectedParams(session.params)

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'

        def colors = Utils.logColours(config.monochromeLogs)
        Integer num_hidden = 0
        String output  = ''
        output        += 'Typical pipeline command:\n\n'
        output        += "  ${colors.cyan}${command}${colors.reset}\n\n"
        Map paramsMap = paramsLoad( Path.of(Utils.getSchemaPath(session.baseDir.toString(), schemaFilename)) )
        Integer maxChars  = paramsMaxChars(paramsMap) + 1

        // Make sure the hidden parameters count is 0
        hiddenParametersCount = 0

        // If a value is passed to help
        if (params.help instanceof String) {
            def String paramName = params.help
            def List<String> paramNames = params.help.tokenize(".") as List<String>
            def Map paramOptions = [:]
            for (group in paramsMap.keySet()) {
                def Map group_params = paramsMap.get(group) as Map // This gets the parameters of that particular group
                if (group_params.containsKey(paramNames[0])) {
                    paramOptions = group_params.get(paramNames[0]) as Map 
                }
            }
            if (paramNames.size() > 1) {
                paramNames.remove(0)
                paramNames.each {
                    paramOptions = (Map) paramOptions?.properties?[it] ?: [:]
                }
            }
            if (!paramOptions) {
                throw new Exception("Specified param '${paramName}' does not exist in JSON schema.")
            }
            output += getDetailedHelpString(paramName, paramOptions, colors)
            output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
            return output
        }

        for (group in paramsMap.keySet()) {
            Integer num_params = 0
            String group_output = "$colors.underlined$colors.bold$group$colors.reset\n"
            def Map group_params = paramsMap.get(group) as Map // This gets the parameters of that particular group
            group_output += getHelpList(group_params, colors, maxChars).join("\n") + "\n"
            if (group_output != "\n"){
                output += group_output
            }
        }
        if (num_hidden > 0){
            output += "$colors.dim !! Hiding $num_hidden params, use the 'validation.showHiddenParams' config value to show them !!\n$colors.reset"
        }
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        return output
    }

    //
    // Get help text in string format
    //
    private List<String> getHelpList(Map<String,Map> params, Map colors, Integer maxChars, String parentParameter = "") {
        def List helpMessage = []
        for (String paramName in params.keySet()) {
            def Map paramOptions = params.get(paramName) as Map 
            if (paramOptions.hidden && !config.showHiddenParams) {
                hiddenParametersCount += 1
                continue
            }
            def String type = '[' + paramOptions.type + ']'
            def String enumsString = ""
            if (paramOptions.enum != null) {
                def List enums = (List) paramOptions.enum
                def String chopEnums = enums.join(", ")
                if(chopEnums.length() > terminalLength){
                    chopEnums = chopEnums.substring(0, terminalLength-5)
                    chopEnums = chopEnums.substring(0, chopEnums.lastIndexOf(",")) + ", ..."
                }
                enumsString = " (accepted: " + chopEnums + ") "
            }
            def String description = paramOptions.description ? paramOptions.description as String + " " : ""
            def defaultValue = paramOptions.default != null ? "[default: " + paramOptions.default.toString() + "] " : ''
            def String nestedParamName = parentParameter ? parentParameter + "." + paramName : paramName
            def String nestedString = paramOptions.properties ? "(This parameter has sub-parameters. Use '--help ${nestedParamName}' to see all sub-parameters) " : ""
            def descriptionDefault = description + colors.dim + enumsString + defaultValue + colors.reset + nestedString
            // Wrap long description texts
            // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
            if (descriptionDefault.length() > terminalLength){
                descriptionDefault = wrapText(descriptionDefault)
            }
            helpMessage.add("  --" +  paramName.padRight(maxChars) + colors.dim + type.padRight(10) + colors.reset + descriptionDefault)
        }
        return helpMessage
    }

    //
    // Get a detailed help string from one parameter
    //
    private String getDetailedHelpString(String paramName, Map paramOptions, Map colors) {
        def String helpMessage = "--" + paramName + '\n'
        for (option in paramOptions) {
            def String key = option.key
            if (key == "fa_icon" || (key == "type" && option.value == "object")) {
                continue
            }
            if (key == "properties") {
                def Map subParamsOptions = option.value as Map
                def Integer maxChars = paramsMaxChars(subParamsOptions) + 2
                def String subParamsHelpString = getHelpList(subParamsOptions, colors, maxChars, paramName)
                    .collect {
                        "      ." + it[4..it.length()-1]
                    }
                    .join("\n")
                helpMessage += "    " + colors.dim + "options".padRight(11) + ": " + colors.reset + "\n" + subParamsHelpString + "\n"
                continue
            }
            def String value = option.value
            if (value.length() > terminalLength) {
                value = wrapText(value)
            }
            helpMessage += "    " + colors.dim + key.padRight(11) + ": " + colors.reset + value + '\n'
        }
        return helpMessage
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    public LinkedHashMap paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'
        def Map params = session.params
        
        // Get a selection of core Nextflow workflow options
        def Map workflowSummary = [:]
        if (workflow.revision) {
            workflowSummary['revision'] = workflow.revision
        }
        workflowSummary['runName']      = workflow.runName
        if (workflow.containerEngine) {
            workflowSummary['containerEngine'] = workflow.containerEngine
        }
        if (workflow.container) {
            workflowSummary['container'] = workflow.container
        }
        def String configFiles = workflow.configFiles
        workflowSummary['launchDir']    = workflow.launchDir
        workflowSummary['workDir']      = workflow.workDir
        workflowSummary['projectDir']   = workflow.projectDir
        workflowSummary['userName']     = workflow.userName
        workflowSummary['profile']      = workflow.profile
        workflowSummary['configFiles']  = configFiles.join(', ')

        // Get pipeline parameters defined in JSON Schema
        def Map paramsSummary = [:]
        def Map paramsMap = paramsLoad( Path.of(Utils.getSchemaPath(session.baseDir.toString(), schemaFilename)) )
        for (group in paramsMap.keySet()) {
            def Map groupSummary = getSummaryMapFromParams(params, paramsMap.get(group) as Map)
            paramsSummary.put(group, groupSummary)
        }
        return [ 'Core Nextflow options' : workflowSummary ] << paramsSummary as LinkedHashMap
    }


    //
    // Create a summary map for the given parameters
    //
    private Map getSummaryMapFromParams(Map params, Map paramsSchema) {
        def Map summary = [:]
        for (String param in paramsSchema.keySet()) {
            if (params.containsKey(param)) {
                def Map schema = paramsSchema.get(param) as Map 
                if (params.get(param) instanceof Map && schema.containsKey("properties")) {
                    summary.put(param, getSummaryMapFromParams(params.get(param) as Map, schema.get("properties") as Map))
                    continue
                }
                def String value = params.get(param)
                def String defaultValue = schema.get("default")
                def String type = schema.type
                if (defaultValue != null) {
                    if (type == 'string') {
                        // TODO rework this in a more flexible way
                        if (defaultValue.contains('$projectDir') || defaultValue.contains('${projectDir}')) {
                            def sub_string = defaultValue.replace('\$projectDir', '')
                            sub_string     = sub_string.replace('\${projectDir}', '')
                            if (value.contains(sub_string)) {
                                defaultValue = value
                            }
                        }
                        if (defaultValue.contains('$params.outdir') || defaultValue.contains('${params.outdir}')) {
                            def sub_string = defaultValue.replace('\$params.outdir', '')
                            sub_string     = sub_string.replace('\${params.outdir}', '')
                            if ("${params.outdir}${sub_string}" == value) {
                                defaultValue = value
                            }
                        }
                    }
                }

                // We have a default in the schema, and this isn't it
                if (defaultValue != null && value != defaultValue) {
                    summary.put(param, value)
                }
                // No default in the schema, and this isn't empty or false
                else if (defaultValue == null && value != "" && value != null && value != false && value != 'false') {
                    summary.put(param, value)
                }
            }
        }
        return summary
    }

    //
    // Beautify parameters for summary and return as string
    //
    @Function
    public String paramsSummaryLog(
        Map options = null,
        WorkflowMetadata workflow
    ) {

        def Map params = session.params

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : 'nextflow_schema.json'

        def colors = Utils.logColours(config.monochromeLogs)
        String output  = ''
        def Map paramsMap = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        paramsMap.each { key, value ->
            paramsMap[key] = flattenNestedParamsMap(value as Map)
        }
        def maxChars  = paramsMaxChars(paramsMap)
        for (group in paramsMap.keySet()) {
            def Map group_params = paramsMap.get(group) as Map // This gets the parameters of that particular group
            if (group_params) {
                output += "$colors.bold$group$colors.reset\n"
                for (String param in group_params.keySet()) {
                    output += "  " + colors.blue + param.padRight(maxChars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                }
                output += '\n'
            }
        }
        output += "!! Only displaying parameters that differ from the pipeline defaults !!\n"
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        return output
    }

    private Map flattenNestedParamsMap(Map paramsMap) {
        def Map returnMap = [:]
        paramsMap.each { param, value ->
            def String key = param as String
            if (value instanceof Map) {
                def Map flatMap = flattenNestedParamsMap(value as Map)
                flatMap.each { flatParam, flatValue ->
                    returnMap.put(key + "." + flatParam, flatValue)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }

    //
    // Clean and check parameters relative to Nextflow native classes
    //
    private static Map cleanParameters(Map params) {
        def Map new_params = (Map) params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value'] && p['value'] != 0) {
                new_params.remove(p.key)
            }
            // Cast MemoryUnit to String
            if (p['value'] instanceof MemoryUnit) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast Duration to String
            if (p['value'] instanceof Duration) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast LinkedHashMap to String
            if (p['value'] instanceof LinkedHashMap) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Parsed nested parameters
            if (p['value'] instanceof Map) {
                new_params.replace(p.key, cleanParameters(p['value'] as Map))
            }
        }
        return new_params
    }

    //
    // This function tries to read a JSON params file
    //
    private static LinkedHashMap paramsLoad(Path json_schema) {
        def paramsMap = new LinkedHashMap()
        try {
            paramsMap = paramsRead(json_schema)
        } catch (Exception e) {
            println "Could not read parameters settings from JSON. $e"
            paramsMap = new LinkedHashMap()
        }
        return paramsMap
    }

    //
    // Method to actually read in JSON file using Groovy.
    // Group (as Key), values are all parameters
    //    - Parameter1 as Key, Description as Value
    //    - Parameter2 as Key, Description as Value
    //    ....
    // Group
    //    -
    private static LinkedHashMap paramsRead(Path json_schema) throws Exception {
        def slurper = new JsonSlurper()
        def Map schema = (Map) slurper.parse( json_schema )
        // $defs is the adviced keyword for definitions. Keeping defs in for backwards compatibility
        def Map schema_defs = (Map) (schema.get('$defs') ?: schema.get("defs"))
        def Map schema_properties = (Map) schema.get('properties')
        /* Tree looks like this in nf-core schema
        * $defs <- this is what the first get('$defs') gets us
                group 1
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
                        parameter 2
                            type
                            description
                group 2
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
        * properties <- parameters can also be ungrouped, outside of $defs
                parameter 1
                    type
                    description
        */

        def paramsMap = new LinkedHashMap()
        // Grouped params
        if (schema_defs) {
            for (group in schema_defs) {
                def Map group_property = (Map) group.value['properties'] // Gets the property object of the group
                def String title = (String) group.value['title']
                def sub_params = new LinkedHashMap()
                group_property.each { innerkey, value ->
                    sub_params.put(innerkey, value)
                }
                paramsMap.put(title, sub_params)
            }
        }

        // Ungrouped params
        if (schema_properties) {
            def ungrouped_params = new LinkedHashMap()
            schema_properties.each { innerkey, value ->
                ungrouped_params.put(innerkey, value)
            }
            paramsMap.put("Other parameters", ungrouped_params)
        }

        return paramsMap
    }

    //
    // Get maximum number of characters across all parameter names
    //
    private static Integer paramsMaxChars( Map paramsMap) {
        Integer maxChars = 0
        for (group in paramsMap.keySet()) {
            def Map group_params = (Map) paramsMap.get(group)  // This gets the parameters of that particular group
            for (String param in group_params.keySet()) {
                if (param.size() > maxChars) {
                    maxChars = param.size()
                }
            }
        }
        return maxChars
    }
}
