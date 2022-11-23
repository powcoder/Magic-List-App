https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.profile;

import model.PagedList;
import model.prospect.Appointment;
import model.prospect.Notification;

import java.util.List;

/**
 *
 */
public class HomePageAgenda {

    private final ProfileAlert alert;
    private final List<Appointment> appointmentsForToday;
    private final PagedList<Notification> notificationsForToday;

    public HomePageAgenda(ProfileAlert alert, List<Appointment> appointmentsForToday,
                          PagedList<Notification> notificationsForToday) {
        this.alert = alert;
        this.appointmentsForToday = appointmentsForToday;
        this.notificationsForToday = notificationsForToday;
    }

    public ProfileAlert getAlert() {
        return alert;
    }

    public List<Appointment> getAppointmentsForToday() {
        return appointmentsForToday;
    }

    public PagedList<Notification> getNotificationsForToday() {
        return notificationsForToday;
    }
}
