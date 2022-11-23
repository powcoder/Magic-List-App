https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import controllers.BaseTestController;
import model.dialsheet.DialSheet;
import org.junit.Before;
import org.junit.Test;
import play.db.Database;

import static org.junit.Assert.*;

public class DialSheetDBAccessorTest extends BaseTestController {

    private DialSheetDBAccessor dialSheetDBAccessor;

    @Before
    public void setup() {
        dialSheetDBAccessor = new DialSheetDBAccessor(app.injector().instanceOf(Database.class));
    }

    @Test
    public void getDialSheetForToday() throws Exception {
        DialSheet dialSheet = dialSheetDBAccessor.getCurrentAllPagesDialSheet(USER_ID);
        assertNotNull(dialSheet);

        DialSheet secondSheet = dialSheetDBAccessor.getCurrentAllPagesDialSheet(USER_ID);
        assertEquals(dialSheet, secondSheet);
    }

}