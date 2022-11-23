https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import cache.DialSheetCache;
import model.PagedList;
import model.prospect.Prospect;
import model.dialsheet.*;
import play.Logger;
import play.db.Database;
import utilities.RandomStringGenerator;

import java.sql.*;
import java.text.DateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import static database.TablesContract._DialSheet.*;
import static database.TablesContract._DialSheet.SHEET_ID;
import static database.TablesContract._DialSheetContact.*;
import static database.TablesContract._User.*;
import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._PersonStateType.*;

public class DialSheetDBAccessor extends AbstractPersonDatabase {

    private static final int AMOUNT_PER_PAGE = 25;

    private final DialSheetAppointmentDBAccessor dialSheetAppointmentDBAccessor;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    public DialSheetDBAccessor(Database database) {
        super(database);
        dialSheetAppointmentDBAccessor = new DialSheetAppointmentDBAccessor(database);
    }

    public boolean changeCallCount(String userId, String sheetId, boolean shouldIncrement) {
        return changeCallCount(userId, sheetId, -1, shouldIncrement);
    }

    public boolean changeCallCount(String userId, String sheetId, int amount) {
        return changeCallCount(userId, sheetId, amount, false);
    }

    public PagedList<DialSheet> getPastDialSheetsWithoutUpdatedDialCounts(String userId, int currentPage) {
        return getDatabase().withConnection(connection -> {
            String sql = getProjectionWithTablesForDialSheet(", count(*) OVER() AS TOTAL_COUNT ") + " " +
                    "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? AND " +
                    NUMBER_OF_DIALS + " = ? AND " +
                    CONTACTS_COUNT + " > ? " +
                    "ORDER BY " + DIAL_SHEET + "." + DIAL_DATE + " DESC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setInt(2, 0);
            statement.setInt(3, 0);

            ResultSet resultSet = statement.executeQuery();
            PagedList<DialSheet> dialSheets = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
            dialSheets.setTotalNumberOfItems(getPastDialSheetsWithoutUpdatedDialsCount(connection, userId));
            while (resultSet.next()) {
                dialSheets.add(getDialSheetFromResultSet(resultSet));
            }
            return dialSheets;
        });
    }

    public int getPastDialSheetsWithoutUpdatedDialsCount(String userId) {
        return getDatabase().withConnection(connection -> {
            return getPastDialSheetsWithoutUpdatedDialsCount(connection, userId);
        });
    }

    public List<DialSheetDates> getPastDialSheetMonthsAndYears(String userId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT date_trunc('month', dial_date) monthly_dials " +
                    "FROM dial_sheet " +
                    "WHERE user_id = ? " +
                    "GROUP BY monthly_dials " +
                    "ORDER BY monthly_dials ASC;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            ResultSet resultSet = statement.executeQuery();

            Map<Integer, List<Date>> yearToDialSheetDatesMap = new HashMap<>();
            while (resultSet.next()) {
                Calendar dialSheetDate = Calendar.getInstance();
                dialSheetDate.setTimeInMillis(resultSet.getDate(1).getTime());

                List<Date> dialSheetDates;
                int year = dialSheetDate.get(Calendar.YEAR);
                if (yearToDialSheetDatesMap.containsKey(year)) {
                    dialSheetDates = yearToDialSheetDatesMap.get(year);
                } else {
                    dialSheetDates = new ArrayList<>();
                    yearToDialSheetDatesMap.put(year, dialSheetDates);
                }

                dialSheetDates.add(new Date(dialSheetDate.getTimeInMillis()));
            }


            Set<Integer> years = yearToDialSheetDatesMap.keySet().stream().sorted(Integer::compareTo).collect(Collectors.toSet());
            return years.stream()
                    .map(year -> new DialSheetDates(year, yearToDialSheetDatesMap.get(year)))
                    .collect(Collectors.toList());
        });
    }

    public List<DialSheet> getPastDialSheets(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            String sql = getProjectionWithTablesForDialSheet(null) + " " +
                    "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? AND " +
                    "date_trunc(\'month\', " + DIAL_SHEET + "." + DIAL_DATE + ") = date_trunc(\'month\', ?::DATE) " +
                    "ORDER BY " + DIAL_DATE + " ASC ";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            ResultSet resultSet = statement.executeQuery();

            List<DialSheet> dialSheetList = new ArrayList<>();
            while (resultSet.next()) {
                dialSheetList.add(getDialSheetFromResultSet(resultSet));
            }
            return dialSheetList;
        });
    }

    public AllPagesDialSheet getCurrentAllPagesDialSheet(String userId) {
        AllPagesDialSheet cachedDialSheet = DialSheetCache.getCurrentDialSheet(userId);
        if (cachedDialSheet != null) {
            return cachedDialSheet;
        }

        return getDatabase().withConnection(connection -> {
            PreparedStatement statement;
            ResultSet resultSet;

            String sql = getProjectionWithTablesForDialSheet(null) + " " +
                    "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? " +
                    "AND " + DIAL_SHEET + "." + DIAL_DATE + " = current_date";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                String sheetId = createDialSheetForToday(connection, userId, new Date());
                if (sheetId != null) {
                    // close all connections now to avoid double connection opening
                    closeConnections(connection, statement, resultSet);
                    return getCurrentAllPagesDialSheet(userId);
                } else {
                    // There was an error creating the dial sheet
                    return null;
                }
            }

            DialSheet dialSheet = getDialSheetFromResultSet(resultSet);

            sql = getSqlQueryForContactStatusType() + " " +
                    "WHERE " + USER_ID + " = ? AND " + SHEET_ID + " = ? " +
                    "ORDER BY " + _INDEX + " ASC;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, dialSheet.getId()); // The first index is in the SQL Query method
            statement.setString(2, userId);
            statement.setString(3, dialSheet.getId());

            resultSet = statement.executeQuery();
            List<DialSheetContactType> contactTypeList = getContactTypesFromResultSet(resultSet);

            List<DialSheetAppointment> appointmentList =
                    dialSheetAppointmentDBAccessor.getDialSheetAppointmentsForCurrentDialSheet(connection, userId);

            AllPagesDialSheet allPagesDialSheet = new AllPagesDialSheet(dialSheet, contactTypeList, appointmentList);

            // Save the dial sheet in the cache
            DialSheetCache.setCurrentDialSheet(userId, allPagesDialSheet);

            return allPagesDialSheet;
        });
    }

    public HomePageDialSheet getHomePageDialSheet(String userId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT sheet_id FROM dial_sheet WHERE dial_date = current_date AND user_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            ResultSet resultSet = statement.executeQuery();

            String sheetId;
            if (!resultSet.next()) {
                // The dial sheet likely does not exist yet
                sheetId = createDialSheetForToday(connection, userId, new Date());
            } else {
                sheetId = resultSet.getString(SHEET_ID);
            }

            if (sheetId == null) {
                return null;
            }

            return getHomePageDialSheet(connection, userId, sheetId);
        });
    }

    public DialSheet getDialSheetById(String userId, String sheetId) {
        return getDatabase().withConnection(connection -> {
            String sql = getProjectionWithTablesForDialSheet(null) + " " +
                    "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? AND " + DIAL_SHEET + "." + SHEET_ID + " = ? ";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, sheetId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getDialSheetFromResultSet(resultSet);
            } else {
                return null;
            }
        });
    }

    public DialSheetDetails getDailyDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.DAY);
        });
    }

    public DialSheetDetails getWeeklyDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.WEEK);
        });
    }

    public DialSheetDetails getMonthlyDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.MONTH);
        });
    }

    public DialSheetDetails getQuarterlyDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.QUARTER);
        });
    }

    public DialSheetDetails getYearlyDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.YEAR);
        });
    }

    public DialSheetDetails getAllTimeDialSheetDetails(String userId, Date date) {
        return getDatabase().withConnection(connection -> {
            return getDialSheetDetails(userId, date, DateTruncateValue.CENTURY);
        });
    }

    public DialSheetPaginationSummary getNextDialSheet(String userId, String currentDialSheetId) {
        return getDialSheetPaginationSummary(true, userId, currentDialSheetId);
    }

    public DialSheetPaginationSummary getPreviousDialSheet(String userId, String currentDialSheetId) {
        return getDialSheetPaginationSummary(false, userId, currentDialSheetId);
    }

//    MARK - Package-Private Methods

    /**
     * @return A SQL query with the sheet ID and appointment count as the projection, and user_id as the only
     * parameter in the predicate
     */
    static String getProjectionWithTablesForDialSheet(String sqlToAppendToSelect) {
        sqlToAppendToSelect = Optional.ofNullable(sqlToAppendToSelect)
                .orElse("");
        //language=PostgreSQL
        return "SELECT  dial_sheet.sheet_id AS sheet_id, " +
                "  dial_sheet.dial_date AS dial_date, " +
                "  number_of_dials, " +
                "  coalesce(contacts_count, 0) AS " + CONTACTS_COUNT + ", " +
                "  coalesce(appointments_count, 0) AS " + APPOINTMENTS_COUNT + ", " +
                "  coalesce(activity_count, 0) AS " + ACTIVITY_COUNT + " " +
                "  " + sqlToAppendToSelect + " " +
                "FROM dial_sheet " +
                "  LEFT JOIN appointment_counts ON dial_sheet.sheet_id = appointment_counts.sheet_id " +
                "  LEFT JOIN contact_counts ON dial_sheet.sheet_id = contact_counts.sheet_id " +
                "  LEFT JOIN activity_counts ON dial_sheet.sheet_id = activity_counts.sheet_id ";
    }

    static DialSheet getDialSheetFromResultSet(ResultSet resultSet) throws SQLException {
        String sheetId = null;
        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
            if (resultSet.getMetaData().getColumnName(i).equals(SHEET_ID)) {
                sheetId = resultSet.getString(SHEET_ID);
            }
        }
        Date dialDate = resultSet.getDate(DIAL_DATE);
        int numberOfDials = resultSet.getInt(NUMBER_OF_DIALS);
        int contactsCount = resultSet.getInt(CONTACTS_COUNT);
        int appointmentsCount = resultSet.getInt(APPOINTMENTS_COUNT);
        return new DialSheet(sheetId, numberOfDials, dialDate.getTime(), contactsCount, appointmentsCount);
    }

    DialSheet getDialSheetByDateAndCreateIfNotExists(Connection connection, String userId, Date date) throws SQLException {
        String sql = getProjectionWithTablesForDialSheet(null) + " " +
                "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? AND " +
                DIAL_SHEET + "." + DIAL_DATE + " = ? :: DATE ";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));

        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            if (createDialSheetForToday(connection, userId, date) == null) {
                logger.error("Could not create dial sheet");
                return null;
            } else {
                return getDialSheetByDateAndCreateIfNotExists(connection, userId, date);
            }
        } else {
            return getDialSheetFromResultSet(resultSet);
        }
    }

//    MARK - Private Methods

    private int getPastDialSheetsWithoutUpdatedDialsCount(Connection connection, String userId) throws SQLException {
        String sql = "SELECT count(*) " +
                "FROM dial_sheet " +
                "LEFT JOIN contact_counts ON dial_sheet.sheet_id = contact_counts.sheet_id " +
                "WHERE user_id = ? AND contacts_count > ? AND number_of_dials = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setInt(2, 0);
        statement.setInt(3, 0);

        ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? resultSet.getInt(1) : 0;
    }

    /**
     * @param amount          The amount that the number of dials should be changed to or -1 if the "shouldIncrement" variable
     *                        should be used instead.
     * @param shouldIncrement True to increment the call count, false to decrement it. This value is ignored if the
     *                        "amount" parameter is not equal to -1.
     */
    private boolean changeCallCount(String userId, String sheetId, int amount, boolean shouldIncrement) {
        return getDatabase().withConnection(connection -> {
            String sql;
            if (amount != -1) {
                sql = "UPDATE dial_sheet SET number_of_dials = ? " +
                        "WHERE user_id = ? AND sheet_id = ?";
            } else if (shouldIncrement) {
                sql = "UPDATE dial_sheet SET number_of_dials = number_of_dials + 1 " +
                        "WHERE user_id = ? AND sheet_id = ?";
            } else {
                sql = "UPDATE dial_sheet SET number_of_dials = number_of_dials - 1 " +
                        "WHERE user_id = ? AND sheet_id = ?";
            }
            PreparedStatement statement = connection.prepareStatement(sql);

            int counter = 1;
            if (amount != -1) {
                statement.setInt(counter++, amount);
            }
            statement.setString(counter++, userId);
            statement.setString(counter, sheetId);

            DialSheetCache.removeCurrentDialSheet(userId);

            return statement.executeUpdate() == 1;
        });
    }

    private static String getSqlQueryForContactStatusType() {
        // language=PostgreSQL
        return "SELECT state_type AS contact_status, state_name, state_class, 0 AS " + STATUS_COUNT + ", " +
                "is_indeterminate, is_objection, is_inventory, _index " +
                "FROM person_state_type " +
                "WHERE is_dial_sheet_state = TRUE " +
                "   AND state_type NOT IN (SELECT contact_status FROM contact_status_type WHERE sheet_id = ?) " +
                "UNION DISTINCT " +
                "SELECT contact_status, state_name, state_class, status_count, " +
                "is_indeterminate, is_objection, is_inventory, _index " +
                "FROM contact_status_type ";
    }

    private String createDialSheetForToday(Connection connection, String userId, Date today) throws SQLException {
        String sheetId = RandomStringGenerator.getInstance().getNextRandomDialSheetId();
        String sql = "INSERT INTO dial_sheet(sheet_id, user_id, number_of_dials, dial_date) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, sheetId);
        statement.setString(2, userId);
        statement.setInt(3, 0);
        statement.setDate(4, new java.sql.Date(today.getTime()));
        if (statement.executeUpdate() == 1) {
            return sheetId;
        } else {
            return null;
        }
    }

    private DialSheetPaginationSummary getDialSheetPaginationSummary(boolean isNext, String userId,
                                                                     String currentDialSheetId) {
        return getDatabase().withConnection(connection -> {
            String sql;
            if (isNext) {
                sql = "SELECT sheet_id, dial_date " +
                        "FROM dial_sheet " +
                        "WHERE user_id = ? AND dial_date > (" +
                        "   SELECT dial_date FROM dial_sheet WHERE user_id = ? AND sheet_id = ?" +
                        ") " +
                        "ORDER BY dial_date ASC LIMIT 1;";
            } else {
                sql = "SELECT sheet_id, dial_date " +
                        "FROM dial_sheet " +
                        "WHERE user_id = ? AND dial_date <  (" +
                        "   SELECT dial_date FROM dial_sheet WHERE user_id = ? AND sheet_id = ?" +
                        ") " +
                        "ORDER BY dial_date DESC LIMIT 1;";
            }

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, userId);
            statement.setString(3, currentDialSheetId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getDialSheetPaginationFromResultSet(resultSet);
            } else {
                return null;
            }
        });
    }

    private HomePageDialSheet getHomePageDialSheet(Connection connection, String userId, String sheetId) throws SQLException {
        PreparedStatement statement;
        ResultSet resultSet;

        // General Info
        String sql = getProjectionWithTablesForDialSheet(null) + " " +
                "WHERE " + DIAL_SHEET + "." + USER_ID + " = ? " +
                "AND " + DIAL_SHEET + "." + SHEET_ID + " = ?;";
        statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, sheetId);
        resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            logger.error("Could not call ResultSet#next for user {}", userId);
            logger.error("Error: ", new IllegalStateException());
            return null;
        }

        DialSheet dialSheet = getDialSheetFromResultSet(resultSet);

        // Contact Types
        sql = getSqlQueryForContactStatusType() + " " +
                "WHERE " + USER_ID + " = ? AND " + SHEET_ID + " = ? " +
                "ORDER BY " + _INDEX + " ASC;";
        statement = connection.prepareStatement(sql);
        statement.setString(1, sheetId); // The first index is in the SQL Query method
        statement.setString(2, userId);
        statement.setString(3, sheetId);

        resultSet = statement.executeQuery();
        List<DialSheetContactType> contactTypeList = getContactTypesFromResultSet(resultSet);

        List<DialSheetAppointment> appointmentList =
                dialSheetAppointmentDBAccessor.getAppointmentsFromDialSheet(connection, userId, sheetId);

        String personIdTablePrefix = PERSON_PROSPECT + ".";

        // All Activity
        sql = "SELECT " + getSelectionForPerson(personIdTablePrefix, null, false) +
                ", " + CONTACT_TIME + " " +
                "FROM dial_sheet_contact " +
                "JOIN person_prospect ON dial_sheet_contact.person_id = person_prospect.person_id " +
                "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "JOIN person_state_type ON contact_status = person_state_type.state_type " +
                "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                "WHERE sheet_id = ? AND (receiver_id = ? OR owner_id = ?) " +
                "GROUP BY " + CONTACT_TIME + ", " +
                getSelectionForPerson(personIdTablePrefix, null, true) + " " +
                "ORDER BY contact_time ASC ";
        statement = connection.prepareStatement(sql);
        statement.setString(1, sheetId);
        statement.setString(2, userId);
        statement.setString(3, userId);
        resultSet = statement.executeQuery();

        List<DialSheetContact> contactsList = new ArrayList<>();
        while (resultSet.next()) {
            Prospect prospect = getPersonFromResultSet(resultSet, personIdTablePrefix, null);
            long contactTime = resultSet.getTimestamp(CONTACT_TIME).getTime();
            contactsList.add(new DialSheetContact(prospect, contactTime));
        }

        int newAppointmentsCount;
        sql = "SELECT count(*) FROM dial_sheet_appointment " +
                "LEFT JOIN person_state_type ON dial_sheet_appointment.appointment_type = person_state_type.state_type " +
                "WHERE user_id = ? AND state_type = ? AND sheet_id = ?;";
        statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, "introduction");
        statement.setString(3, sheetId);
        resultSet = statement.executeQuery();
        if (resultSet.next()) {
            newAppointmentsCount = resultSet.getInt(1);
        } else {
            return null;
        }

        int otherAppointmentsCount;
        sql = "SELECT count(*) FROM dial_sheet_appointment " +
                "LEFT JOIN person_state_type ON dial_sheet_appointment.appointment_type = person_state_type.state_type " +
                "WHERE user_id = ? AND state_type != ? AND sheet_id = ?;";
        statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, "introduction");
        statement.setString(3, sheetId);
        resultSet = statement.executeQuery();
        if (resultSet.next()) {
            otherAppointmentsCount = resultSet.getInt(1);
        } else {
            return null;
        }

        return new HomePageDialSheet(dialSheet, contactTypeList, newAppointmentsCount, otherAppointmentsCount,
                appointmentList, contactsList);
    }

    private DialSheetDetails getDialSheetDetails(String userId, Date date, DateTruncateValue truncate) {
        return getDatabase().withConnection(connection -> {
            PreparedStatement statement;
            ResultSet resultSet;

            // General Info
            String sql = "SELECT date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date) AS dial_date, " +
                    "sum(number_of_dials) AS number_of_dials, " +
                    "sum(contacts_count) AS contacts_count, " +
                    "sum(appointments_count) AS appointments_count " +
                    "FROM dial_sheet " +
                    "LEFT JOIN contact_counts ON dial_sheet.sheet_id = contact_counts.sheet_id " +
                    "LEFT JOIN appointment_counts ON dial_sheet.sheet_id = appointment_counts.sheet_id " +
                    "WHERE dial_sheet.user_id = ? AND " +
                    "date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date) = date_trunc('" + truncate.getValue() + "', ?::DATE) " +
                    "GROUP BY date_trunc('" + truncate.getValue() + "', dial_sheet.dial_date)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            resultSet = statement.executeQuery();

            DialSheet dialSheet;
            if (!resultSet.next()) {
                dialSheet = new DialSheet(null, 0, date.getTime(), 0, 0);
            } else {
                dialSheet = getDialSheetFromResultSet(resultSet);
            }


            // Contact Types
            sql = "SELECT state_type AS contact_status, state_name, state_class, 0 AS " + STATUS_COUNT + ", " +
                    "is_indeterminate, is_objection, is_inventory, _index " +
                    "FROM person_state_type " +
                    "WHERE is_dial_sheet_state = TRUE AND state_type NOT IN (" +
                    "   SELECT contact_status " +
                    "   FROM contact_status_type " +
                    "   WHERE user_id = ? AND  date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE)" +
                    ") " +
                    "UNION DISTINCT " +
                    "SELECT contact_status, state_name, state_class, sum(status_count) AS status_count, " +
                    "is_indeterminate, is_objection, is_inventory, _index " +
                    "FROM contact_status_type " +
                    "WHERE user_id = ? AND " +
                    "date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE) " +
                    "GROUP BY contact_status, state_name, state_class, is_indeterminate, is_objection, " +
                    "is_inventory, _index " +
                    "ORDER BY _index ASC;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            statement.setString(3, userId);
            statement.setDate(4, new java.sql.Date(date.getTime()));

            resultSet = statement.executeQuery();
            List<DialSheetContactType> contactTypeList = getContactTypesFromResultSet(resultSet);

            List<DialSheetAppointment> appointmentList =
                    dialSheetAppointmentDBAccessor.getAppointmentsFromDialSheet(connection, userId, date, truncate);

            String personIdTablePrefix = PERSON_PROSPECT + ".";

            // Activity
            sql = "SELECT " + getSelectionForPerson(personIdTablePrefix, null, false) +
                    ", " + CONTACT_TIME + " " +
                    "FROM dial_sheet_contact " +
                    "JOIN dial_sheet ON dial_sheet_contact.sheet_id = dial_sheet.sheet_id " +
                    "JOIN person_prospect ON dial_sheet_contact.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type ON contact_status = person_state_type.state_type " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "WHERE date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ?::DATE) " +
                    "AND (receiver_id = ? OR owner_id = ?) " +
                    "GROUP BY " + CONTACT_TIME + ", " +
                    getSelectionForPerson(personIdTablePrefix, null, true) + " " +
                    "ORDER BY contact_time ASC ";
            statement = connection.prepareStatement(sql);
            statement.setDate(1, new java.sql.Date(date.getTime()));
            statement.setString(2, userId);
            statement.setString(3, userId);
            resultSet = statement.executeQuery();

            List<DialSheetContact> dialSheetActivity = new ArrayList<>();
            while (resultSet.next()) {
                Prospect prospect = getPersonFromResultSet(resultSet, personIdTablePrefix, null);
                long contactTime = resultSet.getTimestamp(CONTACT_TIME).getTime();
                dialSheetActivity.add(new DialSheetContact(prospect, contactTime));
            }

            int newAppointmentsCount = dialSheetAppointmentDBAccessor.getNewAppointmentsCountFromDialSheet(connection, userId, date, truncate);
            int otherAppointmentsCount = dialSheetAppointmentDBAccessor.getOtherAppointmentsCountFromDialSheet(connection, userId, date, truncate);

            BusinessFunnelDBAccessor businessFunnelDBAccessor = new BusinessFunnelDBAccessor(getDatabase());

            BusinessFunnelStatistic contactsStatistic = businessFunnelDBAccessor.getContactsStatistic(connection, userId, date, truncate);

            BusinessFunnelStatistic followUpsStatistic = businessFunnelDBAccessor.getFollowUpsStatistic(connection, userId, date, truncate);

            BusinessFunnelStatistic objectionsStatistic = businessFunnelDBAccessor.getObjectionsStatistic(connection, userId, date, truncate);

            BusinessFunnelStatistic newAppointmentsScheduledStatistic = businessFunnelDBAccessor.getNewAppointmentsScheduledStatistic(connection, userId, date, truncate);

            BusinessFunnelStatistic newAppointmentsKeptStatistic = businessFunnelDBAccessor.getNewAppointmentsKeptStatistic(connection, userId, date, truncate);

            BusinessFunnelStatistic closesStatistic = businessFunnelDBAccessor.getClosesStatistic(connection, userId, date, truncate);

            return new DialSheetDetails(new HomePageDialSheet(dialSheet, contactTypeList, newAppointmentsCount,
                    otherAppointmentsCount, appointmentList, dialSheetActivity), contactsStatistic, followUpsStatistic,
                    objectionsStatistic, newAppointmentsScheduledStatistic, newAppointmentsKeptStatistic,
                    closesStatistic);
        });
    }

    private static List<DialSheetContactType> getContactTypesFromResultSet(ResultSet resultSet) throws SQLException {
        List<DialSheetContactType> contactTypeList = new ArrayList<>();

        while (resultSet.next()) {
            String stateType = resultSet.getString(CONTACT_STATUS);
            String stateDescription = resultSet.getString(STATE_NAME);
            String stateClass = resultSet.getString(STATE_CLASS);
            int stateCount = resultSet.getInt(STATUS_COUNT);
            boolean isIndeterminate = resultSet.getBoolean(IS_INDETERMINATE);
            boolean isObjection = resultSet.getBoolean(IS_OBJECTION);
            boolean isInventory = resultSet.getBoolean(IS_INVENTORY);
            contactTypeList.add(new DialSheetContactType(stateType, stateDescription, stateClass, stateCount,
                    isIndeterminate, isObjection, isInventory));
        }

        return contactTypeList;
    }

    private static DialSheetPaginationSummary getDialSheetPaginationFromResultSet(ResultSet resultSet) throws SQLException {
        String sheetId = resultSet.getString(SHEET_ID);
        long date = resultSet.getDate(DIAL_DATE).getTime();
        return new DialSheetPaginationSummary(sheetId, date);
    }

}
