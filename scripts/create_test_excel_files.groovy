#!/usr/bin/env groovy

@Grab('org.apache.poi:poi:5.4.1')
@Grab('org.apache.poi:poi-ooxml:5.4.1')
@Grab('org.apache.poi:poi-scratchpad:5.4.1')

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat

/**
 * Helper script to create Excel test files for nf-schema testing
 */
def createTestFiles() {
    def testResourcesDir = Paths.get("src/testResources")

    // Create directory if it doesn't exist
    testResourcesDir.toFile().mkdirs()

    println "Creating Excel test files..."

    // 1. Create correct.xlsx (basic test file equivalent to correct.csv)
    createBasicTestFile(testResourcesDir.resolve("correct.xlsx").toString(), "xlsx")

    // 2. Create multisheet.xlsx (multiple sheets for sheet selection testing)
    createMultiSheetFile(testResourcesDir.resolve("multisheet.xlsx").toString())

    // 3. Create empty_cells.xlsx (file with empty cells)
    createEmptyCellsFile(testResourcesDir.resolve("empty_cells.xlsx").toString())

    println "✅ Excel test files created successfully in ${testResourcesDir}"
}

def createBasicTestFile(String filename, String format) {
    Workbook workbook = format == "xls" ? new HSSFWorkbook() : new XSSFWorkbook()
    Sheet sheet = workbook.createSheet("Sheet1")

    // Create header row matching correct.csv structure
    Row headerRow = sheet.createRow(0)
    def headers = ["sample", "fastq_1", "fastq_2", "strandedness"]
    headers.eachWithIndex { header, index ->
        headerRow.createCell(index).setCellValue(header)
    }

    // Add data rows matching test samplesheet data
    def data = [
        ["SAMPLE_PE", "SAMPLE_PE_RUN1_1.fastq.gz", "SAMPLE_PE_RUN1_2.fastq.gz", "forward"],
        ["SAMPLE_PE", "SAMPLE_PE_RUN2_1.fastq.gz", "SAMPLE_PE_RUN2_2.fastq.gz", "forward"],
        ["SAMPLE_SE", "SAMPLE_SE_RUN1_1.fastq.gz", "", "forward"]
    ]

    data.eachWithIndex { row, rowIndex ->
        Row dataRow = sheet.createRow(rowIndex + 1)
        row.eachWithIndex { value, colIndex ->
            if (value != null && value != "") {
                Cell cell = dataRow.createCell(colIndex)
                cell.setCellValue(value.toString())
            }
        }
    }

    // Auto-size columns
    headers.eachWithIndex { header, index ->
        sheet.autoSizeColumn(index)
    }

    // Save file
    def fileOut = new FileOutputStream(filename)
    workbook.write(fileOut)
    fileOut.close()
    workbook.close()

    println "Created: ${filename}"
}

def createMultiSheetFile(String filename) {
    Workbook workbook = new XSSFWorkbook()

    // Sheet 1 - Same as basic test file
    Sheet sheet1 = workbook.createSheet("Sheet1")
    Row headerRow1 = sheet1.createRow(0)
    def headers = ["sample", "fastq_1", "fastq_2", "strandedness"]
    headers.eachWithIndex { header, index ->
        headerRow1.createCell(index).setCellValue(header)
    }

    Row dataRow1 = sheet1.createRow(1)
    def data1 = ["SAMPLE_PE", "SAMPLE_PE_RUN1_1.fastq.gz", "SAMPLE_PE_RUN1_2.fastq.gz", "forward"]
    data1.eachWithIndex { value, colIndex ->
        Cell cell = dataRow1.createCell(colIndex)
        cell.setCellValue(value.toString())
    }

    // Sheet 2 - Different data
    Sheet sheet2 = workbook.createSheet("Sheet2")
    Row headerRow2 = sheet2.createRow(0)
    headerRow2.createCell(0).setCellValue("sample_id")
    headerRow2.createCell(1).setCellValue("condition")

    Row dataRow2 = sheet2.createRow(1)
    dataRow2.createCell(0).setCellValue("sample2")
    dataRow2.createCell(1).setCellValue("control")

    // Save file
    def fileOut = new FileOutputStream(filename)
    workbook.write(fileOut)
    fileOut.close()
    workbook.close()

    println "Created: ${filename}"
}

def createEmptyCellsFile(String filename) {
    Workbook workbook = new XSSFWorkbook()
    Sheet sheet = workbook.createSheet("Sheet1")

    // Create header row
    Row headerRow = sheet.createRow(0)
    def headers = ["sample", "fastq_1", "fastq_2", "strandedness"]
    headers.eachWithIndex { header, index ->
        headerRow.createCell(index).setCellValue(header)
    }

    // Add row with many empty cells
    Row dataRow = sheet.createRow(1)
    dataRow.createCell(0).setCellValue("SAMPLE_SE")  // sample
    dataRow.createCell(1).setCellValue("SAMPLE_SE_RUN1_1.fastq.gz")  // fastq_1
    // fastq_2 left empty
    dataRow.createCell(3).setCellValue("forward")      // strandedness

    // Save file
    def fileOut = new FileOutputStream(filename)
    workbook.write(fileOut)
    fileOut.close()
    workbook.close()

    println "Created: ${filename}"
}

// Run the script
createTestFiles()