https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.calendar;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.JsonConverter;
import play.libs.Json;
import utilities.DateUtility;

public class BaseCalendarTemplate {

    public static final String KEY_ID = "template_id";
    public static final String KEY_ACCOUNT_ID = "oauth_account_id";
    public static final String KEY_TEMPLATE_NAME = "template_name";
    public static final String KEY_CREATION_DATE = "template_creation_date";
    public static final String KEY_CALENDAR_PROVIDER = "template_provider";

    private final String oauthAccountId;
    private final String templateId;
    private final String templateName;
    private final long creationDate;
    private final CalendarProvider calendarProvider;

    public BaseCalendarTemplate(String oauthAccountId, String templateId, String templateName, long creationDate,
                                CalendarProvider calendarProvider) {
        this.oauthAccountId = oauthAccountId;
        this.templateId = templateId;
        this.templateName = templateName;
        this.creationDate = creationDate;
        this.calendarProvider = calendarProvider;
    }

    public String getOauthAccountId() {
        return oauthAccountId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public String getCreationDateForUi() {
        return DateUtility.getLongDateForUi(creationDate);
    }

    public CalendarProvider getCalendarProvider() {
        return calendarProvider;
    }

    public static class Converter implements JsonConverter<BaseCalendarTemplate> {

        @Override
        public ObjectNode renderAsJsonObject(BaseCalendarTemplate object) {
            return Json.newObject()
                    .put(KEY_ID, escape(object.templateId))
                    .put(KEY_ACCOUNT_ID, escape(object.oauthAccountId))
                    .put(KEY_TEMPLATE_NAME, escape(object.templateName))
                    .put(KEY_CREATION_DATE, escape(object.getCreationDateForUi()))
                    .put(KEY_CALENDAR_PROVIDER, escape(object.calendarProvider.getRawText()));
        }

        @Override
        public BaseCalendarTemplate deserializeFromJson(ObjectNode objectNode) {
            String templateId = objectNode.get(KEY_ID).asText();
            String oauthAccountId = objectNode.get(KEY_ACCOUNT_ID).asText();
            String templateName = objectNode.get(KEY_TEMPLATE_NAME).asText();
            long creationDate = objectNode.get(KEY_CREATION_DATE).asLong();
            String rawProvider = objectNode.get(KEY_CALENDAR_PROVIDER).asText();
            CalendarProvider provider = CalendarProvider.parse(rawProvider);
            return new BaseCalendarTemplate(oauthAccountId, templateId, templateName, creationDate, provider);
        }
    }

}
