https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class User {

    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_NAME = "name";
    public static final String KEY_JOIN_DATE = "join_date";

    private final String userId;
    private final String email;
    private final String name;
    private final long joinDate;

    public User(String userId) {
        this(userId, null, null, -1);
    }

    public User(String userId, String email, String name, long joinDate) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.joinDate = joinDate;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public long getJoinDate() {
        return joinDate;
    }

    @Override
    public String toString() {
        return new Converter().renderAsJsonObject(this).toString();
    }

    public static class Converter implements JsonConverter<User> {

        @Override
        public ObjectNode renderAsJsonObject(User user) {
            return Json.newObject()
                    .put(KEY_USER_ID, escape(user.userId))
                    .put(KEY_EMAIL, escape(user.email))
                    .put(KEY_NAME, escape(user.name))
                    .put(KEY_JOIN_DATE, user.joinDate);
        }

        @Override
        public User deserializeFromJson(ObjectNode objectNode) {
            throw new RuntimeException("Invalid constructor");
        }
    }

}
