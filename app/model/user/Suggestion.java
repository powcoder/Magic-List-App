https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.user;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class Suggestion {

    public static final String KEY_ID = "suggestion_id";
    public static final String KEY_TEXT = "suggestion_text";

    private final String id;
    private final User suggester;
    private final String text;
    private final long date;

    public Suggestion(String id, User suggester, String text, long date) {
        this.id = id;
        this.suggester = suggester;
        this.text = text;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public User getSuggester() {
        return suggester;
    }

    public String getText() {
        return text;
    }

    public long getDate() {
        return date;
    }
}
