https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.PagedList;
import model.calendar.CalendarEvent;
import model.calendar.CalendarProvider;
import model.dialsheet.DialSheetAppointment;
import model.prospect.Appointment;
import model.prospect.ProspectState;
import model.user.User;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.*;
import java.util.Date;

import static database.TablesContract._CalendarAppointmentLinks.*;
import static database.TablesContract._DialSheetAppointment.*;
import static database.TablesContract._DialSheetContact.CONTACT_TIME;
import static database.TablesContract._OAuthAccount.OAUTH_ACCOUNT_ID;
import static database.TablesContract._PersonProspect.PERSON_COMPANY_NAME;
import static database.TablesContract._PersonProspect.PERSON_ID;
import static database.TablesContract._User.USER;
import static database.TablesContract._User.USER_ID;

/**
 * Created by Corey Caplan on 9/14/17.
 */
public class AppointmentDBAccessor extends ProspectDBAccessor {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    public AppointmentDBAccessor(Database database) {
        super(database);
    }

    public boolean setPersonStateFromAppointmentOutcome(String userId, String appointmentId, ProspectState state) {
        return getDatabase().withConnection(false, connection -> {
            ResultSet resultSet;
            PreparedStatement statement;
            String sql;

            sql = "SELECT person_id FROM dial_sheet_appointment WHERE appointment_id = ? AND user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, appointmentId);
            statement.setString(2, userId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                logger.info("Could not get person from appointment: [appointment_id: {}, user_id: {}]", appointmentId, userId);
                return false;
            }

            String personId = resultSet.getString(PERSON_ID);

            sql = "UPDATE dial_sheet_appointment SET appointment_outcome = ? " +
                    "WHERE user_id = ? AND appointment_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, state.getStateType());
            statement.setString(2, userId);
            statement.setString(3, appointmentId);
            if (statement.executeUpdate() != 1) {
                logger.info("Could not update appointment outcome: [appointment_id: {}, user: {}]", appointmentId, userId);
                connection.rollback();
                return false;
            }

            if (!state.getStateType().equals(ProspectState.NOT_CONTACTED.getStateType())) {
                sql = "UPDATE person_state SET state = ? WHERE person_id = ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, state.getStateType());
                statement.setString(2, personId);

                if (statement.executeUpdate() != 1) {
                    logger.info("Could not update person status: [user: {}, notification_id: {}]", userId, appointmentId);
                    connection.rollback();
                    return false;
                }
            }

            Appointment appointment = getAppointmentById(connection, userId, appointmentId);
            if (appointment == null) {
                logger.error("Could not get appointment for setting outcome: {}, {}", userId, appointmentId);
                return false;
            }

            DialSheetContactDBAccessor dialSheetContactDBAccessor = new DialSheetContactDBAccessor(getDatabase());

            Date contactTime = new Date(appointment.getAppointmentDate());
            if (dialSheetContactDBAccessor.createDialSheetActivity(connection, state, userId, personId, contactTime)) {
                connection.commit();
                return true;
            } else {
                return false;
            }
        });
    }

    public PagedList<Appointment> getRecentUnfulfilledAppointments(String userId, Date currentDate, int currentPage,
                                                                   Appointment.Sorter sorter, boolean isAscending) {
        return getDatabase().withConnection(connection -> {
            String sortOrder = isAscending ? "ASC" : "DESC";
            String sql = "SELECT " + getProjectionForAppointmentsWithTables() + " " +
                    "WHERE " + APPOINTMENT_DATE + " <= (? :: TIMESTAMP) AND " +
                    APPOINTMENT_DATE + " >= (? :: TIMESTAMP - INTERVAL '1 months') AND " +
                    DIAL_SHEET_APPOINTMENT + "." + USER_ID + " = ? AND " +
                    APPOINTMENT_OUTCOME + " = ? " +
                    "GROUP BY " + getProjectionForAppointments(true) + " " +
                    "ORDER BY " + convertAppointmentSorterToDatabaseColumn(sorter) + " " + sortOrder + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + ((currentPage - 1) * AMOUNT_PER_PAGE) + ";";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(currentDate.getTime()));
            statement.setTimestamp(2, new Timestamp(currentDate.getTime()));
            statement.setString(3, userId);
            statement.setString(4, ProspectState.NOT_CONTACTED.getStateType());

            int recentAppointmentsCount = getRecentUnfulfilledAppointmentsCount(connection, userId, currentDate);
            PagedList<Appointment> appointments = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
            appointments.setTotalNumberOfItems(recentAppointmentsCount);

            return getAppointmentsFromResultSet(statement.executeQuery(), currentPage, appointments);
        });
    }

    public int getRecentUnfulfilledAppointmentsCount(String userId, Date currentDate) {
        return getDatabase().withConnection(connection -> {
            return getRecentUnfulfilledAppointmentsCount(connection, userId, currentDate);
        });
    }

    public PagedList<Appointment> getAppointmentsForToday(String userId, Date currentDate, int currentPage,
                                                          Appointment.Sorter sorter, boolean isAscending) {
        final int amountPerPage = 100;
        String sortOrder = isAscending ? "ASC" : "DESC";
        return getDatabase().withConnection(connection -> {
            String sql = "WITH total_count_table AS (" +
                    "SELECT count(*) AS total_count, user_id " +
                    "FROM dial_sheet_appointment " +
                    "WHERE appointment_date :: DATE = ? :: DATE " +
                    "GROUP BY " + USER_ID + " " +
                    ") " +
                    "SELECT total_count_table.total_count, " + getProjectionForAppointmentsWithTables() + " " +
                    "JOIN total_count_table ON total_count_table.user_id = " + DIAL_SHEET_APPOINTMENT + "." + USER_ID + " " +
                    "WHERE " + APPOINTMENT_DATE + " :: DATE = ? :: DATE AND " +
                    DIAL_SHEET_APPOINTMENT + "." + USER_ID + " = ? " +
                    "GROUP BY " + getProjectionForAppointments(true) + ", total_count_table.total_count " +
                    "ORDER BY " + convertAppointmentSorterToDatabaseColumn(sorter) + " " + sortOrder + " " +
                    "LIMIT " + amountPerPage + " OFFSET " + ((currentPage - 1) * amountPerPage) + ";";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(currentDate.getTime()));
            statement.setTimestamp(2, new Timestamp(currentDate.getTime()));
            statement.setString(3, userId);

            int appointmentsCountForToday = getAppointmentsForTodayCount(connection, userId, currentDate);
            PagedList<Appointment> appointments = new PagedList<>(currentPage, amountPerPage);
            appointments.setTotalNumberOfItems(appointmentsCountForToday);

            return getAppointmentsFromResultSet(statement.executeQuery(), currentPage, appointments);
        });
    }

    public int getAppointmentsForTodayCount(String userId, Date currentDate) {
        return getDatabase().withConnection(connection -> {
            return getAppointmentsForTodayCount(connection, userId, currentDate);
        });
    }

    public PagedList<Appointment> getUpcomingAppointments(String userId, Date currentDate, int currentPage, Appointment.Sorter sorter, boolean isAscending) {
        return getRecentOrUpcomingAppointments(userId, currentDate, isAscending, currentPage, sorter, true);
    }

    public PagedList<Appointment> getPastAppointments(String userId, Date currentDate, int currentPage, Appointment.Sorter sorter, boolean isAscending) {
        return getRecentOrUpcomingAppointments(userId, currentDate, isAscending, currentPage, sorter, false);
    }

    // Package-Private Methods

    static PagedList<Appointment> getAppointmentsFromResultSet(ResultSet resultSet, int currentPage, PagedList<Appointment> appointmentList) throws SQLException {
        if (appointmentList == null) {
            appointmentList = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
        }

        Map<String, Appointment> appointmentsMap = new HashMap<>();
        while (resultSet.next()) {
            if (currentPage != -1) {
                setMaxPage(appointmentList, resultSet);
            }

            DialSheetAppointment dialSheetAppointment = DialSheetAppointmentDBAccessor.getDialSheetAppointmentFromResultSet(resultSet);

            long lastContacted = Optional.ofNullable(resultSet.getTimestamp(LAST_CONTACTED))
                    .orElse(new Timestamp(-1))
                    .getTime();

            String userPrefix = USER + ".";
            User creator = UserDBAccessor.getUserFromResultSet(resultSet, userPrefix);

            String appointmentId = dialSheetAppointment.getAppointmentId();
            List<CalendarEvent> providerCalendarEvents;
            if (appointmentsMap.containsKey(appointmentId)) {
                providerCalendarEvents = appointmentsMap.get(appointmentId).getCalendarEvents();
            } else {
                providerCalendarEvents = new ArrayList<>();
                Appointment appointment = new Appointment(dialSheetAppointment, providerCalendarEvents, lastContacted, creator);
                appointmentsMap.put(appointmentId, appointment);
                appointmentList.add(appointment);
            }

            String linkId = resultSet.getString(PROVIDER_APPOINTMENT_ID);
            if (linkId != null) {
                CalendarProvider provider = CalendarProvider.parse(resultSet.getString(PROVIDER));
                String oauthAccountId = resultSet.getString(OAUTH_ACCOUNT_ID);
                String hyperLink = resultSet.getString(HYPER_LINK);
                String subject = resultSet.getString(CALENDAR_SUBJECT);
                providerCalendarEvents.add(new CalendarEvent(provider, linkId, oauthAccountId, hyperLink, subject));
            }
        }

        return appointmentList;
    }

    static String getProjectionForAppointments(boolean isGroupBy) {
        String userTablePrefix = USER + ".";
        String provider = CALENDAR_APPOINTMENT_LINKS + "." + PROVIDER;
        String areaCode;

        if (!isGroupBy) {
            areaCode = "substring(person_phone, '\\(?(\\d{3})\\)?') AS area_code";
        } else {
            areaCode = "area_code";
        }

        String lastContacted = null;
        if (!isGroupBy) {
            lastContacted = "MAX(" + CONTACT_TIME + ")" + " AS " + LAST_CONTACTED;
        }

        String formattedColumns = getFormattedColumns(PROVIDER_APPOINTMENT_ID, HYPER_LINK, CALENDAR_SUBJECT, provider,
                areaCode, OAUTH_ACCOUNT_ID, UserDBAccessor.getProjectionForUser(userTablePrefix, isGroupBy),
                DialSheetAppointmentDBAccessor.getProjectionForDialSheetAppointment(isGroupBy));

        if (lastContacted != null) {
            formattedColumns += ", " + lastContacted;
        }

        return formattedColumns;
    }

    static String getProjectionForAppointmentsWithTables() {
        //language=PostgreSQL
        return getProjectionForAppointments(false) + " " +
                "FROM dial_sheet_appointment " +
                "JOIN _user ON dial_sheet_appointment.user_id = _user.user_id " +
                "LEFT JOIN calendar_appointment_links ON appointment_id = app_appointment_id " +
                "JOIN person_prospect ON person_prospect.person_id = dial_sheet_appointment.person_id " +
                "JOIN person_state ON person_state.person_id = person_prospect.person_id " +
                "JOIN person_state_type p_state ON state = p_state.state_type " +
                "JOIN person_state_type a_state ON dial_sheet_appointment.appointment_outcome = a_state.state_type " +
                "JOIN person_state_type a_type_state ON dial_sheet_appointment.appointment_type = a_type_state.state_type " +
                "JOIN dial_sheet ON dial_sheet.sheet_id = dial_sheet_appointment.sheet_id " +
                "LEFT JOIN dial_sheet_contact ON dial_sheet_appointment.person_id = dial_sheet_contact.person_id " +
                "LEFT JOIN oauth_account ON dial_sheet_appointment.user_id = oauth_account.user_id ";
    }

    // Mark Private Methods

    private Appointment getAppointmentById(Connection connection, String userId, String appointmentId) throws SQLException {
        String sql = "SELECT " + getProjectionForAppointmentsWithTables() + " " +
                "WHERE " + DIAL_SHEET_APPOINTMENT + "." + USER_ID + " = ? AND " +
                DIAL_SHEET_APPOINTMENT + "." + APPOINTMENT_ID + " = ? " +
                "GROUP BY " + getProjectionForAppointments(true);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, appointmentId);
        ResultSet resultSet = statement.executeQuery();

        PagedList<Appointment> pagedList = new PagedList<>(-1, 1);
        pagedList = getAppointmentsFromResultSet(resultSet, -1, pagedList);
        if (pagedList.isEmpty()) {
            return null;
        } else {
            return pagedList.get(0);
        }
    }

    private int getRecentUnfulfilledAppointmentsCount(Connection connection, String userId, Date currentDate) throws SQLException {
        String sql = "SELECT count(*) AS total_count " +
                "FROM dial_sheet_appointment " +
                "WHERE appointment_date <= ? :: TIMESTAMP AND " +
                "appointment_date >= (? :: TIMESTAMP - INTERVAL '1 months') AND " +
                "appointment_outcome = ? AND " +
                "user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setTimestamp(1, new Timestamp(currentDate.getTime()));
        statement.setTimestamp(2, new Timestamp(currentDate.getTime()));
        statement.setString(3, "not_contacted");
        statement.setString(4, userId);

        ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? resultSet.getInt(1) : 0;
    }

    private int getAppointmentsForTodayCount(Connection connection, String userId, Date currentDate) throws SQLException {
        String sql = "SELECT count(*) AS total_count " +
                "FROM dial_sheet_appointment " +
                "WHERE appointment_date :: DATE = ? :: DATE AND user_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setTimestamp(1, new Timestamp(currentDate.getTime()));
        statement.setString(2, userId);

        ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? resultSet.getInt(1) : 0;
    }

    private PagedList<Appointment> getRecentOrUpcomingAppointments(String userId, Date currentDate, boolean isAscending, int currentPage,
                                                                   Appointment.Sorter sorter, boolean isUpcomingAppointments) {
        String sortOrder = isAscending ? "ASC" : "DESC";
        final String sign = isUpcomingAppointments ? ">" : "<=";
        int offset = (currentPage - 1) * AMOUNT_PER_PAGE;
        return getDatabase().withConnection(connection -> {
            String sql = "WITH total_count_table AS (" +
                    "SELECT count(*) AS total_count " +
                    "FROM dial_sheet_appointment " +
                    "WHERE user_id = ? AND appointment_date " + sign + " ? :: TIMESTAMP " +
                    ") " +
                    "SELECT total_count_table.total_count, " + getProjectionForAppointmentsWithTables() + " " +
                    "CROSS JOIN total_count_table " +
                    "WHERE " + APPOINTMENT_DATE + " " + sign + " ? :: TIMESTAMP AND " +
                    DIAL_SHEET_APPOINTMENT + "." + USER_ID + " = ? " +
                    "GROUP BY " + getProjectionForAppointments(true) + ", total_count_table.total_count " +
                    "ORDER BY " + convertAppointmentSorterToDatabaseColumn(sorter) + " " + sortOrder + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + offset + ";";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setTimestamp(2, new Timestamp(currentDate.getTime()));
            statement.setTimestamp(3, new Timestamp(currentDate.getTime()));
            statement.setString(4, userId);
            ResultSet resultSet = statement.executeQuery();

            return getAppointmentsFromResultSet(resultSet, currentPage, null);
        });
    }

    private static String convertAppointmentSorterToDatabaseColumn(Appointment.Sorter sorter) {
        if (sorter == Appointment.Sorter.COMPANY_NAME) {
            return PERSON_COMPANY_NAME;
        } else {
            return sorter.getRawText();
        }
    }

}
