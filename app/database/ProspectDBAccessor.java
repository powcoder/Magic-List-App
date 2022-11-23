https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import cache.DialSheetCache;
import model.PagedList;
import model.dialsheet.DialSheet;
import model.lists.ProspectSearch;
import model.prospect.Appointment;
import model.prospect.ContactStatusAuditItem;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.user.User;
import play.Logger;
import play.db.Database;
import utilities.Validation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._DialSheet.*;
import static database.TablesContract._DialSheetAppointment.*;
import static database.TablesContract._DialSheetAppointment.PERSON_ID;
import static database.TablesContract._DialSheetContact.*;
import static database.TablesContract._DialSheetContact.SHEET_ID;
import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._PersonState.PERSON_STATE;
import static database.TablesContract._PersonStateType.PERSON_STATE_TYPE;
import static database.TablesContract._SharesProspect.*;
import static database.TablesContract._User.*;

/**
 *
 */
public class ProspectDBAccessor extends AbstractPersonDatabase {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public ProspectDBAccessor(Database database) {
        super(database);
    }

    public boolean setPersonNotes(String userId, String personId, String notes) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE person_state SET notes = ? WHERE person_id = (" +
                    "   SELECT person_prospect.person_id " +
                    "   FROM person_prospect " +
                    "   LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "   WHERE person_prospect.person_id = ? AND " + getPredicateForProspectByUserId() + " " +
                    ");";
            PreparedStatement statement = connection.prepareStatement(sql);
            setStringOrNull(statement, 1, notes);
            statement.setString(2, personId);
            statement.setString(3, userId);
            statement.setString(4, userId);

            return statement.executeUpdate() == 1;
        });
    }

    public boolean createPerson(String userId, Prospect prospect, String listId) {
        return getDatabase().withConnection(false, connection -> {
            String sql = "INSERT INTO person_prospect (person_name, person_email, person_phone, person_company_name, " +
                    "job_title, owner_id, person_id) VALUES (?, ?, ?, ?, ?, ?, ?);";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, prospect.getName());
            setStringOrNull(statement, 2, prospect.getEmail());
            setStringOrNull(statement, 3, prospect.getPhoneNumber());
            setStringOrNull(statement, 4, prospect.getCompanyName());
            setStringOrNull(statement, 5, prospect.getJobTitle());
            setStringOrNull(statement, 6, userId);
            setStringOrNull(statement, 7, prospect.getId());

            statement.executeUpdate();

            sql = "INSERT INTO person_state(person_id, state, notes) VALUES (?, ?, ?);";
            statement = connection.prepareStatement(sql);
            statement.setString(1, prospect.getId());
            statement.setString(2, ProspectState.NOT_CONTACTED.getStateType());
            setStringOrNull(statement, 3, null);
            statement.executeUpdate();

            if(!Validation.isEmpty(listId)) {
                sql = "INSERT INTO searches(search_id, person_id) VALUES (?, ?)";
                statement = connection.prepareStatement(sql);
                statement.setString(1, listId);
                statement.setString(2, prospect.getId());
                statement.executeUpdate();
            }

            connection.commit();
            return true;
        });
    }

    public boolean updatePerson(String userId, Prospect prospect) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE person_prospect SET person_name = ?, person_email = ?, person_phone = ?, " +
                    "person_company_name = ?, job_title = ? " +
                    "WHERE person_id = (" +
                    "   SELECT person_prospect.person_id " +
                    "   FROM person_prospect " +
                    "   LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "   WHERE person_prospect.person_id = ? AND " + getPredicateForProspectByUserId() + " " +
                    ");";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, prospect.getName());
            setStringOrNull(statement, 2, prospect.getEmail());
            setStringOrNull(statement, 3, prospect.getPhoneNumber());
            setStringOrNull(statement, 4, prospect.getCompanyName());
            setStringOrNull(statement, 5, prospect.getJobTitle());
            setStringOrNull(statement, 6, prospect.getId());
            setStringOrNull(statement, 7, userId);
            setStringOrNull(statement, 8, userId);

            if (statement.executeUpdate() == 1) {
                DialSheetCache.removeCurrentDialSheet(userId);
                return true;
            } else {
                return false;
            }
        });
    }

    public Optional<Boolean> deletePersonById(String userId, String personId) {
        return getDatabase().withConnection(connection -> {
            // Check that the person's selected state matches a state that is defined within a company
            // language=PostgreSQL
            String sql = "DELETE FROM person_prospect WHERE person_id = ? AND owner_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, userId);

            if (statement.executeUpdate() == 1) {
                DialSheetCache.removeCurrentDialSheet(userId);
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        });
    }

    public boolean setPersonState(String userId, String personId, ProspectState newState) {
        return getDatabase().withConnection(false, connection -> {
            return setPersonState(connection, userId, personId, newState);
        });
    }

    public boolean migratePastPersonState(String userId, String personId, ProspectState newState) {
        return getDatabase().withConnection(false, connection -> {
            String sql = "UPDATE person_state SET state = ? WHERE person_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newState.getStateType());
            statement.setString(2, personId);

            if(statement.executeUpdate() != 1) {
                logger.error("Could not revise the most recent activity sheet ");
            }

            DialSheetContactDBAccessor dialSheetContactDBAccessor = new DialSheetContactDBAccessor(getDatabase());

            if (!dialSheetContactDBAccessor.updateMostRecentDialSheetActivityStatus(connection, newState, userId, personId)) {
                logger.error("Could not revise most recent activity sheet status for user: {}", userId);
                return false;
            }

            connection.commit();
            return true;
        });
    }

    public boolean updatePastPersonState(String userId, String personId, ProspectState newState, String sheetId) {
        return getDatabase().withConnection(false, connection -> {

            DialSheet dialSheet = new DialSheetDBAccessor(getDatabase()).getCurrentAllPagesDialSheet(userId);
            if (dialSheet == null) {
                logger.error("Could not retrieve dial sheet for user: {}", userId);
                return false;
            }

            if (dialSheet.getId().equals(sheetId)) {
                // The sheet being edited is today's. Let's update the person's current state too
                // Check that the person's selected state matches a state that is defined within a company
                String sql = "UPDATE person_state SET state = ? WHERE person_id = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, newState.getStateType());
                statement.setString(2, personId);
                statement.executeUpdate();
            }

            DialSheetContactDBAccessor dialSheetContactDBAccessor = new DialSheetContactDBAccessor(getDatabase());
            if (!dialSheetContactDBAccessor.updateDialSheetActivityStatus(connection, newState, userId, personId, sheetId)) {
                logger.error("Could not revise activity sheet status");
                return false;
            }

            connection.commit();
            return true;
        });
    }

    public Optional<Prospect> getPersonById(String userId, String personId) {
        return getDatabase().withConnection(connection -> {
            // language=PostgreSQL
            String sql = "SELECT * " +
                    "FROM person_prospect " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "FULL JOIN _user ON shares_prospect.sharer_id = _user.user_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type ON person_state.state = person_state_type.state_type " +
                    "WHERE person_prospect.person_id = ? AND " + getPredicateForProspectByUserId() + ";";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, userId);
            statement.setString(3, userId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(getPersonFromResultSet(resultSet));
            } else {
                return Optional.empty();
            }
        });
    }

    public Optional<Boolean> isOwner(String userId, String personId) {
        return getDatabase().withConnection(connection -> {
            // language=PostgreSQL
            String sql = "SELECT * " +
                    "FROM person_prospect " +
                    "WHERE person_prospect.person_id = ? AND owner_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, userId);
            ResultSet resultSet = statement.executeQuery();

            return Optional.of(resultSet.isBeforeFirst());
        });
    }

    public PagedList<Prospect> getPeopleForMigrations(String userId, ProspectSearch.Criteria orderBy, boolean isAscending,
                                                      int currentPage) {
        return getDatabase().withConnection(connection -> {
            int offset = (currentPage - 1) * AMOUNT_PER_PAGE;
            String personIdTablePrefix = PERSON_PROSPECT + ".";
            String peronStateTablePrefix = PERSON_STATE_TYPE + ".";
            String sql = "SELECT " + getSelectionForPerson(personIdTablePrefix, peronStateTablePrefix, false) + ", " +
                    "COUNT(*) OVER() AS TOTAL_COUNT " +
                    "FROM person_prospect " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type ON person_state_type.state_type = person_state.state " +
                    "WHERE (owner_id = ? OR receiver_id = ?) AND is_parent = ? " +
                    "GROUP BY " + getSelectionForPerson(personIdTablePrefix, peronStateTablePrefix, true) + " " +
                    "ORDER BY " + SearchDBAccessor.getTableNameFromPredicate(orderBy) + " " + (isAscending ? "ASC" : "DESC") + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + offset;
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, userId);
            statement.setBoolean(3, true);

            PagedList<Prospect> prospectsList = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                setMaxPage(prospectsList, resultSet);
                prospectsList.add(getPersonFromResultSet(resultSet, personIdTablePrefix, peronStateTablePrefix));
            }
            return prospectsList;
        });
    }

    public List<ContactStatusAuditItem> getPersonContactAuditTrail(String userId, String personId, String companyName) {
        Optional<Prospect> prospect = getPersonById(userId, personId);
        if (prospect == null || !prospect.isPresent()) {
            return null;
        }
        long dateCreated = prospect.get().getDateCreated();

        return getDatabase().withConnection(connection -> {
            // The first query in the union just gets the INSERTION date for the contact
            // language=PostgreSQL
            String sql = "SELECT " + getSelectionForContactStatusAuditItem(false) + " " +
                    "FROM person_prospect " +
                    "JOIN person_state ON person_state.person_id = person_prospect.person_id " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                    "JOIN dial_sheet ON dial_sheet_contact.sheet_id = dial_sheet.sheet_id " +
                    "JOIN _user ON dial_sheet.user_id = _user.user_id " +
                    "JOIN person_state_type ON dial_sheet_contact.contact_status = person_state_type.state_type " +
                    "JOIN company_person_states ON person_state_type.state_type = company_person_states.person_state " +
                    "WHERE dial_sheet_contact.person_id = ? AND " +
                    "company_person_states.company_name = ? AND " +
                    getPredicateForProspectByUserId() + " " +
                    "GROUP BY " + getSelectionForContactStatusAuditItem(true) + " " +
                    "ORDER BY contact_time ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, companyName);
            statement.setString(3, userId);
            statement.setString(4, userId);

            ResultSet resultSet = statement.executeQuery();

            List<ContactStatusAuditItem> contactStatusAuditItems = new ArrayList<>();
            contactStatusAuditItems.add(ContactStatusAuditItem.createInitialAuditTrailItem(dateCreated));

            while (resultSet.next()) {
                ProspectState state = ProspectStateDBAccessor.getProspectStateFromResultSet(resultSet, "");
                long date = resultSet.getTimestamp(CONTACT_TIME).getTime();
                String dialSheetId = resultSet.getString(SHEET_ID);

                String userTablePrefix = USER + ".";
                User userThatContactedProspect = UserDBAccessor.getUserFromResultSet(resultSet, userTablePrefix);
                contactStatusAuditItems.add(new ContactStatusAuditItem(state, date, dialSheetId, userThatContactedProspect));
            }

            return contactStatusAuditItems;
        });
    }

    public List<Appointment> getPersonAppointments(String userId, String personId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT " + AppointmentDBAccessor.getProjectionForAppointmentsWithTables() + " " +
                    "WHERE " + PERSON_PROSPECT + "." + PERSON_ID + " = ? " +
                    "AND " + DIAL_SHEET_APPOINTMENT + "." + USER_ID + " = ? " +
                    "GROUP BY " + AppointmentDBAccessor.getProjectionForAppointments(true) + " " +
                    "ORDER BY " + DIAL_SHEET_APPOINTMENT + "." + APPOINTMENT_DATE + " DESC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, userId);

            ResultSet resultSet = statement.executeQuery();

            PagedList<Appointment> appointments = new PagedList<>(-1, -1);
            appointments.setTotalNumberOfItems(10);
            return AppointmentDBAccessor.getAppointmentsFromResultSet(resultSet, -1, appointments);
        });
    }

    // Package-Private Methods

    static String getPredicateForProspectByUserId() {
        return "(" + PERSON_PROSPECT + "." + OWNER_ID + " = ? OR " + SHARES_PROSPECT + "." + RECEIVER_ID + " = ?)";
    }

    boolean setPersonState(Connection connection, String userId, String personId, ProspectState newState) throws SQLException {
        // we already know the person belongs to this user ID
        String sql = "UPDATE person_state SET state = ? WHERE person_id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, newState.getStateType());
        statement.setString(2, personId);

        statement.executeUpdate();

        DialSheetContactDBAccessor dialSheetContactDBAccessor = new DialSheetContactDBAccessor(getDatabase());
        if (!dialSheetContactDBAccessor.createDialSheetActivity(connection, newState, userId, personId, new java.util.Date())) {
            connection.rollback();
            return false;
        }

        connection.commit();
        return true;
    }

    // Private Methods

    private static String getSelectionForContactStatusAuditItem(boolean isGroupBy) {
        String personIdTablePrefix = PERSON_PROSPECT + ".";
        return getFormattedColumns(getSelectionForPerson(personIdTablePrefix, "", isGroupBy),
                CONTACT_TIME, DIAL_SHEET + "." + SHEET_ID, UserDBAccessor.getProjectionForUser(USER, isGroupBy));
    }

}
