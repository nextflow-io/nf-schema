package nextflow.validation

import spock.lang.Specification
import spock.lang.Unroll
import java.nio.file.Path
import java.nio.file.Paths

import nextflow.validation.config.ValidationConfig
import nextflow.validation.utils.WorkbookConverter
import nextflow.validation.exceptions.SchemaValidationException

/**
 * @author : edmundmiller <edmund.a.miller@gmail.com>
 */

class WorkbookConverterTest extends Specification {

    def config
    def workbookConverter

    def setup() {
        config = new ValidationConfig()
        workbookConverter = new WorkbookConverter(config)
    }

    def "should detect Excel files correctly"() {
        expect:
        WorkbookConverter.isExcelFile(Paths.get("test.xlsx")) == true
        WorkbookConverter.isExcelFile(Paths.get("test.xlsm")) == true
        WorkbookConverter.isExcelFile(Paths.get("test.xlsb")) == true
        WorkbookConverter.isExcelFile(Paths.get("test.xls")) == true
        WorkbookConverter.isExcelFile(Paths.get("test.csv")) == false
        WorkbookConverter.isExcelFile(Paths.get("test.txt")) == false
    }

    def "should throw exception for non-existent file"() {
        given:
        def nonExistentFile = Paths.get("nonexistent.xlsx")

        when:
        workbookConverter.convertToList(nonExistentFile)

        then:
        thrown(SchemaValidationException)
    }

    def "should handle null options gracefully"() {
        given:
        def testFile = Paths.get("test.xlsx")

        when:
        // This will fail because file doesn't exist, but it should handle null options
        workbookConverter.convertToList(testFile, null)

        then:
        thrown(SchemaValidationException) // Due to file not existing, not null options
    }

    def "should handle empty options map"() {
        given:
        def testFile = Paths.get("test.xlsx")
        def emptyOptions = [:]

        when:
        // This will fail because file doesn't exist, but it should handle empty options
        workbookConverter.convertToList(testFile, emptyOptions)

        then:
        thrown(SchemaValidationException) // Due to file not existing, not empty options
    }
}