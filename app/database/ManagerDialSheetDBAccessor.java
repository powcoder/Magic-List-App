https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.dialsheet.DialSheet;
import model.dialsheet.ManagerDialSheet;
import model.manager.Employee;
import model.dialsheet.EmployeeDialSheet;
import play.db.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static database.TablesContract._DialSheet.*;
import static database.TablesContract._Manages.*;
import static database.TablesContract._User.*;

/**
 * Created by Corey on 6/11/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerDialSheetDBAccessor extends DialSheetDBAccessor {

    public ManagerDialSheetDBAccessor(Database database) {
        super(database);
    }

    public List<ManagerDialSheet> getTeamDialSheets(String managerId, Date monthToView) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT dial_sheet.sheet_id, dial_sheet.dial_date, sum(number_of_dials) AS dials_count, " +
                    "  sum(activity_count) AS " + ACTIVITY_COUNT + ", " +
                    "  sum(contacts_count) AS " + CONTACTS_COUNT + ", " +
                    "  sum(appointments_count) AS " + APPOINTMENTS_COUNT + " " +
                    "FROM dial_sheet " +
                    "  LEFT JOIN appointment_counts ON dial_sheet.sheet_id = appointment_counts.sheet_id " +
                    "  LEFT JOIN contact_counts ON dial_sheet.sheet_id = contact_counts.sheet_id " +
                    "  LEFT JOIN activity_counts ON dial_sheet.sheet_id = activity_counts.sheet_id " +
                    "  JOIN manages ON employee_id = dial_sheet.user_id " +
                    "WHERE date_trunc(\'month\',  dial_sheet.dial_date) = date_trunc(\'month\', ? :: DATE) " +
                    "AND manager_id = ? " +
                    "GROUP BY dial_sheet.sheet_id, dial_sheet.dial_date, number_of_dials " +
                    "ORDER BY " + DIAL_DATE + " ASC ";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDate(1, new java.sql.Date(monthToView.getTime()));
            statement.setString(2, managerId);

            List<ManagerDialSheet> managerDialSheets = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long date = resultSet.getDate(DIAL_DATE).getTime();
                int dialCount = resultSet.getInt(DIALS_COUNT);
                int contactCount = resultSet.getInt(CONTACTS_COUNT);
                int appointmentCount = resultSet.getInt(APPOINTMENTS_COUNT);

                managerDialSheets.add(new ManagerDialSheet(date, dialCount, contactCount, appointmentCount));
            }

            return managerDialSheets;
        });
    }

    public List<EmployeeDialSheet> getTeamDialSheetsForDate(String managerId, Date date) {
        return getDatabase().withConnection(connection -> {
            String extraSql = ", " + USER_ID + ", " + EMAIL + ", " + TEAM_JOIN_DATE;
            String sql = getProjectionWithTablesForDialSheet(null) + " " +
                    "JOIN " + MANAGES + " ON " + EMPLOYEE_ID + " = " + DIAL_SHEET + "." + USER_ID + " " +
                    "JOIN " + USER + " ON " + DIAL_SHEET + "." + USER_ID + " = " + USER + "." + USER_ID + " " +
                    "WHERE " + DIAL_SHEET + "." + DIAL_DATE + " = ? AND " + MANAGER_ID + " = ? " +
                    "ORDER BY " + NAME + " ASC, " + EMAIL + " ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setDate(1, new java.sql.Date(date.getTime()));
            statement.setString(2, managerId);

            List<EmployeeDialSheet> employeeDialSheets = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                DialSheet dialSheet = DialSheetDBAccessor.getDialSheetFromResultSet(resultSet);

                String userId = resultSet.getString(USER_ID);
                String name = resultSet.getString(NAME);
                String email = resultSet.getString(EMAIL);
                long teamJoinDate = resultSet.getDate(TEAM_JOIN_DATE).getTime();
                long magicListJoinDate = resultSet.getDate(JOIN_DATE).getTime();
                Employee employee = new Employee(userId, name, email, teamJoinDate, magicListJoinDate, managerId);

                employeeDialSheets.add(new EmployeeDialSheet(dialSheet, employee));
            }

            return employeeDialSheets;
        });
    }

}
