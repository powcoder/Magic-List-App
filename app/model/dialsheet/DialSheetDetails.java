https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.dialsheet;

import java.io.Serializable;

/**
 *
 */
public class DialSheetDetails extends HomePageDialSheet implements Serializable {

    private static final long serialVersionUID = 4738294324L;

    private final BusinessFunnelStatistic contactsStatistic;
    private final BusinessFunnelStatistic followUpsStatistic;
    private final BusinessFunnelStatistic objectionsStatistic;
    private final BusinessFunnelStatistic newAppointmentsScheduledStatistic;
    private final BusinessFunnelStatistic newAppointmentsKeptStatistic;
    private final BusinessFunnelStatistic closesStatistic;

    public DialSheetDetails(HomePageDialSheet homePageDialSheet, BusinessFunnelStatistic contactsStatistic,
                            BusinessFunnelStatistic followUpsStatistic,
                            BusinessFunnelStatistic objectionsStatistic,
                            BusinessFunnelStatistic newAppointmentsScheduledStatistic,
                            BusinessFunnelStatistic newAppointmentsKeptStatistic,
                            BusinessFunnelStatistic closesStatistic) {
        super(new DialSheet(homePageDialSheet.getId(), homePageDialSheet.getDialCount(), homePageDialSheet.getDate(),
                        homePageDialSheet.getContactsCount(), homePageDialSheet.getAppointmentsCount()),
                homePageDialSheet.getContactTypeList(), homePageDialSheet.getNewAppointmentsCount(),
                homePageDialSheet.getOtherAppointmentsCount(), homePageDialSheet.getAppointmentList(),
                homePageDialSheet.getDialSheetActivity());
        this.contactsStatistic = contactsStatistic;
        this.followUpsStatistic = followUpsStatistic;
        this.objectionsStatistic = objectionsStatistic;
        this.newAppointmentsScheduledStatistic = newAppointmentsScheduledStatistic;
        this.newAppointmentsKeptStatistic = newAppointmentsKeptStatistic;
        this.closesStatistic = closesStatistic;
    }

    public BusinessFunnelStatistic getContactsStatistic() {
        return contactsStatistic;
    }

    public BusinessFunnelStatistic getFollowUpsStatistic() {
        return followUpsStatistic;
    }

    public BusinessFunnelStatistic getObjectionsStatistic() {
        return objectionsStatistic;
    }

    public BusinessFunnelStatistic getNewAppointmentsScheduledStatistic() {
        return newAppointmentsScheduledStatistic;
    }

    public BusinessFunnelStatistic getNewAppointmentsKeptStatistic() {
        return newAppointmentsKeptStatistic;
    }

    public BusinessFunnelStatistic getClosesStatistic() {
        return closesStatistic;
    }
}
