package nextflow.validation.samplesheet

import groovy.util.logging.Slf4j
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import groovy.json.JsonOutput

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SamplesheetCreationException
import static nextflow.validation.utils.Files.getFileType

/**
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class ListConverter {

    final private ValidationConfig config

    ListConverter(ValidationConfig config) {
        this.config = config
    }

    public void validateAndConvertToSamplesheet(
        List<Map> inputList,
        Path samplesheet,
        Path schema
    ) {
        def String fileType = getFileType(samplesheet)
        if(["csv", "tsv"].contains(fileType) && isNested(inputList)) {
            def String msg = "Cannot create a CSV or TSV samplesheet from a list of nested values."
            throw new SamplesheetCreationException(msg)
        }

        // TODO implement validation

        def List<Map> convertedList = convertNonStandardValues(inputList)

        switch(fileType) {
            case "csv":
                createSeparatedValueFile(convertedList, samplesheet, ",")
                break
            case "tsv":
                createSeparatedValueFile(convertedList, samplesheet, "\t")
                break
            case "yaml":
                samplesheet.text = new Yaml().dumpAs(convertedList, null, FlowStyle.BLOCK)
                break
            case "json":
                samplesheet.text = JsonOutput.prettyPrint(JsonOutput.toJson(convertedList))
                break
        }
    }

    private Boolean isNested(List input) {
        def Boolean nested = false
        input.each { entry ->
            entry.each { key, element ->
                if(element instanceof List || element instanceof Map) {
                    nested = true
                    return
                }
            }
            if(nested) return
        }
        return nested
    }

    private List<String> determineHeader(List<Map> inputList) {
        def List<String> headers = []
        inputList.each { entry ->
            entry.each { key, element ->
                if(!headers.contains(key)) {
                    headers.add(key)
                }
            }
        }
        return headers
    }

    private void createSeparatedValueFile(List<Map> inputList, Path samplesheet, String delimiter) {
        def List<String> headers = determineHeader(inputList)
        def List<String> content = [headers.join(delimiter)]
        inputList.each { entry ->
            def List<String> entryContent = []
            headers.each { header ->
                def Object item = entry.get(header)
                if(item == null) {
                    entryContent.add("")
                } else {
                    entryContent.add(item.toString())
                }
            }
            content.add(entryContent.join(delimiter))
        }
        samplesheet.text = content.join("\n")
    }

    private Object convertNonStandardValues(Object unconvertedObject) {
        if(
            unconvertedObject instanceof String ||
            unconvertedObject instanceof Integer ||
            unconvertedObject instanceof Float ||
            unconvertedObject instanceof Double ||
            unconvertedObject instanceof BigDecimal ||
            unconvertedObject instanceof Boolean
        ) {
            return unconvertedObject
        }

        if(unconvertedObject instanceof List) {
            return unconvertedObject.collect { element ->
                convertNonStandardValues(element)
            }
        }

        if(unconvertedObject instanceof Map) {
            return unconvertedObject.collectEntries { key, element ->
                [convertNonStandardValues(key), convertNonStandardValues(element)]
            }
        }

        if(unconvertedObject instanceof Path) {
            // TODO figure out a way to get the paths to published files instead
            return unconvertedObject.toAbsolutePath().toUri().toString().replace("file:///", "/")
        }

        return unconvertedObject.toString()
    }
}
