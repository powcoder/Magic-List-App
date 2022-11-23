https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.user;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class UserQuote {

    public static final String KEY_ID = "quote_id";
    public static final String KEY_TEXT = "quote_text";
    public static final String KEY_AUTHOR = "quote_author";

    private final String id;
    private final User user;
    private final String text;
    private final String author;
    private final long date;

    public UserQuote(String id, User user, String text, String author, long date) {
        this.id = id;
        this.user = user;
        this.text = text;
        this.author = author;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public long getDate() {
        return date;
    }
}
