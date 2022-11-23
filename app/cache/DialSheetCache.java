https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package cache;

import model.dialsheet.AllPagesDialSheet;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Corey Caplan on 10/19/17.
 */
public class DialSheetCache {

    public static AllPagesDialSheet getCurrentDialSheet(String userId) {
        return MemCacheWrapper.getInstance().get(getDailyDialSheetHash(userId));
    }

    public static void setCurrentDialSheet(String userId, AllPagesDialSheet dialSheet) {
        MemCacheWrapper.getInstance().set(getDailyDialSheetHash(userId), MemCacheWrapper.FIFTEEN_MINUTES, dialSheet);
    }

    public static void removeCurrentDialSheet(String userId) {
        MemCacheWrapper.getInstance().removeAndBlock(getDailyDialSheetHash(userId));
    }

    // Private Methods

    private static String getDailyDialSheetHash(String userId) {
        String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date());
        return MemCacheWrapper.createHash(userId + date);
    }

}
