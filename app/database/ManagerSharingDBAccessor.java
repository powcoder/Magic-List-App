https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.*;

import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._SearchResult.*;
import static database.TablesContract._Searches.PERSON_ID;
import static database.TablesContract._Searches.SEARCH_ID;

/**
 *
 */
public class ManagerSharingDBAccessor extends ProspectDBAccessor {

    public enum SharingResult {
        SUCCESS, FAILURE;
    }

    private final Logger.ALogger logger = Logger.of(this.getClass());

    public ManagerSharingDBAccessor(Database database) {
        super(database);
    }

    /**
     * Transfers the list of prospects but <b>not</b> the lists they are in
     */
    public boolean transferProspects(String managerId, String originUserId, String destinationUserId,
                                     List<String> prospectIdsToTransfer) {
        Connection connection = null;
        try {
            connection = getDatabase().getConnection();
            connection.setAutoCommit(false);

            // Insert prospects into the destination's database
            insertPersonProspectList(connection, originUserId, destinationUserId, prospectIdsToTransfer);

            connection.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            rollbackQuietly(connection);
            return false;
        } finally {
            enableAutoCommitQuietly(connection);
            closeConnections(connection);
        }
    }

    public boolean transferList(String managerId, String originUserId, String destinationUserId,
                                String originListIdToTransfer) {
        Connection connection = null;
        try {
            connection = getDatabase().getConnection();

            List<String> savedSearchIdList = getUnsharedSavedSearchLists(connection, originUserId, destinationUserId,
                    managerId, Collections.singletonList(originListIdToTransfer));
            if (savedSearchIdList.isEmpty()) {
                return true;
            }

            insertSavedLists(connection, originUserId, destinationUserId, savedSearchIdList);

            List<String> prospectIdsFromList = getOriginProspectIdsFromListId(connection, originUserId, destinationUserId,
                    managerId, originListIdToTransfer);

            if (!prospectIdsFromList.isEmpty()) {
                insertPersonProspectList(connection, originUserId, destinationUserId, prospectIdsFromList);
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection);
        }
    }

    public boolean transferAllProspectsAndLists(String managerId, String originUserId, String destinationUserId) {
        return transferProspectsAndTheirLists(managerId, originUserId, destinationUserId, null);
    }

    /**
     * Transfers the prospects in the given list and all of the lists in which these prospects are. If the prospect IDs
     * are null, it will transfer <b>ALL</b> prospects and lists.
     *
     * @param managerId             The ID of the manager of the two users
     * @param originUserId          The user who is sending the prospects
     * @param destinationUserId     The user receiving the prospects
     * @param prospectIdsToTransfer The IDs of the sender's prospects to send (which will send their corresponding
     *                              lists too) or null to transfer <b>ALL</b> off them
     * @return True if successful, false otherwise
     */
    public boolean transferProspectsAndTheirLists(String managerId, String originUserId, String destinationUserId,
                                                  List<String> prospectIdsToTransfer) {
        Connection connection = null;
        try {
            connection = getDatabase().getConnection();
            connection.setAutoCommit(false);

            logger.info("Starting a transfer between [origin: {}, receiver: {}, manager: {}]", originUserId, destinationUserId, managerId);

            // Get the original prospects
            List<String> unsharedProspects = getUnsharedProspects(connection, originUserId, destinationUserId,
                    managerId, prospectIdsToTransfer);

            if (unsharedProspects.isEmpty()) {
                // There are no new prospects to share
                logger.info("There are no prospects to insert from {} to {}", originUserId, destinationUserId);
            } else {
                // Insert prospects into the destination's database
                logger.info("Transferring {} contacts from {} to {}", unsharedProspects.size(), originUserId, destinationUserId);
                insertPersonProspectList(connection, originUserId, destinationUserId, unsharedProspects);
            }

            // Get the original saved lists
            List<String> originSavedSearchList = getUnsharedSavedSearchLists(connection, originUserId,
                    destinationUserId, managerId, null);

            if (originSavedSearchList.isEmpty()) {
                // There are no lists to insert
                logger.debug(String.format("There are no lists to insert from %s to %s", originUserId, destinationUserId));
                connection.commit();
                return true;
            }

            // Insert the saved lists into the destination's database
            insertSavedLists(connection, originUserId, destinationUserId, originSavedSearchList);

            connection.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            rollbackQuietly(connection);
            return false;
        } finally {
            enableAutoCommitQuietly(connection);
            closeConnections(connection);
        }
    }

    // Private Methods

    /**
     * Gets all of the prospects that have <b>NOT</b> been shared yet with the user.
     * <br>
     * This list may be empty if the user has already shared the contacts, the user is not a co-worker of the
     * destination user ID, or the prospect IDs are invalid.
     *
     * @param prospectIdsToTransfer The list of IDs to transfer or NULL/empty if all should be transferred
     */
    private List<String> getUnsharedProspects(Connection connection, String originUserId, String destinationUserId,
                                              String managerId, List<String> prospectIdsToTransfer) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql;
            // Get all prospects that are accessible by origin, are coworkers with the destination, and have not been shared
            // to the destination
            sql = "SELECT person_prospect.person_id " +
                    "FROM person_prospect " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type ON person_state.state = person_state_type.state_type " +
                    "WHERE (owner_id = ? OR shares_prospect.receiver_id = ?) " + // owner
                    "AND person_prospect.person_id NOT IN (" +
                    "   SELECT person_id FROM shares_prospect WHERE receiver_id = ? " + //destination
                    "   UNION " +
                    "   SELECT person_id FROM person_prospect WHERE owner_id = ? " + //destination
                    ") " +
                    "AND EXISTS (" +
                    getSqlClauseForUsersBeingCoworkers() +
                    ") ";

            if (prospectIdsToTransfer != null && prospectIdsToTransfer.size() > 0) {
                String personIdParams = getQuestionMarkParametersForList(prospectIdsToTransfer, "?");

                sql += " AND (" + PERSON_PROSPECT + "." + PERSON_ID + " IN (" + personIdParams + "))";
            }

            sql += " GROUP BY " + PERSON_PROSPECT + "." + PERSON_ID;

            int statementOffset = 1;
            statement = connection.prepareStatement(sql);

            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset++, destinationUserId);
            statement.setString(statementOffset++, destinationUserId);
            statement.setString(statementOffset++, managerId);
            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset++, destinationUserId);

            if (prospectIdsToTransfer != null) {
                for (String prospectId : prospectIdsToTransfer) {
                    statement.setString(statementOffset++, prospectId);
                }
            }

            resultSet = statement.executeQuery();

            List<String> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(resultSet.getString(PERSON_ID));
            }

            return list;
        } finally {
            closeConnections(statement, resultSet);
        }
    }

    /**
     * @return A list of prospects in a given list
     */
    private List<String> getOriginProspectIdsFromListId(Connection connection, String originUserId, String destinationUserId,
                                                        String managerId, String listId) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT person_prospect.person_id " +
                    "FROM person_prospect " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type ON person_state.state = person_state_type.state_type " +
                    "JOIN searches ON person_prospect.person_id = searches.person_id " +
                    "JOIN search_result ON searches.search_id = search_result.search_id " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +

                    "WHERE (person_prospect.owner_id = ? OR shares_prospect.receiver_id = ?) " +
                    "AND (searches.search_id = ? OR shares_search_result.receiver_id = ?) " +
                    "AND EXISTS (" +
                    getSqlClauseForUsersBeingCoworkers() +
                    ");";

            int statementOffset = 1;
            statement = connection.prepareStatement(sql);
            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset++, listId);
            statement.setString(statementOffset++, listId);
            statement.setString(statementOffset++, managerId);
            statement.setString(statementOffset++, originUserId);
            statement.setString(statementOffset, destinationUserId);

            resultSet = statement.executeQuery();

            List<String> prospectIds = new ArrayList<>();
            while (resultSet.next()) {
                prospectIds.add(resultSet.getString(PERSON_ID));
            }
            return prospectIds;
        } finally {
            closeConnections(statement, resultSet);
        }
    }

    private void insertPersonProspectList(Connection connection, String originUserId, String destinationUserId,
                                          List<String> originalProspectList) throws SQLException {
        int offset = 2500;
        for (int i = 0; i < originalProspectList.size(); i += offset) {
            // Execute the update in groups of max(2500)
            int endIndexExclusive = i + offset;
            endIndexExclusive = endIndexExclusive > originalProspectList.size() ? originalProspectList.size() : endIndexExclusive;

            List<String> prospectListToInsert = originalProspectList.subList(i, endIndexExclusive);
            String sharesProspectParameters = getQuestionMarkParametersForList(prospectListToInsert, "(?, ?, ?)");

            PreparedStatement statement = null;
            try {
                String sql = "INSERT INTO shares_prospect (person_id, sharer_id, receiver_id) VALUES " +
                        sharesProspectParameters + " ON CONFLICT DO NOTHING ";
                statement = connection.prepareStatement(sql);

                int counter = 0;
                for (String prospectId : prospectListToInsert) {
                    statement.setString(++counter, prospectId);
                    statement.setString(++counter, originUserId);
                    statement.setString(++counter, destinationUserId);
                }
                statement.executeUpdate();
            } finally {
                closeConnections(statement);
            }
        }
    }

    /**
     * @return All of the origin user's saved lists that have <b>NOT</b> yet been shared to the receiver.
     */
    private List<String> getUnsharedSavedSearchLists(Connection connection, String originUserId,
                                                     String destinationUserId, String managerId,
                                                     List<String> listIds) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            // select all of the lists that have not yet been shared to the receiver and the sharer wasn't the receiver at one point
            String sql = "SELECT search_result.search_id " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "WHERE (search_result.owner_id = ? OR shares_search_result.receiver_id = ?) AND " +
                    "search_result.search_id NOT IN (" +
                    "   SELECT shares_search_result.search_id FROM shares_search_result WHERE receiver_id = ? " +
                    "   UNION " +
                    "   SELECT search_result.search_id FROM search_result WHERE owner_id = ? " +
                    ") AND " +
                    "EXISTS (" + getSqlClauseForUsersBeingCoworkers() + ") ";
            if (listIds != null && !listIds.isEmpty()) {
                String parameters = getQuestionMarkParametersForList(listIds, "?");
                sql += " AND " + SEARCH_RESULT + "." + SEARCH_ID + " IN (" + parameters + ") ";
            }

            sql += "GROUP BY " + SEARCH_RESULT + "." + SEARCH_ID;

            int offset = 1;
            statement = connection.prepareStatement(sql);
            statement.setString(offset++, originUserId);
            statement.setString(offset++, originUserId);
            statement.setString(offset++, destinationUserId);
            statement.setString(offset++, destinationUserId);
            statement.setString(offset++, managerId);
            statement.setString(offset++, originUserId);
            statement.setString(offset++, destinationUserId);

            if (listIds != null && !listIds.isEmpty()) {
                for (String listId : listIds) {
                    statement.setString(offset++, listId);
                }
            }

            resultSet = statement.executeQuery();

            List<String> originSavedSearchList = new ArrayList<>();
            while (resultSet.next()) {
                originSavedSearchList.add(resultSet.getString(SEARCH_ID));
            }
            return originSavedSearchList;
        } finally {
            closeConnections(statement, resultSet);
        }
    }

    private void insertSavedLists(Connection connection, String originUserId, String destinationUserId,
                                  List<String> originSavedSearchList) throws SQLException {
        String parameters = getQuestionMarkParametersForList(originSavedSearchList, "(?, ?, ?)");
        String sql = "INSERT INTO shares_search_result(search_id, sharer_id, receiver_id) VALUES " + parameters;
        PreparedStatement statement = connection.prepareStatement(sql);

        int counter = 1;
        for (String originSavedSearchId : originSavedSearchList) {
            statement.setString(counter++, originSavedSearchId);
            statement.setString(counter++, originUserId);
            statement.setString(counter++, destinationUserId);
        }

        statement.executeUpdate();
    }

    /**
     * @return A SQL string that has three parameters (in the following order) - the manager ID, emp 1 and emp 2.
     */
    private static String getSqlClauseForUsersBeingCoworkers() {
        // language=PostgreSQL
        return "SELECT manager_id FROM manages " +
                "WHERE manager_id = ? AND employee_id IN (?, ?)" +
                "GROUP BY manager_id " +
                "HAVING count(manager_id) > 1";
    }

}
