https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import cache.DialSheetCache;
import model.dialsheet.DialSheet;
import model.prospect.ProspectState;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.Date;

/**
 * Created by Corey Caplan on 9/6/17.
 */
public class DialSheetContactDBAccessor extends AbstractPersonDatabase {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    private final DialSheetDBAccessor dialSheetDBAccessor;

    public DialSheetContactDBAccessor(Database database) {
        super(database);
        dialSheetDBAccessor = new DialSheetDBAccessor(getDatabase());
    }

    /**
     * @return The dial sheet associated with this new contact time, after the changes were applied or null if an error
     * occurred.
     */
    public DialSheet editDialSheetContactTime(String userId, String personId, String originalSheetId, long newContactTime) {
        return getDatabase().withConnection(false, connection -> {
            DialSheet newDialSheet = dialSheetDBAccessor.getDialSheetByDateAndCreateIfNotExists(connection, userId, new Date(newContactTime));
            if (newDialSheet == null) {
                logger.error("Could not get/create new activity sheet for user: {}", userId);
                connection.rollback();
                return null;
            }

            String sql = "SELECT contact_status " +
                    "FROM dial_sheet_contact " +
                    "WHERE person_id = ? AND sheet_id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, originalSheetId);
            ResultSet resultSet = statement.executeQuery();

            String contactStatus;
            if (resultSet.next()) {
                contactStatus = resultSet.getString(1);
            } else {
                logger.error("Could not get contact status for user: {}", userId);
                connection.rollback();
                return null;
            }

            sql = "DELETE FROM dial_sheet_contact WHERE person_id = ? AND sheet_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, originalSheetId);

            if (statement.executeUpdate() != 1) {
                logger.error("Could not delete old contact for user: {}", userId);
                connection.rollback();
                return null;
            }

            sql = "INSERT INTO dial_sheet_contact(sheet_id, person_id, contact_time, contact_status) " +
                    "VALUES (?, ?, ?, ?) ON CONFLICT (sheet_id, person_id) DO UPDATE SET " +
                    "contact_status = EXCLUDED.contact_status, " +
                    "contact_time = EXCLUDED.contact_time";
            statement = connection.prepareStatement(sql);
            statement.setString(1, newDialSheet.getId());
            statement.setString(2, personId);
            statement.setTimestamp(3, new Timestamp(newContactTime));
            statement.setString(4, contactStatus);

            if (statement.executeUpdate() != 1) {
                logger.error("Could not edit contact time for user: {}", userId);
                connection.rollback();
                return null;
            }

            DialSheetCache.removeCurrentDialSheet(userId);

            connection.commit();
            return newDialSheet;
        });
    }

    public boolean updateMostRecentDialSheetActivityStatus(Connection connection, ProspectState newState, String userId,
                                                           String personId) throws SQLException {
        PreparedStatement statement;
        if (ProspectState.NOT_CONTACTED.getStateType().equals(newState.getStateType())) {
            String sql = "DELETE FROM dial_sheet_contact WHERE person_id = ? AND sheet_id = (" +
                    "SELECT sheet_id " +
                    "FROM dial_sheet " +
                    "WHERE user_id = ? " +
                    "ORDER BY dial_date DESC " +
                    "LIMIT 1 " +
                    ")";
            statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, userId);
        } else {
            String sql = "UPDATE dial_sheet_contact SET contact_status = ? WHERE person_id = ? AND sheet_id = (" +
                    "SELECT sheet_id " +
                    "FROM dial_sheet " +
                    "WHERE user_id = ? " +
                    "ORDER BY dial_date DESC " +
                    "LIMIT 1 " +
                    ")";
            statement = connection.prepareStatement(sql);
            statement.setString(1, newState.getStateType());
            statement.setString(2, personId);
            statement.setString(3, userId);
        }

        statement.executeUpdate();

        return true;
    }

    // Package-Private methods

    boolean updateDialSheetActivityStatus(Connection connection, ProspectState newState, String userId,
                                          String personId, String dialSheetId) throws SQLException {
        PreparedStatement statement;
        if (ProspectState.NOT_CONTACTED.getStateType().equals(newState.getStateType())) {
            String sql = "DELETE FROM dial_sheet_contact WHERE sheet_id = ? AND person_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, dialSheetId);
            statement.setString(2, personId);
        } else {
            String sql = "UPDATE dial_sheet_contact SET contact_status = ? WHERE sheet_id = ? AND person_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, newState.getStateType());
            statement.setString(2, dialSheetId);
            statement.setString(3, personId);
        }

        if (statement.executeUpdate() != 1) {
            logger.error("Could not update activity sheet time for user: {}", userId);
            return false;
        } else {
            return true;
        }
    }

    boolean createDialSheetActivity(Connection connection, ProspectState newState, String userId,
                                    String personId, Date contactTime) throws SQLException {
        DialSheetDBAccessor dialSheetDBAccessor = new DialSheetDBAccessor(getDatabase());
        DialSheet dialSheet = dialSheetDBAccessor.getDialSheetByDateAndCreateIfNotExists(connection, userId, contactTime);

        if (dialSheet == null) {
            logger.error("Could not create dial sheet for user: {}", userId);
            return false;
        }

        if (!ProspectState.NOT_CONTACTED.getStateType().equals(newState.getStateType())) {
            if (!createDialSheetContact(connection, personId, dialSheet.getId(),
                    contactTime, newState.getStateType())) {
                logger.error("Could not update contact status for activity sheet: [user_id: {}]", userId);
                connection.rollback();
                return false;
            }
        } else {
            if (!deleteDialSheetContact(connection, userId, personId, contactTime)) {
                logger.error("Could not delete contact status for activity sheet: [user_id: {}]", userId);
                connection.rollback();
                return false;
            }
        }

        DialSheetCache.removeCurrentDialSheet(userId);

        return true;
    }

    // Private Methods

    private boolean createDialSheetContact(Connection connection, String personId, String sheetId,
                                           Date contactTime, String contactStatus) {
        PreparedStatement statement = null;
        try {
            String sql = "INSERT INTO dial_sheet_contact(sheet_id, person_id, contact_status, contact_time) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT ON CONSTRAINT dial_sheet_contact_sheet_id_person_id_pk DO UPDATE SET " +
                    "contact_status = EXCLUDED.contact_status, " +
                    "contact_time = EXCLUDED.contact_time";
            statement = connection.prepareStatement(sql);
            statement.setString(1, sheetId);
            statement.setString(2, personId);
            statement.setString(3, contactStatus);
            statement.setTimestamp(4, new Timestamp(contactTime.getTime()));

            return statement.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(statement);
        }
    }

    private boolean deleteDialSheetContact(Connection connection, String userId, String personId, Date date) {
        PreparedStatement statement = null;
        try {
            String sql = "DELETE FROM dial_sheet_contact " +
                    "WHERE sheet_id = (SELECT sheet_id FROM dial_sheet WHERE user_id = ? AND dial_date = ? :: DATE) " +
                    "AND person_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setDate(2, new java.sql.Date(date.getTime()));
            statement.setString(3, personId);

            return statement.executeUpdate() <= 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(statement);
        }
    }

}
