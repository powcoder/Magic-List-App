https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package excel;

import model.prospect.Prospect;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import play.Logger;

import java.io.*;
import java.util.List;

import static org.apache.poi.ss.usermodel.Font.BOLDWEIGHT_BOLD;

/**
 *
 */
public class ExcelDocumentCreator {

    private static final String KEY_SHEET_NAME = "Activity Sheet";
    private static final String FILE_EXTENSION = ".xlsx";

    // TODO use SXSSFWorkbook to stream the creation of the excel file

    private String fileName;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    /**
     * @param fileName The name of the file, without the file extension
     */
    public ExcelDocumentCreator(String fileName) {
        this.fileName = fileName + FILE_EXTENSION;
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(KEY_SHEET_NAME);
    }

    public ExcelFile createSheet(List<Prospect> personList) {
        createRowForPersonHeaders();
        final int OFFSET = 1;

        for (int i = 0; i < personList.size(); i++) {
            Prospect person = personList.get(i);
            Row row = sheet.createRow(OFFSET + i);
            int columnNumber = 0;

            Cell cell = row.createCell(columnNumber++);
            cell.setCellValue(person.getName());

            cell = row.createCell(columnNumber++);
            cell.setCellValue(person.getPhoneNumber());

            cell = row.createCell(columnNumber++);
            cell.setCellValue(person.getJobTitle());

            cell = row.createCell(columnNumber++);
            cell.setCellValue(person.getEmail());

            cell = row.createCell(columnNumber);
            cell.setCellValue(person.getCompanyName());
        }

        createConditionalFormatting(personList.size() + 1);

        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            workbook.write(stream);
            return new ExcelFile(fileName, stream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                Logger.error("Could not close stream: ", e);
            }
        }
    }

    private void createRowForPersonHeaders() {
        Row row = sheet.createRow(0);
        int rowNumber = 0;
        int characterCount = 48;

        Cell nameCell = row.createCell(rowNumber++);
        nameCell.setCellValue("Name");
        nameCell.setCellStyle(getCellStyleForHeader());
        sheet.setColumnWidth(0, 160 * characterCount);

        Cell cell = row.createCell(rowNumber++);
        cell.setCellValue("Phone");
        cell.setCellStyle(getCellStyleForHeader());
        sheet.setColumnWidth(1, 140 * characterCount);

        Cell jobTitleCell = row.createCell(rowNumber++);
        jobTitleCell.setCellValue("Job Title");
        jobTitleCell.setCellStyle(getCellStyleForHeader());
        sheet.setColumnWidth(2, 200 * characterCount);

        Cell emailCell = row.createCell(rowNumber++);
        emailCell.setCellValue("Email");
        emailCell.setCellStyle(getCellStyleForHeader());
        sheet.setColumnWidth(3, 220 * characterCount);

        Cell companyCell = row.createCell(rowNumber);
        companyCell.setCellValue("Company");
        companyCell.setCellStyle(getCellStyleForHeader());
        sheet.setColumnWidth(4, 140 * characterCount);
    }

    private CellStyle getCellStyleForHeader() {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBoldweight(BOLDWEIGHT_BOLD);
        cellStyle.setFont(font);
        return cellStyle;
    }

    private void createConditionalFormatting(int size) {
        XSSFSheetConditionalFormatting conditionalFormatting = sheet.getSheetConditionalFormatting();

        // Created banded rows
        ConditionalFormattingRule rule = conditionalFormatting.createConditionalFormattingRule("ISEVEN(ROW())");

        PatternFormatting patternFormatting = rule.createPatternFormatting();
        patternFormatting.setFillBackgroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.index);
        patternFormatting.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        CellRangeAddress[] cellRangeAddresses = {CellRangeAddress.valueOf("A1:E" + size)};
        conditionalFormatting.addConditionalFormatting(cellRangeAddresses, rule);
    }

    public static class ExcelFile {

        private final String filename;
        private final byte[] fileBytes;

        private ExcelFile(String filename, byte[] fileBytes) {
            this.filename = filename;
            this.fileBytes = fileBytes;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getFileBytes() {
            return fileBytes;
        }
    }

}
