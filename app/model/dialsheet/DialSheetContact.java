https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import model.prospect.Prospect;
import model.prospect.ProspectState;

/**
 * Created by Corey on 3/17/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class DialSheetContact extends Prospect {

    private final long contactTime;

    public DialSheetContact(Prospect p, long contactTime) {
        super(p.getId(), p.getName(), p.getEmail(), p.getPhoneNumber(), p.getJobTitle(), p.getCompanyName(),
                p.getState(), p.getNotes(), p.getDateCreated());
        this.contactTime = contactTime;
    }

    public long getContactTime() {
        return contactTime;
    }

}
