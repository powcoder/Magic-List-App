https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.lists;

import model.serialization.MagicListObject;
import model.user.User;

import java.text.DateFormat;
import java.util.Arrays;

public class SavedList extends MagicListObject {

    public enum Sorter {
        SEARCH_NAME, DATE_CREATED;

        public static Sorter parse(String s) {
            return Arrays.stream(Sorter.values())
                    .filter(sorter -> sorter.toString().equalsIgnoreCase(s))
                    .findFirst()
                    .orElse(SEARCH_NAME);
        }
    }

    public static final String KEY_LIST_ID = "list_id";
    public static final String KEY_LIST_NAME = "list_name";
    public static final String KEY_DATE_CREATED = "date_created";
    public static final String KEY_COMMENT = "comment";

    private final String listId;
    private final String ownerId;
    private final String listName;
    private final long dateCreated;
    private final String comment;
    private final User sharer;
    private final long shareDate;

    public SavedList(String listId, String listName, long dateCreated, String comment) {
        this(listId, null, listName, dateCreated, comment, null, -1);
    }

    public SavedList(String listId, String ownerId, String listName, long dateCreated, String comment, User sharer, long shareDate) {
        this.listId = listId;
        this.ownerId = ownerId;
        this.listName = listName;
        this.dateCreated = dateCreated;
        this.comment = comment;
        this.sharer = sharer;
        this.shareDate = shareDate;
    }

    public String getListId() {
        return listId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getListName() {
        return listName;
    }

    public String getDateCreated() {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        return dateFormat.format(dateCreated);
    }

    public long getRawSearchDate() {
        return dateCreated;
    }

    public String getComment() {
        return comment;
    }

    public User getSharer() {
        return sharer;
    }

    public long getShareDate() {
        return shareDate;
    }

}
