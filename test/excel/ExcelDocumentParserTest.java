https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package excel;

import com.sun.jna.platform.win32.Kernel32;
import org.apache.poi.ss.usermodel.Cell;
import org.junit.Before;
import org.junit.Test;
import play.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 *
 */
public class ExcelDocumentParserTest {

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private ExcelDocumentParser parser;
    private static final String VALID_FILE_PATH = "monmouth_alumni.xlsx";
    private static final String INVALID_FILE_PATH = "fake_excel.xlsx";

    @Before
    public void setUp() throws Exception {
        File[] files = new File("./").listFiles();
        assertNotNull(files);

        Arrays.stream(files)
                .forEach(file -> logger.debug("File: {}", file.getName()));

        parser = new ExcelDocumentParser(new File(VALID_FILE_PATH));
    }

    @Test
    public void isFileValid() throws Exception {
        assertTrue(parser.isFileValid());
        assertFalse(new ExcelDocumentParser(new File(INVALID_FILE_PATH)).isFileValid());
    }

    @Test
    public void stressTest() throws Exception {
        logger.debug("PID: {}", Kernel32.INSTANCE.GetCurrentProcessId());
        long start = new Date().getTime();
        List<CompletableFuture<Boolean>> list = new ArrayList<>();
        int amount = 1;
        for (int i = 0; i < amount; i++) {
            list.add(
                    CompletableFuture.supplyAsync(() -> {
                        ExcelDocumentParser parser = new ExcelDocumentParser(new File(VALID_FILE_PATH));
                        assertTrue(parser.isFileValid());
                        try {
                            parser.getProspectListFromExcel();
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
            );
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[list.size()]));

        assertTrue(list.stream().allMatch(CompletableFuture::join));

        long elapsedTimeMillis = new Date().getTime() - start;
        logger.debug("Millis Elapsed: {}", elapsedTimeMillis);
        logger.debug("Average per Sheet: {}", elapsedTimeMillis / amount);
    }

    @Test
    public void splitHyperlink() {
        String link = "HYPERLINK(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\",\"Shamrock Sales Inc\")";
        String link2 = "hyperlink(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\",\"Shamrock Sales Inc\")";
        String link3 = "HYPERLINK(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\" \"Shamrock Sales Inc\")";
        String link4 = "hyperlink(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\"Shamrock Sales Inc\")";
        String link5 = "hyperlink(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\",\"Shamrock Sales Inc)";
        String link6 = "hyperlink(\"http://subscriber.zoominfo.com/zoominfo/#!search/profile/company?companyId=348769448&targetid=profile\",Shamrock Sales Inc)";

        assertEquals("Shamrock Sales Inc", parser.splitHyperlink(link));
        assertEquals("Shamrock Sales Inc", parser.splitHyperlink(link2));
        assertEquals("Shamrock Sales Inc", parser.splitHyperlink(link3));
        assertNotEquals("Shamrock Sales Inc", parser.splitHyperlink(link4));
        assertNull(parser.splitHyperlink(link5));
        assertNull(parser.splitHyperlink(link6));
    }

}