https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package excel;

import com.myjeeva.poi.*;
import model.prospect.Prospect;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import play.Logger;
import utilities.RandomStringGenerator;
import utilities.StringUtility;
import utilities.Validation;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Corey on 3/10/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ExcelDocumentParser implements XSSFSheetXMLHandler.SheetContentsHandler, Closeable {

    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_COMPANY = "company";
    private static final String KEY_JOB = "job";
    private static final String KEY_NOTES = "notes";

    private final File file;
    private final List<Prospect> prospectList = new ArrayList<>();
    /**
     * Maps the prospect's keys to values for the given row
     */
    private final Map<String, String> prospectRowKeyValuesMap = new HashMap<>();
    private final Logger.ALogger logger = Logger.of(this.getClass());

    private OPCPackage opcPackage;
    private ExcelReader excelReader;

    private int rowNumber = 0;
    private int cellIndex = 0;

    private int nameIndex;
    private int emailIndex;
    private int companyNameIndex;
    private int jobTitleIndex;
    private int phoneIndex;
    private int notesIndex;
    private boolean isHeaderIndicesInitialized = false;

    private Map<Integer, Boolean> invalidEmailLinesMap = new HashMap<>();

    public ExcelDocumentParser(File file) {
        this.file = file;
        nameIndex = -1;
        emailIndex = -1;
        companyNameIndex = -1;
        jobTitleIndex = -1;
        phoneIndex = -1;
        notesIndex = -1;
    }

    public boolean isFileValid() {
        try {
            opcPackage = OPCPackage.open(file, PackageAccess.READ);
            excelReader = new ExcelReader(opcPackage, this, null);
            return true;
        } catch (Exception e) {
            logger.debug("Error: ", e);
            return false;
        }
    }

    public List<Prospect> getProspectListFromExcel() throws Exception {
        excelReader.process();

        logger.debug("[name: {}, email: {}, phone: {}, job: {}, company: {}, notes: {}]",
                nameIndex, emailIndex, phoneIndex, jobTitleIndex, companyNameIndex, notesIndex);

        logger.debug("List Size: {}", prospectList.size());

        return prospectList;
    }

    @Override
    public void startRow(int rowNumber) {
        this.rowNumber = rowNumber;
        this.prospectRowKeyValuesMap.clear();
        this.cellIndex = 0;
    }

    @Override
    public void endRow() {
        String personId = RandomStringGenerator.getInstance().getNextRandomPersonId();
        String name = prospectRowKeyValuesMap.get(KEY_NAME);
        String email = prospectRowKeyValuesMap.get(KEY_EMAIL);
        String phone = prospectRowKeyValuesMap.get(KEY_PHONE);
        String job = prospectRowKeyValuesMap.get(KEY_JOB);
        String company = prospectRowKeyValuesMap.get(KEY_COMPANY);
        String notes = prospectRowKeyValuesMap.get(KEY_NOTES);

        int zeroIndexRowNumber = rowNumber - 1;
        Pattern pattern = Pattern.compile(StringUtility.EMAIL_GROUPING_REGEX);
        if (!Validation.isEmpty(email)) {
            Matcher matcher = pattern.matcher(email);
            if(!matcher.find()) {
                invalidEmailLinesMap.put(zeroIndexRowNumber, true);
                logger.debug("Found invalid email at row {}, cell: {}", zeroIndexRowNumber, cellIndex);
            } else {
                email = matcher.group();
                invalidEmailLinesMap.put(zeroIndexRowNumber, false);
            }
        } else {
            invalidEmailLinesMap.put(zeroIndexRowNumber, false);
        }

        Prospect prospect = Prospect.Factory.createFromRawInput(personId, name, email, phone, job, company, notes);

        if (prospect.getName() != null && (prospect.getEmail() != null || prospect.getPhoneNumber() != null)) {
            prospectList.add(prospect);
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue) {
        logger.debug("Row Number: {}, Reference: {}; Cell: {}", rowNumber, cellReference, formattedValue);
        cellIndex = getCellIndexFromCellReference(cellReference);

        logger.debug("Cell Index: {}", cellIndex);

        if (formattedValue == null) {
            return;
        } else {
            formattedValue = formattedValue.trim();
        }

        if (rowNumber > 0 && !isHeaderIndicesInitialized) {
            String reason = "The header values for the excel file are invalid. Please make sure they are named properly and in the first row.";
            throw new RuntimeException(reason);
        } else if (rowNumber == 0) {
            initHeaderFromRow(formattedValue);
        } else {
            getValueFromCell(formattedValue);
        }

    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
        if (isHeader) {
            initHeaderFromRow(text);
        }

        cellIndex += 1;
    }

    @Override
    public void close() throws IOException {
        opcPackage.close();
    }

    String splitHyperlink(String hyperLink) {
        Matcher matcher = Pattern.compile(".+\".*\".+\"(.*)\".+").matcher(hyperLink);
        if (matcher.matches()) {
            // Group's are 1-index based
            return matcher.group(1);
        } else {
            logger.error("Error: ", new IllegalStateException("HYPERLINK did not match regex"));
            return null;
        }
    }

    public Map<Integer, Boolean> getInvalidEmailLinesMap() {
        return invalidEmailLinesMap;
    }

    // Private Methods

    private void initHeaderFromRow(String text) {
        text = text.toLowerCase();
        if (nameIndex == -1 && (text.contains(KEY_NAME) || text.equalsIgnoreCase(KEY_NAME))) {
            nameIndex = cellIndex;
        }
        if (emailIndex == -1 && (text.contains(KEY_EMAIL) || text.equalsIgnoreCase(KEY_EMAIL))) {
            emailIndex = cellIndex;
        }
        if (phoneIndex == -1 && (text.contains(KEY_PHONE) || text.equalsIgnoreCase(KEY_PHONE))) {
            phoneIndex = cellIndex;
        }
        if (jobTitleIndex == -1 && (text.contains(KEY_JOB) || text.equalsIgnoreCase(KEY_JOB))) {
            jobTitleIndex = cellIndex;
        }
        if (companyNameIndex == -1 && (text.contains(KEY_COMPANY) || text.equalsIgnoreCase(KEY_COMPANY))) {
            companyNameIndex = cellIndex;
        }
        if (notesIndex == -1 && (text.contains(KEY_NOTES) || text.equalsIgnoreCase(KEY_NOTES))) {
            notesIndex = cellIndex;
        }

        isHeaderIndicesInitialized = nameIndex != -1 && (emailIndex != -1 || phoneIndex != -1);
    }

    private void getValueFromCell(String formattedValue) {
        String value = cleanse(formattedValue);

        if (cellIndex == nameIndex) {
            prospectRowKeyValuesMap.put(KEY_NAME, value);
        } else if (cellIndex == emailIndex) {
            prospectRowKeyValuesMap.put(KEY_EMAIL, value);
        } else if (cellIndex == phoneIndex) {
            prospectRowKeyValuesMap.put(KEY_PHONE, value);
        } else if (cellIndex == jobTitleIndex) {
            prospectRowKeyValuesMap.put(KEY_JOB, value);
        } else if (cellIndex == companyNameIndex) {
            prospectRowKeyValuesMap.put(KEY_COMPANY, value);
        } else if (cellIndex == notesIndex) {
            prospectRowKeyValuesMap.put(KEY_NOTES, value);
        }
    }

    private String cleanse(String formattedValue) {
        if (formattedValue.toLowerCase().contains("hyperlink")) {
            return splitHyperlink(formattedValue);
        } else {
            return formattedValue;
        }
    }

    private int getCellIndexFromCellReference(String cellReference) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cellReference.length(); i++) {
            if (Character.isAlphabetic(cellReference.charAt(i))) {
                builder.append(cellReference.charAt(i));
            }
        }

        String s = builder.toString().toUpperCase();
        int digitPlace = (s.length() - 1) * 26;
        int amount = 0;
        for (int i = 0; i < s.length(); i++) {
            int letterValue = (s.charAt(i) - 'A' + 1);
            amount += ((digitPlace * letterValue) + letterValue);
            digitPlace -= 26;
        }

        return amount;
    }

}
