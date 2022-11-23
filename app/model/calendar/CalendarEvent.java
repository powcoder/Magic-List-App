https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.calendar;

public class CalendarEvent {

    private final String oauthAccountId;
    private final CalendarProvider provider;
    private final String eventId;
    private String hyperLink;
    private String subject;

    public CalendarEvent(CalendarProvider provider, String eventId, String oauthAccountId, String hyperLink,
                         String subject) {
        this.provider = provider;
        this.eventId = eventId;
        this.oauthAccountId = oauthAccountId;
        this.hyperLink = hyperLink;
        this.subject = subject;
    }

    public CalendarProvider getProvider() {
        return provider;
    }

    public String getEventId() {
        return eventId;
    }

    public String getOauthAccountId() {
        return oauthAccountId;
    }

    public String getHyperLink() {
        return hyperLink;
    }

    public String getSubject() {
        return subject;
    }

    public void setHyperLink(String hyperLink) {
        this.hyperLink = hyperLink;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
