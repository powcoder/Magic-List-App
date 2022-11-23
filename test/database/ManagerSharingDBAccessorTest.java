https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import controllers.BaseTestController;
import org.junit.Before;
import org.junit.Test;
import play.db.Database;
import utilities.ListUtility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ManagerSharingDBAccessorTest extends BaseTestController {

    private ManagerSharingDBAccessor managerSharingDBAccessor;

    /**
     * Jack
     */
    private static final String ORIGIN_USER_ID = "usr_s33IdHEiRVzwnPrBFIw4Tqede3reGGv9";

    /**
     * Carolyn
     */
    private static final String DESTINATION_USER_ID = "usr_n57zfMI7JRtbMWQKHb4fmRKDcm9OLn1v";

    /**
     * Julian
     */
    private static final String MANAGER_USER_ID = "usr_192dab24deb0ee0195431ca4bba52adb94065d66";

    @Before
    public void setUp() throws Exception {
        managerSharingDBAccessor = new ManagerSharingDBAccessor(app.injector().instanceOf(Database.class));
    }

    @Test
    public void transferAllContacts() throws Exception {
        try (Connection connection = managerSharingDBAccessor.getDatabase().getConnection()) {
            String sql;
            int originProspectCount;
            int originListCount;
            int originProspectInListCount;

            // language=PostgreSQL
            sql = "SELECT count(DISTINCT person_prospect.person_id) FROM person_prospect " +
                    "LEFT JOIN shares_prospect ON person_prospect.person_id = shares_prospect.person_id " +
                    "WHERE owner_id = ? OR receiver_id = ?";
            originProspectCount = getCountFromQuery(connection, sql, ListUtility.asList(ORIGIN_USER_ID, ORIGIN_USER_ID));

            // language=PostgreSQL
            sql = "SELECT count(DISTINCT search_result.search_id) " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "WHERE owner_id = ? OR receiver_id = ? " +
                    "GROUP BY search_result.search_id";
            originListCount = getCountFromQuery(connection, sql, ListUtility.asList(ORIGIN_USER_ID, ORIGIN_USER_ID));

            // language=PostgreSQL
            sql = "WITH owned_search_count AS (" +
                    "SELECT count(*) AS total FROM search_result " +
                    "JOIN searches ON search_result.search_id = searches.search_id WHERE owner_id = ?" +
                    "), " +
                    "shared_search_count AS (" +
                    "SELECT count(*) AS total FROM shares_search_result " +
                    "JOIN searches ON searches.search_id = searches.search_id WHERE receiver_id = ?" +
                    ") " +
                    "SELECT owned_search_count.total + shared_search_count.total " +
                    "FROM owned_search_count CROSS JOIN shared_search_count";
            originProspectInListCount = getCountFromQuery(connection, sql, ListUtility.asList(ORIGIN_USER_ID, ORIGIN_USER_ID));

            testTransferProspects(connection, null, originProspectCount, originListCount, originProspectInListCount);
        }
    }

    @Test
    public void transferList() throws Exception {
        try (Connection connection = managerSharingDBAccessor.getDatabase().getConnection()) {
            final String LIST_ID = "lst_AJTZifRAiqNzDpg7eHLf0mZil3SPaZGA";
            String sql;
            PreparedStatement statement;
            ResultSet resultSet;

            boolean isSuccessful = managerSharingDBAccessor.transferList(MANAGER_USER_ID, ORIGIN_USER_ID, DESTINATION_USER_ID, LIST_ID);
            assertTrue(isSuccessful);

            sql = "SELECT count(*) FROM person_prospect WHERE owner_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, DESTINATION_USER_ID);
            resultSet = statement.executeQuery();
            resultSet.next();
            int totalDestinationProspectCount = resultSet.getInt(1);

            assertEquals(5, totalDestinationProspectCount);
            DBAccessor.closeConnections(statement);

            sql = "SELECT count(*) FROM search_result WHERE owner_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, DESTINATION_USER_ID);
            resultSet = statement.executeQuery();
            resultSet.next();
            int totalSearchResults = resultSet.getInt(1);
            assertEquals(1, totalSearchResults);
            DBAccessor.closeConnections(statement);
        }
    }

    @Test
    public void transferSpecificContacts() throws Exception {
        try (Connection connection = managerSharingDBAccessor.getDatabase().getConnection()) {
            List<String> prospectIds = new ArrayList<>();
            int count;

            // language=PostgreSQL
            String sql = "SELECT * " +
                    "FROM person_prospect " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "WHERE owner_id = ? " +
                    "ORDER BY person_name " +
                    "LIMIT 100;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, ORIGIN_USER_ID);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                prospectIds.add(resultSet.getString(TablesContract._PersonProspect.PERSON_ID));
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < prospectIds.size(); i++) {
                builder.append("?");
                if (i < prospectIds.size() - 1) {
                    builder.append(", ");
                }
            }

            // Lists

            // language=PostgreSQL
            sql = "SELECT count(*) over() " +
                    "FROM search_result " +
                    "JOIN searches ON search_result.search_id = searches.search_id " +
                    "JOIN person_prospect ON  searches.person_id = person_prospect.person_id " +
                    "WHERE searches.person_id IN (" + builder.toString() + ") " +
                    "GROUP BY searches.search_id " +
                    "LIMIT 1;";
            statement = connection.prepareStatement(sql);
            count = 1;
            for (String prospectId : prospectIds) {
                statement.setString(count++, prospectId);
            }
            resultSet = statement.executeQuery();
            resultSet.next();
            int savedSearchListCount = resultSet.getInt(1);

            // Prospects in lists

            // language=PostgreSQL
            sql = "SELECT count(*) " +
                    "FROM searches " +
                    "WHERE searches.person_id IN (" + builder.toString() + ");";
            statement = connection.prepareStatement(sql);
            count = 1;
            for (String prospectId : prospectIds) {
                statement.setString(count++, prospectId);
            }
            resultSet = statement.executeQuery();
            resultSet.next();
            int prospectsInListsCount = resultSet.getInt(1);

            testTransferProspects(connection, prospectIds, prospectIds.size(), savedSearchListCount, prospectsInListsCount);
        }
    }

    private void testTransferProspects(Connection connection, List<String> prospectIdList, int prospectToTransferCount,
                                       int savedSearchToTransferCount, int prospectsInListsToTransferCount) throws SQLException {
        String sql;
        int destinationProspectCount;
        int destinationListCount;
        int destinationProspectInListCount;
        int newDestinationProspectCount;
        int newDestinationListCount;
        int newDestinationProspectInListCount;

        // language=PostgreSQL
        sql = "SELECT count(*) FROM shares_prospect WHERE receiver_id = ?";
        destinationProspectCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        // language=PostgreSQL
        sql = "SELECT count(*) FROM shares_search_result WHERE receiver_id = ?";
        destinationListCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        // language=PostgreSQL
        sql = "SELECT count(*) " +
                "FROM shares_search_result JOIN searches ON shares_search_result.search_id = searches.search_id " +
                "WHERE receiver_id = ?";
        destinationProspectInListCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        boolean isSuccessful;
        if (prospectIdList == null) {
            isSuccessful = managerSharingDBAccessor.transferAllProspectsAndLists(MANAGER_USER_ID, ORIGIN_USER_ID, DESTINATION_USER_ID);
            assertTrue(isSuccessful);
        } else {
            isSuccessful = managerSharingDBAccessor.transferProspectsAndTheirLists(MANAGER_USER_ID, ORIGIN_USER_ID, DESTINATION_USER_ID, prospectIdList);
            assertTrue(isSuccessful);
        }

        // language=PostgreSQL
        sql = "SELECT count(*) FROM shares_prospect WHERE receiver_id = ?";
        newDestinationProspectCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        // language=PostgreSQL
        sql = "SELECT count(*) FROM shares_search_result WHERE receiver_id = ?";
        newDestinationListCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        // language=PostgreSQL
        sql = "SELECT count(*) " +
                "FROM shares_search_result JOIN searches ON shares_search_result.search_id = searches.search_id " +
                "WHERE receiver_id = ?";
        newDestinationProspectInListCount = getCountFromQuery(connection, sql, ListUtility.asList(DESTINATION_USER_ID));

        assertEquals(prospectToTransferCount + destinationProspectCount, newDestinationProspectCount);
        assertEquals(savedSearchToTransferCount + destinationListCount, newDestinationListCount);
        assertEquals(prospectsInListsToTransferCount + destinationProspectInListCount, newDestinationProspectInListCount);
    }

    private int getCountFromQuery(Connection connection, String sql, List<String> parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        int count = 1;
        for (String s : parameters) {
            statement.setString(count++, s);
        }
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        return resultSet.getInt(1);
    }

}