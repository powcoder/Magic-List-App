https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.manager;

import model.user.User;
import utilities.DateUtility;

public class EmployeeStatistic {

    private final User employee;
    private final BaseStatistic lifetimeStatistic;
    private final BaseStatistic periodStatistic;

    public EmployeeStatistic(User employee, BaseStatistic lifetimeStatistic, BaseStatistic periodStatistic) {
        this.employee = employee;
        this.lifetimeStatistic = lifetimeStatistic;
        this.periodStatistic = periodStatistic;
    }

    public static class BaseStatistic {

        private final int numberOfDials;
        private final int numberOfContacts;
        private final int numberOfAppointments;
        private final long periodStartDate;
        private final long periodEndDate;

        public BaseStatistic(int numberOfDials, int numberOfContacts, int numberOfAppointments, long periodStartDate,
                             long periodEndDate) {
            this.numberOfDials = numberOfDials;
            this.numberOfContacts = numberOfContacts;
            this.numberOfAppointments = numberOfAppointments;
            this.periodStartDate = periodStartDate;
            this.periodEndDate = periodEndDate;
        }

        public int getNumberOfDials() {
            return numberOfDials;
        }

        public int getNumberOfContacts() {
            return numberOfContacts;
        }

        public int getNumberOfAppointments() {
            return numberOfAppointments;
        }

        public String getPeriodStartDateForUi() {
            return DateUtility.getLongDateForUi(periodStartDate);
        }

        public String getPeriodEndDateForUi() {
            return DateUtility.getLongDateForUi(periodEndDate);
        }

        public double getDialsToContactRatio() {
            return (double) numberOfContacts / numberOfDials;
        }

        public double getContactsToAppointmentRatio() {
            return (double) numberOfAppointments / numberOfContacts;
        }

    }

}
