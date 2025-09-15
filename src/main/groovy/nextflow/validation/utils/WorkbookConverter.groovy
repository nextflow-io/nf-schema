package nextflow.validation.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.io.FileInputStream
import java.io.IOException

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.openxml4j.exceptions.InvalidFormatException

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException

/**
 * Workbook converter for reading Excel files (XLSX, XLSM, XLSB, XLS)
 * and converting them to lists compatible with nf-schema validation
 *
 * @author : edmundmiller <edmund.a.miller@gmail.com>
 */

@Slf4j
@CompileStatic
class WorkbookConverter {

    // Constants for Excel file formats
    private static final List<String> EXCEL_EXTENSIONS = ['xlsx', 'xlsm', 'xlsb', 'xls']
    private static final int DEFAULT_SHEET_INDEX = 0

    private ValidationConfig config

    WorkbookConverter(ValidationConfig config) {
        this.config = config
    }

    /**
     * Convert Excel workbook to List format
     */
    public List<Map<String, Object>> convertToList(
        Path workbookFile,
        Map options = null
    ) {
        // Ensure options is not null
        if (options == null) {
            options = [:]
        }
        def colors = Colors.fromConfig(config)

        // Validate file exists
        if (!workbookFile.exists()) {
            def msg = "${colors.red}Excel workbook file ${workbookFile.toString()} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        try {
            return readWorkbook(workbookFile, options)
        } catch (Exception e) {
            def msg = "${colors.red}Failed to read Excel file ${workbookFile.toString()}: ${e.message}\n${colors.reset}\n"
            log.error("Failed to read Excel workbook!")
            throw new SchemaValidationException(msg)
        }
    }

    /**
     * Read workbook and convert to list format
     */
    private List<Map<String, Object>> readWorkbook(Path workbookFile, Map options) {
        Workbook workbook = null
        List<Map<String, Object>> result = []

        try {
            // Open workbook based on file format
            workbook = openWorkbook(workbookFile)

            // Get the specified sheet or default to first sheet
            Sheet sheet = getSheet(workbook, options)

            // Convert sheet to list format
            result = convertSheetToList(sheet)

        } finally {
            if (workbook != null) {
                workbook.close()
            }
        }

        return result
    }

    /**
     * Open workbook based on file format
     */
    private Workbook openWorkbook(Path workbookFile) {
        try {
            // Use WorkbookFactory for automatic format detection
            return WorkbookFactory.create(workbookFile.toFile())
        } catch (Exception e) {
            def colors = Colors.fromConfig(config)
            def msg = "${colors.red}Failed to open Excel file ${workbookFile}: ${e.message}\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }
    }

    /**
     * Get sheet from workbook based on options
     */
    private Sheet getSheet(Workbook workbook, Map options) {
        def sheetSelector = options.sheet
        Sheet sheet = null
        def colors = Colors.fromConfig(config)

        if (sheetSelector == null) {
            // Default to first sheet
            sheet = workbook.getSheetAt(DEFAULT_SHEET_INDEX)
        } else if (sheetSelector instanceof String) {
            // Select by sheet name
            sheet = workbook.getSheet(sheetSelector as String)
            if (sheet == null) {
                def msg = "${colors.red}Sheet '${sheetSelector}' not found in workbook\n${colors.reset}\n"
                throw new SchemaValidationException(msg)
            }
        } else if (sheetSelector instanceof Integer) {
            // Select by sheet index
            def sheetIndex = sheetSelector as Integer
            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                def msg = "${colors.red}Sheet index ${sheetIndex} is out of range (0-${workbook.getNumberOfSheets()-1})\n${colors.reset}\n"
                throw new SchemaValidationException(msg)
            }
            sheet = workbook.getSheetAt(sheetIndex)
        } else {
            def msg = "${colors.red}Sheet selector must be either a String (sheet name) or Integer (sheet index)\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        return sheet
    }

    /**
     * Convert Excel sheet to list of maps
     */
    private List<Map<String, Object>> convertSheetToList(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() == 0) {
            return []
        }

        // Process headers
        List<String> headers = processHeaders(sheet)
        boolean hasHeader = headers.any { it != null && !it.trim().isEmpty() }

        // Process data rows
        return processDataRows(sheet, headers, hasHeader)
    }

    /**
     * Process headers from the sheet
     */
    private List<String> processHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum())
        if (headerRow == null) {
            return []
        }

        List<String> headers = extractHeaders(headerRow)
        boolean hasValidHeaders = headers.any { it != null && !it.trim().isEmpty() }

        // If no valid headers, create generic column names
        if (!hasValidHeaders) {
            def colCount = headerRow.getLastCellNum()
            headers = (0..<colCount).collect { "column_${it}".toString() }
        }

        return headers
    }

    /**
     * Process data rows from the sheet
     */
    private List<Map<String, Object>> processDataRows(Sheet sheet, List<String> headers, boolean hasHeader) {
        List<Map<String, Object>> result = []

        int startRow = hasHeader ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum()
        int endRow = sheet.getLastRowNum()

        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex)
            if (row != null) {
                Map<String, Object> rowData = processRow(row, headers)
                if (rowData && !rowData.isEmpty()) {
                    result.add(rowData)
                }
            }
        }

        return result
    }

    /**
     * Extract header names from header row
     */
    private List<String> extractHeaders(Row headerRow) {
        List<String> headers = []

        for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
            Cell cell = headerRow.getCell(colIndex)
            String headerValue = getCellValue(cell, true) as String
            headers.add(headerValue ?: "column_${colIndex}".toString())
        }

        return headers
    }

    /**
     * Process a data row and convert to map
     */
    private Map<String, Object> processRow(Row row, List<String> headers) {
        Map<String, Object> rowData = [:]
        boolean hasData = false

        for (int colIndex = 0; colIndex < Math.max(headers.size(), row.getLastCellNum()); colIndex++) {
            Cell cell = row.getCell(colIndex)
            Object cellValue = getCellValue(cell)

            String header = colIndex < headers.size() ? headers[colIndex] : "column_${colIndex}".toString()
            rowData[header] = cellValue

            if (cellValue != null && !(cellValue instanceof String && ((String)cellValue).trim().isEmpty())) {
                hasData = true
            }
        }

        return hasData ? rowData : [:]
    }

    /**
     * Extract cell value based on cell type
     * @param cell The cell to extract value from
     * @param asString If true, returns string representation for headers
     */
    private Object getCellValue(Cell cell, boolean asString = false) {
        if (cell == null) {
            return asString ? "" : null
        }

        try {
            switch (cell.getCellType()) {
                case CellType.STRING:
                    return cell.getStringCellValue()?.trim() ?: (asString ? "" : null)
                case CellType.NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Handle date cells
                        Date date = cell.getDateCellValue()
                        return new SimpleDateFormat("yyyy-MM-dd").format(date)
                    } else {
                        double numValue = cell.getNumericCellValue()
                        if (asString) {
                            // For string representation, format appropriately
                            return (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) ?
                                String.valueOf((int)numValue) : String.valueOf(numValue)
                        } else {
                            // Return as integer if it's a whole number
                            return (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) ?
                                (int)numValue : numValue
                        }
                    }
                case CellType.BOOLEAN:
                    return asString ? String.valueOf(cell.getBooleanCellValue()) : cell.getBooleanCellValue()
                case CellType.FORMULA:
                    Object result = evaluateFormula(cell)
                    return asString ? String.valueOf(result) : result
                case CellType.BLANK:
                    return asString ? "" : null
                case CellType.ERROR:
                    return "#ERROR#"
                default:
                    return cell.toString()?.trim() ?: (asString ? "" : null)
            }
        } catch (Exception e) {
            log.warn("Error reading cell value at row ${cell.getRowIndex()}, column ${cell.getColumnIndex()}: ${e.message}")
            return cell.toString()?.trim() ?: (asString ? "" : null)
        }
    }

    /**
     * Evaluate formula in a cell
     */
    private Object evaluateFormula(Cell cell) {
        try {
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator()
            CellValue cellValue = evaluator.evaluate(cell)

            switch (cellValue.getCellType()) {
                case CellType.NUMERIC:
                    double numValue = cellValue.getNumberValue()
                    if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                        return (int)numValue
                    } else {
                        return numValue
                    }
                case CellType.STRING:
                    return cellValue.getStringValue()?.trim()
                case CellType.BOOLEAN:
                    return cellValue.getBooleanValue()
                default:
                    return cell.getCellFormula()
            }
        } catch (Exception e) {
            log.warn("Error evaluating formula: ${e.message}")
            return cell.getCellFormula()
        }
    }

    /**
     * Check if file is an Excel format
     */
    public static boolean isExcelFile(Path file) {
        def extension = Files.getFileExtension(file.toString().toLowerCase())
        return extension in EXCEL_EXTENSIONS
    }
}