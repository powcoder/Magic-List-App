https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.prospect;

import model.user.User;

/**
 * Created by Corey on 3/13/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ContactStatusAuditItem {

    private final ProspectState state;
    private final long date;
    private final String dialSheetId;
    private final User contacter;

    public static ContactStatusAuditItem createInitialAuditTrailItem(long dateCreated) {
        return new ContactStatusAuditItem(ProspectState.NOT_CONTACTED, dateCreated, null, null);
    }

    public ContactStatusAuditItem(ProspectState state, long date, String dialSheetId, User contacter) {
        this.state = state;
        this.date = date;
        this.dialSheetId = dialSheetId;
        this.contacter = contacter;
    }

    public ProspectState getState() {
        return state;
    }

    public long getDate() {
        return date;
    }

    public String getDialSheetId() {
        return dialSheetId;
    }

    public User getContacter() {
        return contacter;
    }
}
