https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import akka.japi.Pair;
import model.graph.*;
import play.Logger;
import play.db.Database;
import utilities.ListUtility;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static database.TablesContract._DialSheet.*;
import static database.TablesContract._DialSheetContact.*;
import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._PersonStateType.IS_OBJECTION;

/**
 * Created by Corey on 6/26/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerComparisonDBAccessor extends GraphDBAccessor {

    private static final String KEY_ID = "id";

    private static final String APPOINTMENT_SET = "appointment_set";
    private static final String OPEN_INVENTORY = "open_inventory";
    private static final String MISSED_DISCOVERY = "missed_discovery";

    public ManagerComparisonDBAccessor(Database database) {
        super(database);
    }

    public EmployeeLineGraph getContactsToDials(String managerId, GraphSqlGetter sqlGetter) {
        //language=PostgreSQL
        String sql = "WITH coworkers AS (" +
                "    SELECT employee_id FROM manages WHERE manager_id = ? " +
                "), number_of_dials_table AS ( " +
                "    SELECT " +
                "      CASE sum(number_of_dials) " +
                "      WHEN 0 " +
                "        THEN 1.0 :: DECIMAL " +
                "      ELSE sum(number_of_dials) :: DECIMAL END AS data_y, " +
                "      employee_id                              AS id, " +
                "      " + sqlGetter.getSelectionStatement(DIAL_DATE, true) + " " +
                "    FROM dial_sheet " +
                "      LEFT JOIN manages ON employee_id = user_id " +
                "    WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                "       " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "    GROUP BY " + KEY_ID + ", " + KEY_DATA_X + " " +
                "), contact_status_count_table AS ( " +
                "    SELECT " +
                "      COUNT(contact_status) :: DECIMAL AS data_y, " +
                "      manages.employee_id              AS id, " +
                "      " + sqlGetter.getSelectionStatement(DIAL_DATE, true) + " " +
                "    FROM dial_sheet " +
                "      LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "      LEFT JOIN manages ON manages.employee_id = user_id " +
                "    WHERE manages.employee_id IN (SELECT * FROM coworkers) AND " +
                "       " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "    GROUP BY " + KEY_ID + ", " + KEY_DATA_X + " " +
                ") " +
                "SELECT " +
                "  number_of_dials_table." + KEY_ID + ", " +
                "  number_of_dials_table." + KEY_DATA_X + ", " +
                "  contact_status_count_table.data_y / number_of_dials_table.data_y AS data_y " +
                "FROM number_of_dials_table " +
                "  LEFT JOIN contact_status_count_table ON number_of_dials_table.id = contact_status_count_table.id " +
                "   AND number_of_dials_table." + KEY_DATA_X + " = contact_status_count_table." + KEY_DATA_X + " " +
                "ORDER BY id ASC, data_x ASC;";

        String graphTitle = "Contacts to Dials";
        List<Object> parameters = ListUtility.asList(managerId);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        return getDataFromQuery(graphTitle, sql, parameters, ManagerComparisonDBAccessor::getEmployeeLineGraphFromResultSet);
    }

    public EmployeeLineGraph getAppointmentsToContacts(String managerId, GraphSqlGetter sqlGetter) {
        //language=PostgreSQL
        String sql = "WITH coworkers AS (" +
                "SELECT employee_id FROM manages WHERE manager_id = ?" +
                "), " +
                "appointment_counts AS (" +
                "SELECT count(*)::DECIMAL AS " + KEY_DATA_Y + ", " +
                "user_id AS " + KEY_ID + ", " +
                sqlGetter.getSelectionStatement(DIAL_DATE, true) + " " +
                "FROM dial_sheet JOIN dial_sheet_contact on dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "WHERE contact_status = ? " +
                "AND user_id in (select * from coworkers) " +
                "AND " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "GROUP BY " + KEY_ID + ", " + KEY_DATA_X +
                "), " +
                "contact_counts AS (" +
                "SELECT case count(*) WHEN 0 THEN 1.0::DECIMAL " +
                "   ELSE count(*)::DECIMAL END AS " + KEY_DATA_Y + ", " +
                "user_id AS " + KEY_ID + ", " +
                sqlGetter.getSelectionStatement(DIAL_DATE, true) + " " +
                "FROM dial_sheet JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "WHERE user_id in (select * from coworkers) " +
                "AND " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "GROUP BY " + KEY_ID + ", " + KEY_DATA_X + " " +
                ") " +
                "SELECT appointment_counts.data_y / contact_counts.data_y AS " + KEY_DATA_Y + ", " +
                "contact_counts." + KEY_DATA_X + " AS " + KEY_DATA_X + ", " +
                "contact_counts.id AS " + KEY_ID + " " +
                "FROM contact_counts " +
                "LEFT JOIN appointment_counts ON contact_counts.id = appointment_counts.id AND " +
                "appointment_counts." + KEY_DATA_X + " = contact_counts." + KEY_DATA_X + " " +
                "ORDER BY " + KEY_ID + ", " + KEY_DATA_X + " ASC";
        String graphTitle = "Appointments to Contacts";

        List<Object> parameters = ListUtility.asList(managerId, APPOINTMENT_SET);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        return getDataFromQuery(graphTitle, sql, parameters, ManagerComparisonDBAccessor::getEmployeeLineGraphFromResultSet);
    }

    public EmployeeBarGraph getAppointments(String managerId, GraphSqlGetter sqlGetter) {
        String title = "Appointments Scheduled";
        return getEmployeeContactStatus(managerId, sqlGetter, title,
                CONTACT_STATUS + " = ? ", APPOINTMENT_SET);
    }

    public EmployeeBarGraph getMissedDiscoveries(String managerId, GraphSqlGetter sqlGetter) {
        String title = "Missed Discoveries";
        return getEmployeeContactStatus(managerId, sqlGetter, title,
                CONTACT_STATUS + " = ? ", MISSED_DISCOVERY);
    }

    public EmployeeBarGraph getObjections(String managerId, GraphSqlGetter sqlGetter) {
        String title = "Objections";
        return getEmployeeContactStatus(managerId, sqlGetter, title,
                IS_OBJECTION + " = ? ", true);
    }

    public EmployeeBarGraph getLimbosConvertedToAppointments(String managerId, GraphSqlGetter sqlGetter) {
        // language=PostgreSQL
        String sql = "WITH coworkers AS ( " +
                "    SELECT employee_id " +
                "    FROM manages " +
                "    WHERE manager_id = ? " +
                "), " +
                "    limbos_to_objections AS ( " +
                "      SELECT " +
                "        count(*)::DECIMAL    AS " + KEY_DATA + ", " +
                "        employee_id AS " + KEY_ID + ", " +
                "        " + sqlGetter.getSelectionStatementForRange(DIAL_DATE) + " " +
                "      FROM dial_sheet " +
                "        LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "        JOIN manages ON employee_id = user_id " +
                "        JOIN person_state_type ON dial_sheet_contact.contact_status = person_state_type.state_type " +
                "      WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                "            person_state_type.is_indeterminate = TRUE AND " +
                "            dial_sheet_contact.person_id IN (SELECT person_prospect.person_id " +
                "                          FROM person_prospect " +
                "                            JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "                            JOIN person_state_type " +
                "                              ON person_state.state = person_state_type.state_type " +
                "                          WHERE is_objection = TRUE AND " +
                "                                owner_id IN (SELECT * FROM coworkers)) AND " +
                "                            " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "      GROUP BY id, " + KEY_LABEL + " " +
                "  ), " +
                "    limbos_to_appointments AS ( " +
                "      SELECT " +
                "        count(*)::DECIMAL    AS " + KEY_DATA + ", " +
                "        employee_id AS " + KEY_ID + ", " +
                "        " + sqlGetter.getSelectionStatementForRange(DIAL_DATE) + " " +
                "      FROM dial_sheet " +
                "        LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "        JOIN manages ON employee_id = user_id " +
                "        JOIN person_state_type ON dial_sheet_contact.contact_status = person_state_type.state_type " +
                "      WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                "            person_state_type.is_indeterminate = TRUE AND " +
                "            person_id IN (SELECT person_prospect.person_id " +
                "                          FROM person_prospect " +
                "                            JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "                            JOIN person_state_type " +
                "                              ON person_state.state = person_state_type.state_type " +
                "                          WHERE state = ? AND " +
                "                                owner_id IN (SELECT * FROM coworkers)) AND " +
                "                            " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "      GROUP BY id, " + KEY_LABEL + " " +
                "  ) " +
                "SELECT " +
                "  limbos_to_appointments." + KEY_DATA + " / " +
                "(limbos_to_appointments." + KEY_DATA + " + limbos_to_objections." + KEY_DATA + ") AS " + KEY_DATA + ", " +
                "  limbos_to_objections." + KEY_ID + " AS " + KEY_ID + ", " +
                "  limbos_to_objections." + KEY_LABEL + " AS " + KEY_LABEL + " " +
                "FROM limbos_to_objections " +
                "  JOIN limbos_to_appointments ON limbos_to_objections.id = limbos_to_appointments.id " +
                "       AND limbos_to_appointments." + KEY_LABEL + " = limbos_to_objections." + KEY_LABEL + ";";

        List<Object> parameters = ListUtility.asList(managerId);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);
        parameters.add(APPOINTMENT_SET);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        String title = "% Callbacks Converted to Appointments";

        return getDataFromQuery(title, sql, parameters, ManagerComparisonDBAccessor::getEmployeeBarGraphFromResultSet);
    }

    public EmployeeBarGraph getAppointmentsConvertedToOpenInventory(String managerId, GraphSqlGetter sqlGetter) {
        // language=PostgreSQL
        String sql = "WITH coworkers AS ( " +
                "    SELECT employee_id " +
                "    FROM manages " +
                "    WHERE manager_id = ? " +
                "), " +
                "    appointments_to_objections AS ( " +
                "      SELECT " +
                "        count(*)::DECIMAL    AS data, " +
                "        employee_id AS id, " +
                "        " + sqlGetter.getSelectionStatementForRange(DIAL_DATE) + " " +
                "      FROM dial_sheet " +
                "        LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "        JOIN manages ON employee_id = user_id " +
                "        JOIN person_state_type ON dial_sheet_contact.contact_status = person_state_type.state_type " +
                "      WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                "            contact_status = ? AND " +
                "            person_id IN (SELECT person_prospect.person_id " +
                "                          FROM person_prospect " +
                "                            JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "                            JOIN person_state_type " +
                "                              ON person_state.state = person_state_type.state_type " +
                "                          WHERE person_state_type.is_objection = TRUE AND " +
                "                               owner_id IN (SELECT * FROM coworkers) " +
                "                          ) AND " +
                "        " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "      GROUP BY id, " + KEY_LABEL + " " +
                "  ), " +
                "    appointments_to_open_inventory AS ( " +
                "      SELECT " +
                "        count(*)::DECIMAL    AS data, " +
                "        employee_id          AS id, " +
                "        " + sqlGetter.getSelectionStatementForRange(DIAL_DATE) + " " +
                "      FROM dial_sheet " +
                "        LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "        JOIN manages ON employee_id = user_id " +
                "        FULL JOIN person_state_type ON dial_sheet_contact.contact_status = person_state_type.state_type " +
                "      WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                "            contact_status = ? AND " +
                "            person_id IN (SELECT person_prospect.person_id " +
                "                          FROM person_prospect " +
                "                            JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "                            JOIN person_state_type " +
                "                              ON person_state.state = person_state_type.state_type " +
                "                          WHERE state = ? AND owner_id IN (SELECT * FROM coworkers) " +
                "           ) " +
                "      AND " + sqlGetter.getPredicateStatement(DIAL_DATE) +
                "      GROUP BY id, " + KEY_LABEL + " " +
                "  )" +
                "SELECT " +
                "  appointments_to_open_inventory.data / " +
                "  (appointments_to_open_inventory.data + appointments_to_objections.data) AS data, " +
                "  appointments_to_objections.id AS id, " +
                "  appointments_to_objections." + KEY_LABEL + " AS " + KEY_LABEL + " " +
                "FROM appointments_to_objections " +
                "  JOIN appointments_to_open_inventory ON appointments_to_objections.id = appointments_to_open_inventory.id " +
                "       AND appointments_to_objections." + KEY_LABEL + " = appointments_to_open_inventory." + KEY_LABEL + ";";

        List<Object> parameters = ListUtility.asList(managerId, APPOINTMENT_SET);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);
        parameters.addAll(ListUtility.asList(APPOINTMENT_SET, OPEN_INVENTORY));
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        String title = "% Appointments Converted to Open Inventory";

        return getDataFromQuery(title, sql, parameters, ManagerComparisonDBAccessor::getEmployeeBarGraphFromResultSet);
    }

    public CategorizedBarGraph getAppointmentsByCompany(String managerId, GraphSqlGetter sqlGetter) {
        // language=PostgreSQL
        String sql = "WITH coworkers AS ( " +
                "    SELECT employee_id " +
                "    FROM manages " +
                "    WHERE manager_id = ? " +
                ") " +
                "SELECT " +
                "  count(*)                         AS data, " +
                "  trim(BOTH ' ' FROM person_company_name) AS label " +
                "FROM manages " +
                "  JOIN dial_sheet_appointment ON employee_id = dial_sheet_appointment.user_id " +
                "  JOIN dial_sheet ON dial_sheet_appointment.sheet_id = dial_sheet.sheet_id " +
                "  JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                "WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "GROUP BY label " +
                "ORDER BY data DESC " +
                "LIMIT 25;";

        String title = "Appointments By Company";

        List<Object> parameters = ListUtility.asList(managerId);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        return getDataFromQuery(title, sql, parameters, ManagerComparisonDBAccessor::getCategorizedBarGraphFromResultSet)
                .setXAxisTitle("Company");
    }

    public CategorizedBarGraph getAppointmentsByAreaCode(String managerId, GraphSqlGetter sqlGetter) {
        // language=PostgreSQL
        String sql = "WITH coworkers AS ( " +
                "    SELECT employee_id " +
                "    FROM manages " +
                "    WHERE manager_id = ? " +
                ") " +
                "SELECT " +
                "  count(*)                         AS data, " +
                "  '(' || substring(person_phone, '(\\d{3})') || ')' AS label " +
                "FROM manages " +
                "  JOIN dial_sheet_appointment ON employee_id = dial_sheet_appointment.user_id " +
                "  JOIN dial_sheet ON dial_sheet_appointment.sheet_id = dial_sheet.sheet_id " +
                "  JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                "WHERE employee_id IN (SELECT * FROM coworkers) AND " +
                sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "GROUP BY label " +
                "ORDER BY data DESC " +
                "LIMIT 25;";

        String title = "Appointments By Area Code";

        List<Object> parameters = ListUtility.asList(managerId);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        return getDataFromQuery(title, sql, parameters, ManagerComparisonDBAccessor::getCategorizedBarGraphFromResultSet)
                .setXAxisTitle("Area Code");
    }

    public EmployeeBarGraph getUploadedContacts(String managerId, GraphSqlGetter sqlGetter) {
        // language=PostgreSQL
        String sql = "SELECT count(*) AS " + KEY_DATA + ", " +
                "employee_id AS " + KEY_ID + ", " +
                sqlGetter.getSelectionStatement(INSERT_DATE, false) + " " +
                "FROM person_prospect JOIN manages ON owner_id = employee_id " +
                "WHERE employee_id IN (select employee_id FROM manages where manager_id = ?) AND " +
                sqlGetter.getPredicateStatement(INSERT_DATE) + " " +
                "GROUP BY " + KEY_ID + ", " + KEY_LABEL + ";";

        List<Object> parameters = ListUtility.asList(managerId);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        String title = "Contacts Uploaded";

        return getDataFromQuery(title, sql, parameters, ManagerComparisonDBAccessor::getEmployeeBarGraphFromResultSet);
    }

    // MARK - Private Methods

    private EmployeeBarGraph getEmployeeContactStatus(String managerId, GraphSqlGetter sqlGetter,
                                                      String graphTitle, String contactStatusPredicate,
                                                      Object parameter) {
        //language=PostgreSQL
        String sql = "SELECT count(*) AS " + KEY_DATA + ", " +
                sqlGetter.getSelectionStatement(DIAL_DATE, false) + ", " +
                "employee_id AS " + KEY_ID + " " +
                "FROM dial_sheet " +
                "LEFT JOIN dial_sheet_contact ON dial_sheet.sheet_id = dial_sheet_contact.sheet_id " +
                "JOIN person_state_type ON contact_status = state_type " +
                "LEFT JOIN manages on employee_id = user_id " +
                "WHERE employee_id IN (select employee_id FROM manages where manager_id = ?) " +
                "AND " + contactStatusPredicate + " " +
                "AND " + sqlGetter.getPredicateStatement(DIAL_DATE) + " " +
                "GROUP BY " + KEY_LABEL + ", " + KEY_ID + " " +
                "ORDER BY " + KEY_LABEL + " ASC";
        List<Object> parameters = ListUtility.asList(managerId, parameter);
        sqlGetter.addParametersForCustomDateIfNecessary(parameters);

        return getDataFromQuery(graphTitle, sql, parameters, ManagerComparisonDBAccessor::getEmployeeBarGraphFromResultSet);
    }

    private static EmployeeLineGraph getEmployeeLineGraphFromResultSet(String graphTitle, ResultSet resultSet) throws SQLException {
        Map<String, List<Pair<Long, Double>>> employeeMap = new HashMap<>();

        while (resultSet.next()) {
            String id = resultSet.getString(KEY_ID);
            long x = resultSet.getLong(KEY_DATA_X);
            double y = resultSet.getDouble(KEY_DATA_Y);

            Logger.trace(String.format("Results for %s: {id: %s, x: %d y: %f}", graphTitle, id, x, y));

            List<Pair<Long, Double>> employeeData;
            if (employeeMap.containsKey(id)) {
                employeeData = employeeMap.get(id);
            } else {
                employeeData = new ArrayList<>();
                employeeMap.put(id, employeeData);
            }
            employeeData.add(new Pair<>(x, y));
        }

        List<EmployeeLineGraph.Data> employeeDataList = employeeMap.entrySet().stream()
                .map(entry -> new EmployeeLineGraph.Data(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new EmployeeLineGraph(graphTitle, employeeDataList);
    }

    private static EmployeeBarGraph getEmployeeBarGraphFromResultSet(String graphTitle, ResultSet resultSet) throws SQLException {
        Map<String, Map<String, Double>> labelToEmployeeToDataMap = new HashMap<>();
        Set<String> employeeIdSet = new HashSet<>();

        while (resultSet.next()) {
            String id = resultSet.getString(KEY_ID);
            String label = resultSet.getString(KEY_LABEL);
            Double data = resultSet.getDouble(KEY_DATA);

            if (!employeeIdSet.contains(id)) {
                employeeIdSet.add(id);
            }

            Map<String, Double> employeeIdToDataMap;
            if (!labelToEmployeeToDataMap.containsKey(label)) {
                employeeIdToDataMap = new HashMap<>();
                labelToEmployeeToDataMap.put(label, employeeIdToDataMap);
            } else {
                employeeIdToDataMap = labelToEmployeeToDataMap.get(label);
            }

            employeeIdToDataMap.put(id, data);
        }

        List<EmployeeBarGraph.Data> employeeDataList = labelToEmployeeToDataMap.entrySet().stream()
                .map(entry -> new EmployeeBarGraph.Data(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<String> employeeIdList = new ArrayList<>(employeeIdSet);

        return new EmployeeBarGraph(graphTitle, employeeDataList, employeeIdList);
    }

    private static EmployeeDoughnutGraph getEmployeeDoughnutGraphFromResultSet(String graphTitle, ResultSet resultSet) throws SQLException {
        List<Pair<String, Double>> employeeDataList = new ArrayList<>();

        while (resultSet.next()) {
            String id = resultSet.getString(KEY_ID);
            double data = resultSet.getDouble(KEY_DATA);

            employeeDataList.add(new Pair<>(id, data));
        }

        return new EmployeeDoughnutGraph(graphTitle, employeeDataList);
    }

}
