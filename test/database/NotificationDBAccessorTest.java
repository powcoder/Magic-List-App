https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import controllers.BaseTestController;
import model.prospect.Notification;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import org.junit.Before;
import org.junit.Test;
import play.db.Database;
import utilities.RandomStringGenerator;

import java.util.Calendar;

import static org.junit.Assert.*;

public class NotificationDBAccessorTest extends BaseTestController {

    private NotificationDBAccessor notificationDBAccessor;
    private final long ONE_WEEK = 60 * 60 * 24 * 7;

    @Before
    public void setup() {
        notificationDBAccessor = new NotificationDBAccessor(app.injector().instanceOf(Database.class));
    }

    @Test
    public void insertNotification() throws Exception {
        String notificationId = RandomStringGenerator.getInstance().getNextRandomNotificationId();
        long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
        long date = currentTime + ONE_WEEK;
        Prospect person = Prospect.Factory.createFromId(PERSON_ID);
        Notification notification = new Notification(notificationId, USER_ID, person,
                "Call him asap! Rescheduled stuff", date, currentTime, false,
                ProspectState.NOT_CONTACTED);
        notification = notificationDBAccessor.insertNotification(notification);
        assertNotNull(notification);
    }

    @Test
    public void setNotificationMessage() throws Exception {

    }

    @Test
    public void setReadNotification() throws Exception {

    }

    @Test
    public void deleteNotification() throws Exception {

    }

    @Test
    public void getCurrentNotifications() throws Exception {

    }

    @Test
    public void getUpcomingNotifications() throws Exception {

    }

    @Test
    public void getPastNotifications() throws Exception {

    }

    @Test
    public void getPersonUpcomingNotifications() throws Exception {

    }

    @Test
    public void getPersonPastNotifications() throws Exception {

    }

}