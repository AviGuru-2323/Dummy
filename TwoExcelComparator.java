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

        Row headerRow = sheet1.getRow(0);
        int totalCols = headerRow.getLastCellNum();

        Map<String, Row> sheet2DataMap = new HashMap<>();
        for (int i = 1; i <= sheet2.getLastRowNum(); i++) {
            Row row = sheet2.getRow(i);
            if (row == null) continue;
            Cell idCell = row.getCell(0);
            if (idCell != null) {
                sheet2DataMap.put(idCell.toString().trim(), row);
            }
        }

        Workbook diffWorkbook = new XSSFWorkbook();
        Sheet diffSheet = diffWorkbook.createSheet("Differences");

        Workbook sqlWorkbook = new XSSFWorkbook();
        Sheet sqlSheet = sqlWorkbook.createSheet("SQL");

        int diffRowNum = 0;
        int sqlRowNum = 0;

        // Headers
        Row diffHeader = diffSheet.createRow(diffRowNum++);
        diffHeader.createCell(0).setCellValue("portfolio_id");
        diffHeader.createCell(1).setCellValue("Column Name");
        diffHeader.createCell(2).setCellValue("EXCEL_1 Value");
        diffHeader.createCell(3).setCellValue("EXCEL_2 Value");

        Row sqlHeader = sqlSheet.createRow(sqlRowNum++);
        sqlHeader.createCell(0).setCellValue("portfolio_id");
        sqlHeader.createCell(1).setCellValue("SQL Update Statement");

        // Map for storing updates per portfolio_id
        Map<String, Map<String, String>> updateMap = new HashMap<>();

        for (int i = 1; i <= sheet1.getLastRowNum(); i++) {
            Row row1 = sheet1.getRow(i);
            if (row1 == null) continue;

            String portfolioId = row1.getCell(0).toString().trim();
            Row row2 = sheet2DataMap.get(portfolioId);
            if (row2 == null) continue;

            for (int j = 1; j < totalCols; j++) {
                Cell cell1 = row1.getCell(j);
                Cell cell2 = row2.getCell(j);
                String value1 = getCellValue(cell1);
                String value2 = getCellValue(cell2);

                if (!Objects.equals(value1, value2)) {
                    String headerName = headerRow.getCell(j).toString().trim();

                    // Write to Differences sheet
                    Row diffRow = diffSheet.createRow(diffRowNum++);
                    diffRow.createCell(0).setCellValue(portfolioId);
                    diffRow.createCell(1).setCellValue(headerName);
                    diffRow.createCell(2).setCellValue(value1);
                    diffRow.createCell(3).setCellValue(value2);

                    // Collect update info
                    updateMap.putIfAbsent(portfolioId, new LinkedHashMap<>());
                    updateMap.get(portfolioId).put(headerName, value1.replace("'", "''"));
                }
            }
        }

        // Write updateMap to SQL.xlsx
        for (Map.Entry<String, Map<String, String>> entry : updateMap.entrySet()) {
            String portfolioId = entry.getKey();
            Map<String, String> updates = entry.getValue();

            StringBuilder sql = new StringBuilder("UPDATE mst_portfolio SET ");
            List<String> assignments = new ArrayList<>();
            for (Map.Entry<String, String> e : updates.entrySet()) {
                assignments.add(e.getKey() + "='" + e.getValue() + "'");
            }
            sql.append(String.join(", ", assignments));
            sql.append(" WHERE portfolio_id='").append(portfolioId).append("';");

            Row sqlRow = sqlSheet.createRow(sqlRowNum++);
            sqlRow.createCell(0).setCellValue(portfolioId);
            sqlRow.createCell(1).setCellValue(sql.toString());
        }

        // Save both files
        FileOutputStream diffOut = new FileOutputStream("DIFFERENCES.xlsx");
        diffWorkbook.write(diffOut);

        FileOutputStream sqlOut = new FileOutputStream("SQL.xlsx");
        sqlWorkbook.write(sqlOut);

        // Close everything
        diffOut.close();
        sqlOut.close();
        wb1.close();
        wb2.close();
        fis1.close();
        fis2.close();
        diffWorkbook.close();
        sqlWorkbook.close();

        System.out.println("Comparison done. Differences.xlsx and SQL.xlsx generated.");
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double d = cell.getNumericCellValue();
                    if (d == (long) d) return String.valueOf((long) d);
                    else return String.valueOf(d);
                }
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
