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
public class EventAttendee extends EventRecipient {

    private static final String KEY_TYPE = "type";
    private static final String KEY_STATUS = "status";
    private static final String KEY_RESPONSE = "response";
    private static final String KEY_TIME = "time";

    private final Status status;
    private final String time;
    private final Type type;

    /**
     * Constructor from event template
     */
    public EventAttendee(String email, boolean isAttendanceRequired) {
        this(null, email, Status.NONE, null, isAttendanceRequired);
    }

    /**
     * Constructor from activity sheet appointment
     */
    public EventAttendee(String email, String displayName, boolean isAttendanceRequired) {
        this(displayName, email, Status.NONE, null, isAttendanceRequired);
    }

    public EventAttendee(String displayName, String email, Status status, String time, boolean isAttendanceRequired) {
        super(displayName, email);
        this.status = status;
        this.time = time;
        this.type = isAttendanceRequired ? Type.REQUIRED : Type.OPTIONAL;
    }

    public EventAttendee(String displayName, String email, Status status, String time, Type type) {
        super(displayName, email);
        this.status = status;
        this.time = time;
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public String getTime() {
        return time;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return new Converter().renderAsJsonObject(this).toString();
    }

    public enum Status {

        NONE, ORGANIZER, TENTATIVELY_ACCEPTED, ACCEPTED, DECLINED;

        private static final String KEY_NONE = "none";
        private static final String KEY_ORGANIZER = "organizer";
        private static final String KEY_TENTATIVELY_ACCEPTED = "tentatively_accepted";
        private static final String KEY_ACCEPTED = "accepted";
        private static final String KEY_DECLINED = "declined";

        public String getRawText() {
            if (this == NONE) {
                return KEY_NONE;
            } else if (this == ORGANIZER) {
                return KEY_ORGANIZER;
            } else if (this == TENTATIVELY_ACCEPTED) {
                return KEY_TENTATIVELY_ACCEPTED;
            } else if (this == ACCEPTED) {
                return KEY_ACCEPTED;
            } else if (this == DECLINED) {
                return KEY_DECLINED;
            } else {
                System.err.println("Invalid state, found: " + this);
                return "Invalid";
            }
        }

        @Override
        public String toString() {
            if (this == NONE) {
                return "None";
            } else if (this == ORGANIZER) {
                return "Organizer";
            } else if (this == TENTATIVELY_ACCEPTED) {
                return "Tentatively Accepted";
            } else if (this == ACCEPTED) {
                return "Accepted";
            } else if (this == DECLINED) {
                return "Declined";
            } else {
                System.err.println("Invalid state, found: " + this);
                return "Invalid";
            }
        }

        public static Status parse(String rawStatus) {
            if (KEY_NONE.equalsIgnoreCase(rawStatus)) {
                return NONE;
            } else if (KEY_ORGANIZER.equalsIgnoreCase(rawStatus)) {
                return ORGANIZER;
            } else if (KEY_TENTATIVELY_ACCEPTED.equalsIgnoreCase(rawStatus)) {
                return TENTATIVELY_ACCEPTED;
            } else if (KEY_ACCEPTED.equalsIgnoreCase(rawStatus)) {
                return ACCEPTED;
            } else if (KEY_DECLINED.equalsIgnoreCase(rawStatus)) {
                return DECLINED;
            } else {
                System.err.println("Invalid state, found: " + rawStatus);
                return null;
            }
        }

    }

    public enum Type {

        REQUIRED, OPTIONAL, RESOURCE;

        private static final String KEY_REQUIRED = "required";
        private static final String KEY_OPTIONAL = "optional";
        private static final String KEY_RESOURCE = "resource";

        public String getRawText() {
            if (this == REQUIRED) {
                return KEY_REQUIRED;
            } else if (this == OPTIONAL) {
                return KEY_OPTIONAL;
            } else if (this == RESOURCE) {
                return KEY_RESOURCE;
            } else {
                System.err.println("Invalid Type, found: " + this);
                return "Invalid";
            }
        }

        @Override
        public String toString() {
            if (this == REQUIRED) {
                return "Required";
            } else if (this == OPTIONAL) {
                return "Optional";
            } else if (this == RESOURCE) {
                return "Resource";
            } else {
                System.err.println("Invalid Type, found: " + this);
                return "Invalid";
            }
        }

        public static Type parse(String rawType) {
            if (KEY_REQUIRED.equalsIgnoreCase(rawType)) {
                return REQUIRED;
            } else if (KEY_OPTIONAL.equalsIgnoreCase(rawType)) {
                return OPTIONAL;
            } else if (KEY_RESOURCE.equalsIgnoreCase(rawType)) {
                return RESOURCE;
            } else {
                System.err.println("Invalid Type, found: " + rawType);
                return null;
            }
        }

    }

    public static class Converter implements JsonConverter<EventAttendee> {

        @Override
        public ObjectNode renderAsJsonObject(EventAttendee object) {
            ObjectNode objectNode = new EventRecipient.Converter().renderAsJsonObject(object);
            ObjectNode statusNode = Json.newObject()
                    .put(KEY_RESPONSE, escape(object.status.getRawText()))
                    .put(KEY_TIME, object.time);

            objectNode.put(KEY_TYPE, escape(object.type.getRawText()));
            objectNode.set(KEY_STATUS, statusNode);
            return objectNode;
        }

        @Override
        public EventAttendee deserializeFromJson(ObjectNode objectNode) {
            EventRecipient recipient = new EventRecipient.Converter().deserializeFromJson(objectNode);
            String type = objectNode.get(KEY_TYPE).asText();
            String response = objectNode.get(KEY_STATUS).get(KEY_RESPONSE).asText();
            String time = objectNode.get(KEY_STATUS).get(KEY_TIME).asText();
            String name = recipient.getDisplayName();
            String email = recipient.getEmail();
            return new EventAttendee(name, email, Status.parse(response), time, Type.parse(type));
        }

    }

}
