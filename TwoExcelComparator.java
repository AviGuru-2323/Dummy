import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelComparator {

    public static void main(String[] args) throws Exception {
        FileInputStream fis1 = new FileInputStream("EXCEL_1.xlsx");
        FileInputStream fis2 = new FileInputStream("EXCEL_2.xlsx");

        Workbook wb1 = new XSSFWorkbook(fis1);
        Workbook wb2 = new XSSFWorkbook(fis2);

        Sheet sheet1 = wb1.getSheetAt(0);
        Sheet sheet2 = wb2.getSheetAt(0);

        // Assuming first row is header
        Row headerRow = sheet1.getRow(0);
        int totalCols = headerRow.getLastCellNum();

        // Map portfolio_id -> Row (sheet2 for comparison)
        Map<String, Row> sheet2DataMap = new HashMap<>();
        for (int i = 1; i <= sheet2.getLastRowNum(); i++) {
            Row row = sheet2.getRow(i);
            if (row == null) continue;
            Cell idCell = row.getCell(0); // assuming column A is portfolio_id
            if (idCell != null) {
                sheet2DataMap.put(idCell.toString().trim(), row);
            }
        }

        // Output workbook
        Workbook diffWorkbook = new XSSFWorkbook();
        Sheet diffSheet = diffWorkbook.createSheet("Differences");
        int diffRowNum = 0;

        // Header for difference file
        Row diffHeader = diffSheet.createRow(diffRowNum++);
        diffHeader.createCell(0).setCellValue("portfolio_id");
        diffHeader.createCell(1).setCellValue("Column Name");
        diffHeader.createCell(2).setCellValue("EXCEL_1 Value");
        diffHeader.createCell(3).setCellValue("EXCEL_2 Value");
        diffHeader.createCell(4).setCellValue("Update Statement");

        // Loop through sheet1
        for (int i = 1; i <= sheet1.getLastRowNum(); i++) {
            Row row1 = sheet1.getRow(i);
            if (row1 == null) continue;

            String portfolioId = row1.getCell(0).toString().trim();
            Row row2 = sheet2DataMap.get(portfolioId);
            if (row2 == null) continue;

            for (int j = 1; j < totalCols; j++) { // skip column 0 (portfolio_id)
                Cell cell1 = row1.getCell(j);
                Cell cell2 = row2.getCell(j);
                String value1 = getCellValue(cell1);
                String value2 = getCellValue(cell2);

                if (!Objects.equals(value1, value2)) {
                    String headerName = headerRow.getCell(j).toString().trim();
                    Row diffRow = diffSheet.createRow(diffRowNum++);
                    diffRow.createCell(0).setCellValue(portfolioId);
                    diffRow.createCell(1).setCellValue(headerName);
                    diffRow.createCell(2).setCellValue(value1);
                    diffRow.createCell(3).setCellValue(value2);

                    String updateStmt = String.format(
                        "UPDATE mst_portfolio SET %s='%s' WHERE portfolio_id='%s';",
                        headerName, value1.replace("'", "''"), portfolioId
                    );
                    diffRow.createCell(4).setCellValue(updateStmt);
                }
            }
        }

        FileOutputStream fos = new FileOutputStream("DIFFERENCES.xlsx");
        diffWorkbook.write(fos);

        // Close resources
        fos.close();
        fis1.close();
        fis2.close();
        wb1.close();
        wb2.close();
        diffWorkbook.close();

        System.out.println("Comparison complete. Differences written to DIFFERENCES.xlsx.");
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
