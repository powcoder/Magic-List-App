https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import cache.DialSheetCache;
import model.dialsheet.DialSheet;
import model.dialsheet.DialSheetAppointment;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._DialSheetAppointment.*;
import static database.TablesContract._PersonProspect.PERSON_PROSPECT;

/**
 * Created by Corey Caplan on 9/6/17.
 */
public class DialSheetAppointmentDBAccessor extends AbstractPersonDatabase {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    public DialSheetAppointmentDBAccessor(Database database) {
        super(database);
    }

    public boolean createDialSheetAppointment(String userId, String appointmentId, String personId,
                                              boolean isConferenceCall, long appointmentDate, String appointmentNotes,
                                              ProspectState appointmentType) {
        return getDatabase().withConnection(false, connection -> {
            DialSheet dialSheet = new DialSheetDBAccessor(getDatabase())
                    .getCurrentAllPagesDialSheet(userId);

            if (dialSheet == null) {
                logger.error("Could not get today\'s dial sheet", new IllegalStateException());
                connection.rollback();
                return false;
            }

            String sql = "INSERT INTO dial_sheet_appointment(appointment_id, user_id, sheet_id, person_id, " +
                    "is_conference_call, appointment_date, appointment_notes, appointment_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, appointmentId);
            statement.setString(2, userId);
            statement.setString(3, dialSheet.getId());
            statement.setString(4, personId);
            statement.setBoolean(5, isConferenceCall);
            statement.setTimestamp(6, new Timestamp(appointmentDate));
            setStringOrNull(statement, 7, appointmentNotes);
            statement.setString(8, appointmentType.getStateType());

            if (statement.executeUpdate() != 1) {
                logger.error("Could not insert appointment", new IllegalStateException());
                return false;
            }

            boolean isSuccessful = new ProspectDBAccessor(getDatabase())
                    .setPersonState(connection, userId, personId, appointmentType);
            if (!isSuccessful) {
                logger.error("Could not update activity sheet", new IllegalStateException());
                return false;
            }

            DialSheetCache.removeCurrentDialSheet(userId);
            connection.commit();
            return true;
        });
    }

    public Optional<Boolean> deleteDialSheetAppointment(String userId, String appointmentId) {
        return getDatabase().withConnection(connection -> {
            String sql = "DELETE FROM dial_sheet_appointment WHERE appointment_id = ? AND user_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, appointmentId);
            statement.setString(2, userId);

            DialSheetCache.removeCurrentDialSheet(userId);

            return Optional.of(statement.executeUpdate() == 1);
        });
    }

    public Optional<Boolean> editDialSheetAppointment(String userId, DialSheetAppointment dialSheetAppointment) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE dial_sheet_appointment " +
                    "SET appointment_date = ?, appointment_notes = ?, is_conference_call = ?, appointment_type = ? " +
                    "WHERE appointment_id = ? AND user_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(dialSheetAppointment.getAppointmentDate()));
            setStringOrNull(statement, 2, dialSheetAppointment.getNotes());
            statement.setBoolean(3, dialSheetAppointment.isConferenceCall());
            statement.setString(4, dialSheetAppointment.getAppointmentType().getStateType());
            statement.setString(5, dialSheetAppointment.getAppointmentId());
            statement.setString(6, userId);

            // TODO fix how changing the appt type FROM intro to anything else puts the prospecting sheet out of sync.
            // FIXME fix the contact status that was set for this appt!
            DialSheetCache.removeCurrentDialSheet(userId);

            int numberOfUpdates = statement.executeUpdate();

            return Optional.of(numberOfUpdates == 1);
        });
    }

    public DialSheetAppointment getAppointmentById(String userId, String appointmentId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT " + getProjectionForDialSheetAppointment(false) + " " +
                    "FROM dial_sheet_appointment " +
                    "JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_state.person_id = person_prospect.person_id " +
                    "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                    "LEFT JOIN person_state_type a_state ON person_state.state = a_state.state_type " +
                    "LEFT JOIN person_state_type a_type_state ON appointment_type = a_type_state.state_type " +
                    "WHERE appointment_id = ? AND dial_sheet_appointment.user_id = ? " +
                    "UNION " +
                    "SELECT " + getProjectionForDialSheetAppointment(false) + " " +
                    "FROM dial_sheet_appointment " +
                    "JOIN shares_prospect ON dial_sheet_appointment.person_id = shares_prospect.person_id " +
                    "JOIN person_prospect ON shares_prospect.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_state.person_id = shares_prospect.person_id " +
                    "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                    "LEFT JOIN person_state_type a_state ON person_state.state = a_state.state_type " +
                    "LEFT JOIN person_state_type a_type_state ON appointment_type = a_type_state.state_type " +
                    "WHERE appointment_id = ? AND dial_sheet_appointment.user_id = ? AND receiver_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, appointmentId);
            statement.setString(2, userId);
            statement.setString(3, appointmentId);
            statement.setString(4, userId);
            statement.setString(5, userId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getDialSheetAppointmentFromResultSet(resultSet);
            } else {
                return null;
            }
        });
    }

    // Package-Private Methods

    @SuppressWarnings("SameParameterValue")
    static String getProjectionForDialSheetAppointment(boolean isGroupBy) {
        String personIdTablePrefix = PERSON_PROSPECT + ".";
        String personStateTablePrefix = "p_state.";
        String appointmentStateTablePrefix = "a_state.";
        String appointmentTypeTablePrefix = "a_type_state.";
        return getFormattedColumns(APPOINTMENT_ID, APPOINTMENT_DATE, APPOINTMENT_NOTES, IS_CONFERENCE_CALL,
                ProspectStateDBAccessor.getProjectionForProspectState(appointmentTypeTablePrefix, isGroupBy),
                ProspectStateDBAccessor.getProjectionForProspectState(appointmentStateTablePrefix, isGroupBy),
                ProspectDBAccessor.getSelectionForPerson(personIdTablePrefix, personStateTablePrefix, isGroupBy));
    }

    List<DialSheetAppointment> getAppointmentsFromDialSheet(Connection connection, String userId, String sheetId) throws SQLException {
        String sql = "SELECT " + getProjectionForDialSheetAppointment(false) + " " +
                "FROM dial_sheet_appointment " +
                "JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                "JOIN person_state ON person_state.person_id = person_prospect.person_id " +
                "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                "LEFT JOIN person_state_type a_state ON person_state.state = a_state.state_type " +
                "LEFT JOIN person_state_type a_type_state ON appointment_type = a_type_state.state_type " +
                "WHERE sheet_id = ? AND dial_sheet_appointment.user_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, sheetId);
        statement.setString(2, userId);

        ResultSet resultSet = statement.executeQuery();

        List<DialSheetAppointment> appointmentList = new ArrayList<>();
        while (resultSet.next()) {
            appointmentList.add(getDialSheetAppointmentFromResultSet(resultSet));
        }

        return appointmentList;
    }

    List<DialSheetAppointment> getDialSheetAppointmentsForCurrentDialSheet(Connection connection, String userId) throws SQLException {
        String sql = "SELECT " + getProjectionForDialSheetAppointment(false) + " " +
                "FROM dial_sheet_appointment " +
                "JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                "JOIN dial_sheet ON dial_sheet_appointment.sheet_id = dial_sheet.sheet_id " +
                "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                "LEFT JOIN person_state_type a_state ON person_state.state = a_state.state_type " +
                "LEFT JOIN person_state_type a_type_state ON appointment_type = a_type_state.state_type " +
                "WHERE dial_date = current_date AND dial_sheet_appointment.user_id = ?;";

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        ResultSet resultSet = statement.executeQuery();
        List<DialSheetAppointment> appointmentList = new ArrayList<>();

        while (resultSet.next()) {
            appointmentList.add(getDialSheetAppointmentFromResultSet(resultSet));
        }

        return appointmentList;
    }

    List<DialSheetAppointment> getAppointmentsFromDialSheet(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT " + getProjectionForDialSheetAppointment(false) + " " +
                "FROM dial_sheet_appointment " +
                "JOIN dial_sheet ON dial_sheet_appointment.sheet_id = dial_sheet.sheet_id " +
                "JOIN person_prospect ON dial_sheet_appointment.person_id = person_prospect.person_id " +
                "JOIN person_state ON person_state.person_id = person_prospect.person_id " +
                "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                "LEFT JOIN person_state_type a_state ON person_state.state = a_state.state_type " +
                "LEFT JOIN person_state_type a_type_state ON appointment_type = a_type_state.state_type " +
                "WHERE dial_sheet_appointment.user_id = ? " +
                "AND date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ?::DATE);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setDate(2, new java.sql.Date(date.getTime()));

        ResultSet resultSet = statement.executeQuery();

        List<DialSheetAppointment> appointmentList = new ArrayList<>();
        while (resultSet.next()) {
            appointmentList.add(getDialSheetAppointmentFromResultSet(resultSet));
        }

        return appointmentList;
    }

    int getNewAppointmentsCountFromDialSheet(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) " +
                "FROM dial_sheet " +
                "LEFT JOIN dial_sheet_appointment ON dial_sheet.sheet_id = dial_sheet_appointment.sheet_id " +
                "WHERE dial_sheet.user_id = ? AND " +
                "appointment_type = ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, "introduction");
        statement.setDate(3, new java.sql.Date(date.getTime()));

        return getIntegerFromResultSet(statement.executeQuery());
    }

    int getOtherAppointmentsCountFromDialSheet(Connection connection, String userId, Date date, DateTruncateValue truncate) throws SQLException {
        String sql = "SELECT count(*) " +
                "FROM dial_sheet " +
                "LEFT JOIN dial_sheet_appointment ON dial_sheet.sheet_id = dial_sheet_appointment.sheet_id " +
                "WHERE dial_sheet.user_id = ? AND " +
                "appointment_type != ? AND " +
                "date_trunc('" + truncate.getValue() + "', dial_date) = date_trunc('" + truncate.getValue() + "', ? :: DATE)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, "introduction");
        statement.setDate(3, new java.sql.Date(date.getTime()));

        return getIntegerFromResultSet(statement.executeQuery());
    }

    static DialSheetAppointment getDialSheetAppointmentFromResultSet(ResultSet resultSet) throws SQLException {
        String appointmentId = resultSet.getString(APPOINTMENT_ID);
        boolean isConferenceCall = resultSet.getBoolean(IS_CONFERENCE_CALL);
        long appointmentDate = resultSet.getTimestamp(APPOINTMENT_DATE).getTime();
        String appointmentNotes = resultSet.getString(APPOINTMENT_NOTES);

        String personIdTablePrefix = PERSON_PROSPECT + ".";
        String personStateTablePrefix = "p_state.";
        Prospect person = getPersonFromResultSet(resultSet, personIdTablePrefix, personStateTablePrefix);

        String appointmentTypeTablePrefix = "a_type_state.";
        ProspectState appointmentType = ProspectStateDBAccessor.getProspectStateFromResultSet(resultSet, appointmentTypeTablePrefix);

        String appointmentOutcomeTablePrefix = "a_state.";
        ProspectState appointmentOutcome = ProspectStateDBAccessor.getProspectStateFromResultSet(resultSet, appointmentOutcomeTablePrefix);

        return new DialSheetAppointment(appointmentId, isConferenceCall, appointmentDate, person, appointmentNotes,
                appointmentType, appointmentOutcome);
    }

}
