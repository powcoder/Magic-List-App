https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.user;

import model.user.User;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class BugReport {

    public static final String KEY_ID = "bug_id";
    public static final String KEY_TEXT = "bug_text";

    private final String id;
    private final User bugReporter;
    private final String text;
    private final long date;

    public BugReport(String id, User bugReporter, String text, long date) {
        this.id = id;
        this.bugReporter = bugReporter;
        this.text = text;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public User getBugReporter() {
        return bugReporter;
    }

    public String getText() {
        return text;
    }

    public long getDate() {
        return date;
    }
}
