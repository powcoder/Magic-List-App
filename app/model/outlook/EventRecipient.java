https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.outlook;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;

/**
 * Created by Corey on 3/19/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class EventRecipient {

    private static final String KEY_EMAIL_ADDRESS_OBJECT = "emailAddress";
    private static final String KEY_DISPLAY_NAME = "name";
    private static final String KEY_EMAIL_ADDRESS = "address";

    private final String displayName;
    private final String email;

    public EventRecipient(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public static class Converter implements JsonConverter<EventRecipient> {

        @Override
        public ObjectNode renderAsJsonObject(EventRecipient object) {
            return (ObjectNode) Json.newObject()
                    .set(KEY_EMAIL_ADDRESS_OBJECT, Json.newObject()
                            .put(KEY_DISPLAY_NAME, escape(object.displayName))
                            .put(KEY_EMAIL_ADDRESS, escape(object.email))
                    );
        }

        @Override
        public EventRecipient deserializeFromJson(ObjectNode objectNode) {
            String name = objectNode.get(KEY_EMAIL_ADDRESS_OBJECT).get(KEY_DISPLAY_NAME).asText();
            String email = objectNode.get(KEY_EMAIL_ADDRESS_OBJECT).get(KEY_EMAIL_ADDRESS).asText();
            return new EventRecipient(name, email);
        }

    }

}
