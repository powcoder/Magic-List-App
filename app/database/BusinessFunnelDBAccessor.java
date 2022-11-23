https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.dialsheet.BusinessFunnelStatistic;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static database.TablesContract._PersonStateType.IS_INVENTORY;
import static database.TablesContract._PersonStateType.IS_OBJECTION;

/**
 * Created by Corey Caplan on 9/13/17.
 */
class BusinessFunnelDBAccessor extends DialSheetDBAccessor {

    private static final String ACTUAL = "actual";
    private static final String ACTUAL_RATIO = "actual_ratio";
    private static final String TARGET = "target";
    private static final String TARGET_RATIO = "target_ratio";

    BusinessFunnelDBAccessor(Database database) {
        super(database);
    }

    // Package-Private Methods

    BusinessFunnelStatistic getContactsStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT sum(contacts_count) AS ACTUAL, " +
                "sum(number_of_dials) :: DECIMAL * 0.10 AS TARGET, " +
                "(sum(contacts_count)::DECIMAL / (CASE sum(number_of_dials) WHEN 0 THEN 1 ELSE sum(number_of_dials) END)::DECIMAL) AS ACTUAL_RATIO, " +
                "0.10 AS TARGET_RATIO " +
                "FROM dial_sheet " +
                "LEFT JOIN contact_counts ON contact_counts.sheet_id = dial_sheet.sheet_id " +
                "WHERE user_id = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));

        return getBusinessFunnelStatisticFromResultSet("Contacts Made", false, statement.executeQuery());
    }

    BusinessFunnelStatistic getFollowUpsStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(person_id) AS actual, 0.25::DECIMAL AS target_ratio, dial_date, is_inventory " +
                "FROM dial_sheet ds_outer " +
                "  LEFT JOIN dial_sheet_contact ON ds_outer.sheet_id = dial_sheet_contact.sheet_id " +
                "  FULL JOIN person_state_type ON contact_status = state_type " +
                "WHERE user_id = ? AND " +
                "   date_trunc('" + truncate.getValue() + "', ds_outer.dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) AND " +
                "   person_id IN (SELECT dial_sheet_contact.person_id " +
                "                    FROM dial_sheet ds " +
                "                      LEFT JOIN dial_sheet_contact ON ds.sheet_id = dial_sheet_contact.sheet_id " +
                "                      JOIN person_state_type ON state_type = contact_status " +
                "                    WHERE ds.user_id = ? AND is_indeterminate = TRUE) " +
                "GROUP BY dial_date, is_inventory;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));
        statement.setString(3, userId);

        ResultSet resultSet = statement.executeQuery();

        String category = "Follow Ups Converted";
        return getGroupedBusinessFunnelStatisticFromResultSet(category, IS_INVENTORY, resultSet);
    }

    BusinessFunnelStatistic getObjectionsStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) AS ACTUAL, (count(*) / coalesce(sum(contacts_count), 1)) AS ACTUAL_RATIO, " +
                "0.50::DECIMAL AS TARGET_RATIO, (sum(contacts_count) * 0.50) AS TARGET " + // be sure to update target ratio in both places
                "FROM dial_sheet " +
                "  LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "  FULL JOIN person_state_type ON contact_status = state_type " +
                "  LEFT JOIN contact_counts ON dial_sheet.sheet_id = contact_counts.sheet_id " +
                "WHERE user_id = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) AND " +
                "is_objection = TRUE";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));

        ResultSet resultSet = statement.executeQuery();
        String category = "Objections Received";
        return getBusinessFunnelStatisticFromResultSet(category, true, resultSet);
    }

    BusinessFunnelStatistic getNewAppointmentsScheduledStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) AS actual, (0.20 * sum(appointments_count)) AS target, " +
                "(count(*) / coalesce(sum(appointments_count), 1) :: DECIMAL) AS actual_ratio, 0.20 AS target_ratio " +
                "FROM dial_sheet " +
                "  LEFT JOIN appointment_counts ON dial_sheet.sheet_id = appointment_counts.sheet_id " +
                "  LEFT JOIN dial_sheet_appointment ON dial_sheet.sheet_id = dial_sheet_appointment.sheet_id " +
                "WHERE dial_sheet.user_id = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) AND " +
                "appointment_type = ? ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));
        statement.setString(3, "introduction");

        String category = "New Appointments Scheduled";
        return getBusinessFunnelStatisticFromResultSet(category, false, statement.executeQuery());
    }

    BusinessFunnelStatistic getNewAppointmentsKeptStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) AS ACTUAL, 0.33 AS TARGET_RATIO, is_inventory " +
                "FROM dial_sheet_appointment " +
                "  JOIN dial_sheet ON dial_sheet_appointment.appointment_date :: DATE = dial_sheet.dial_date " +
                "                     AND dial_sheet_appointment.user_id = dial_sheet.user_id " +
                "  JOIN person_state_type ON dial_sheet_appointment.appointment_outcome = person_state_type.state_type " +
                "WHERE dial_sheet_appointment.user_id = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) AND " +
                "appointment_type = ? " +
                "GROUP BY is_inventory; ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));
        statement.setString(3, "introduction");

        String category = "New Appointments Kept";
        return getGroupedBusinessFunnelStatisticFromResultSet(category, IS_INVENTORY, statement.executeQuery());
    }

    BusinessFunnelStatistic getClosesStatistic(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) AS ACTUAL, is_inventory, 0.25 AS TARGET_RATIO " +
                "FROM dial_sheet_appointment " +
                "  JOIN dial_sheet ON dial_sheet_appointment.appointment_date :: DATE = dial_sheet.dial_date " +
                "                     AND dial_sheet_appointment.user_id = dial_sheet.user_id " +
                "  JOIN person_state_type ON dial_sheet_appointment.appointment_outcome = person_state_type.state_type " +
                "WHERE dial_sheet_appointment.user_id = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) AND " +
                "appointment_type = ? " +
                "GROUP BY is_inventory; ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));
        statement.setString(3, "close");

        String category = "Closes";
        return getGroupedBusinessFunnelStatisticFromResultSet(category, IS_INVENTORY, statement.executeQuery());
    }

    // Private Methods

    @SuppressWarnings("SameParameterValue")
    private static BusinessFunnelStatistic getBusinessFunnelStatisticFromResultSet(String category, boolean isInverse,
                                                                                   ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return new BusinessFunnelStatistic(category, 0, 0, 0, 0, isInverse);
        }

        int actual = resultSet.getInt(ACTUAL);
        double actualRatio = resultSet.getDouble(ACTUAL_RATIO);
        int target = resultSet.getInt(TARGET);
        double targetRatio = resultSet.getDouble(TARGET_RATIO);

        return new BusinessFunnelStatistic(category, actual, target, actualRatio, targetRatio, isInverse);
    }

    @SuppressWarnings("SameParameterValue")
    private static BusinessFunnelStatistic getGroupedBusinessFunnelStatisticFromResultSet(String category, String booleanGrouper,
                                                                                          ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return new BusinessFunnelStatistic(category, 0, 0, 0, 0, false);
        }

        int actual = 0;
        double targetRatio;
        int sum = 0;
        do {
            if (resultSet.getBoolean(booleanGrouper)) {
                actual = resultSet.getInt(ACTUAL);
                sum += actual;
            } else {
                sum += resultSet.getInt(ACTUAL);
            }
            targetRatio = resultSet.getDouble(TARGET_RATIO);

        } while (resultSet.next());

        int target = (int) (sum * targetRatio);
        double actualRatio = (double) actual / sum;

        return new BusinessFunnelStatistic(category, actual, target, actualRatio, targetRatio, false);
    }

}
