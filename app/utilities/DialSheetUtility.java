https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import database.DialSheetDBAccessor;
import model.dialsheet.AllPagesDialSheet;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.Result;

import static controllers.BaseController.*;

/**
 * Created by Corey on 8/1/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public final class DialSheetUtility {

    private static Logger.ALogger logger = Logger.of(DialSheetUtility.class);

    private DialSheetUtility() {
    }

    public static Result getCurrentDialSheet(String userId) {
        // We need to return the updated dial sheet since the person's state changed
        AllPagesDialSheet dialSheet = new DialSheetDBAccessor(getDatabase())
                .getCurrentAllPagesDialSheet(userId);
        if (dialSheet != null) {
            return sendJsonOk(MagicListObject.serializeToJson(dialSheet));
        } else {
            String reason = "Could not retrieve updated activity sheet";
            logger.error(reason, new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
