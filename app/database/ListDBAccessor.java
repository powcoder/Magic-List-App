https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.account.Account;
import model.ImporterLimit;
import model.PagedList;
import model.lists.ProspectSearch;
import model.prospect.Prospect;
import model.lists.SavedList;
import model.database.SavedListDatabaseResult;
import model.user.User;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._SearchResult.*;
import static database.TablesContract._Searches.SEARCH_ID;
import static database.TablesContract._SharesSearchResult.*;
import static database.TablesContract._User.*;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ListDBAccessor extends DBAccessor {

    private static final Logger.ALogger logger = Logger.of(ListDBAccessor.class);

    private static final int AMOUNT_PER_PAGE = 50;

    private final ImporterDBAccessor importerDBAccessor;

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public ListDBAccessor(Database database) {
        super(database);
        importerDBAccessor = new ImporterDBAccessor(database);
    }

    public SavedListDatabaseResult insertListFromPredicates(Account account, SavedList savedList, ProspectSearch search) {
        return getDatabase().withConnection(false, connection -> {
            // Search Results
            SavedListDatabaseResult result = insertSavedList(connection, account.getUserId(), savedList);
            if (result != SavedListDatabaseResult.SUCCESS) {
                return result;
            }

            String sql = SearchDBAccessor.buildQueryForPersonIdFromSearchPredicates(search);
            PreparedStatement statement = connection.prepareStatement(sql);

            int counter = 1;
            statement.setString(counter++, savedList.getListId());
            SearchDBAccessor.bindParametersToSearchQuery(statement, account.getUserId(), search, counter);

            if(statement.executeUpdate() > 0) {
                connection.commit();
                return SavedListDatabaseResult.SUCCESS;
            } else {
                return SavedListDatabaseResult.UNKNOWN_ERROR;
            }
        });
    }

    public SavedListDatabaseResult insertListFromImporter(Account account, SavedList savedList, List<Prospect> prospectList) {
        return getDatabase().withConnection(false, connection -> {
            if (!account.isAdmin()) {
                ImporterLimit importerLimit = importerDBAccessor.getImporterLimit(connection, account.getUserId());
                if (importerLimit == null) {
                    logger.error("Could not retrieve limits for account: {}", account.getUserId());
                    return SavedListDatabaseResult.UNKNOWN_ERROR;
                }

                int dailyTotal = importerLimit.getDailyTotal() + prospectList.size();
                int monthlyTotal = importerLimit.getMonthlyTotal() + prospectList.size();
                if (dailyTotal > importerLimit.getDailyMax() || monthlyTotal > importerLimit.getMonthlyMax()) {
                    return SavedListDatabaseResult.ERROR_TOO_MANY_IMPORTS;
                }
            }

            boolean didAddToImportCount =
                    importerDBAccessor.addImportCount(connection, account.getUserId(), prospectList.size());
            if (!didAddToImportCount) {
                throw new SQLException("Could not update import count");
            }

            SavedListDatabaseResult result = insertPersonList(connection, account.getUserId(), savedList, prospectList);
            if (result == SavedListDatabaseResult.SUCCESS) {
                connection.commit();
                return SavedListDatabaseResult.SUCCESS;
            } else {
                return SavedListDatabaseResult.UNKNOWN_ERROR;
            }
        });
    }

    public SavedListDatabaseResult insertListFromWebUpload(Account account, SavedList savedList, List<Prospect> prospectList) {
        return getDatabase().withConnection(false, connection -> {
            SavedListDatabaseResult result = insertPersonList(connection, account.getUserId(), savedList, prospectList);
            if (result == SavedListDatabaseResult.SUCCESS) {
                connection.commit();
                return SavedListDatabaseResult.SUCCESS;
            } else {
                return result;
            }
        });
    }

    public Optional<Boolean> deleteList(String userId, String searchId) {
        return getDatabase().withConnection(connection -> {
            String sql = "DELETE FROM search_result WHERE owner_id = ? AND search_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, searchId);
            return Optional.of(statement.executeUpdate() == 1);
        });
    }

    public boolean setListName(String userId, String searchId, String searchName) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            // language=PostgreSQL
            String sql = "UPDATE search_result SET search_name = ? " +
                    "WHERE search_id = (" +
                    "SELECT search_result.search_id " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "WHERE " + getPredicateForListByUserId() + " AND " +
                    "search_result.search_id = ? " +
                    "GROUP BY search_result.search_id" +
                    ");";
            statement = connection.prepareStatement(sql);
            statement.setString(1, searchName);
            statement.setString(2, userId);
            statement.setString(3, userId);
            statement.setString(4, searchId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public Optional<Boolean> setListComments(String userId, String searchId, String comment) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE search_result " +
                    "SET search_comment = ? " +
                    "WHERE search_id = (" +
                    "SELECT search_result.search_id " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "WHERE " + getPredicateForListByUserId() + " AND " +
                    "search_result.search_id = ? " +
                    "GROUP BY search_result.search_id" +
                    ");";
            statement = connection.prepareStatement(sql);

            setStringOrNull(statement, 1, comment);
            statement.setString(2, userId);
            statement.setString(3, userId);
            statement.setString(4, searchId);
            return Optional.of(statement.executeUpdate() == 1);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement);
        }
    }

    public PagedList<SavedList> getUserLists(String userId, int currentPage, SavedList.Sorter sorter, boolean isAscending) {
        return getDatabase().withConnection(connection -> {
            int offset = AMOUNT_PER_PAGE * (currentPage - 1);
            String orderBy = convertSortByToColumn(sorter);
            String sortBy = isAscending ? " ASC " : " DESC ";
            // language=PostgreSQL
            String sql = "SELECT *, " +
                    "count(*) OVER() " + TOTAL_COUNT + " " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "LEFT JOIN _user ON shares_search_result.sharer_id = _user.user_id " +
                    "WHERE " + getPredicateForListByUserId() + " " +
                    "ORDER BY " + orderBy + " " + sortBy + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + offset;
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, userId);
            ResultSet resultSet = statement.executeQuery();

            PagedList<SavedList> savedLists = new PagedList<>(currentPage, AMOUNT_PER_PAGE);

            while (resultSet.next()) {
                setMaxPage(savedLists, resultSet);
                savedLists.add(getSavedSearchListFromResultSet(resultSet));
            }
            return savedLists;
        });
    }

    public SavedList getUserListById(String userId, String listId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT * " +
                    "FROM search_result " +
                    "LEFT JOIN shares_search_result ON search_result.search_id = shares_search_result.search_id " +
                    "FULL JOIN _user ON shares_search_result.sharer_id = _user.user_id " +
                    "WHERE " + getPredicateForListByUserId() + " AND search_result.search_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, userId);
            statement.setString(3, listId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return getSavedSearchListFromResultSet(resultSet);
            } else {
                return null;
            }
        });
    }

    public boolean mergeLists(String newListId, String listName, String userId, List<String> listIdList) {
        return getDatabase().withConnection(false, (connection) -> {
            String sql = "INSERT INTO search_result(search_id, search_name, search_date, search_comment, owner_id) " +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, newListId);
            statement.setString(2, listName);
            statement.setLong(3, Calendar.getInstance().getTimeInMillis());
            statement.setNull(4, Types.VARCHAR);
            statement.setString(5, userId);

            if (statement.executeUpdate() == 1) {
                Logger.debug("Successfully inserted the new list");
            } else {
                Logger.error("There was an error inserting the new list", new IllegalStateException());
                connection.rollback();
                return false;
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < listIdList.size(); i++) {
                //language=PostgreSQL
                builder.append("SELECT ?, person_id FROM searches WHERE search_id = ?");
                if (i < listIdList.size() - 1) {
                    builder.append(" UNION ");
                }
            }

            sql = "INSERT INTO searches (search_id, person_id) " + "(" + builder.toString() + ");";
            statement = connection.prepareStatement(sql);

            int counter = 1;
            for (String oldListId : listIdList) {
                statement.setString(counter++, newListId);
                statement.setString(counter++, oldListId);
            }

            if (statement.executeUpdate() >= 0) {
                Logger.debug("Successfully inserted the list\'s contacts");
            } else {
                Logger.error("There was an error inserting the list\'s contacts", new IllegalStateException());
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        });
    }

    // Private Methods

    private static String convertSortByToColumn(SavedList.Sorter sorter) {
        switch (sorter) {
            case SEARCH_NAME:
                return SEARCH_NAME;
            case DATE_CREATED:
                return SEARCH_DATE;
            default:
                logger.error("Invalid Sorter!", new IllegalArgumentException("Invalid argument, found: " + sorter));
                return null;
        }
    }

    private SavedListDatabaseResult insertPersonList(Connection connection, String userId, SavedList savedList,
                                                     List<Prospect> prospectList) throws SQLException {
        // Search Results
        SavedListDatabaseResult result = insertSavedList(connection, userId, savedList);
        if (result != SavedListDatabaseResult.SUCCESS) {
            return result;
        }

        // Person
        String personBuilderParameters = getQuestionMarkParametersForList(prospectList, "(?, ?, ?, ?, ?, ?, ?)");
        String sql = "INSERT INTO person_prospect (person_id, person_name, person_company_name, job_title, person_email, " +
                "person_phone, owner_id) VALUES " +
                personBuilderParameters + " ON CONFLICT (person_id) DO NOTHING;";

        PreparedStatement statement = connection.prepareStatement(sql);

        int counter = 1;
        for (Prospect person : prospectList) {
            statement.setString(counter++, person.getId());
            statement.setString(counter++, person.getName());
            setStringOrNull(statement, counter++, person.getCompanyName());
            setStringOrNull(statement, counter++, person.getJobTitle());
            setStringOrNull(statement, counter++, person.getEmail());
            setStringOrNull(statement, counter++, person.getPhoneNumber());
            setStringOrNull(statement, counter++, userId);
        }

        statement.executeUpdate();

        String searchIdParameters = getQuestionMarkParametersForList(prospectList, "?");
        sql = "INSERT INTO searches (search_id, person_id) " +
                "SELECT ?, person_id " +
                "FROM person_prospect " +
                "WHERE person_id IN (" + searchIdParameters + ");";
        statement = connection.prepareStatement(sql);

        counter = 1;
        statement.setString(counter++, savedList.getListId());
        for (Prospect prospect : prospectList) {
            statement.setString(counter++, prospect.getId());
        }

        if (statement.executeUpdate() == 0) {
            logger.error("Could not insert searches: ", new SQLException());
            return SavedListDatabaseResult.UNKNOWN_ERROR;
        }

        // Person State
        String personStateParameters = getQuestionMarkParametersForList(prospectList, "(?, ?, ?)");
        sql = "INSERT INTO person_state (person_id, state, notes) VALUES " +
                personStateParameters + " ON CONFLICT (person_id) DO NOTHING;";

        statement = connection.prepareStatement(sql);

        counter = 1;
        for (Prospect person : prospectList) {
            statement.setString(counter++, person.getId());
            statement.setString(counter++, person.getState().getStateType());
            setStringOrNull(statement, counter++, person.getNotes());
        }

        statement.executeUpdate();

        return SavedListDatabaseResult.SUCCESS;
    }

    private SavedListDatabaseResult insertSavedList(Connection connection, String userId, SavedList savedList) throws SQLException {
        String sql = "INSERT INTO search_result (search_id, search_name, search_date, search_comment, owner_id) " +
                "VALUES (?, ?, ?, ?, ?);";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, savedList.getListId());
        statement.setString(2, savedList.getListName());
        statement.setLong(3, savedList.getRawSearchDate());
        setStringOrNull(statement, 4, savedList.getComment());
        statement.setString(5, userId);
        if (statement.executeUpdate() != 1) {
            logger.error("Error inserting into search_result: ", new SQLException("Could not insert list!"));
            return SavedListDatabaseResult.UNKNOWN_ERROR;
        } else {
            return SavedListDatabaseResult.SUCCESS;
        }
    }

    private static SavedList getSavedSearchListFromResultSet(ResultSet resultSet) throws SQLException {
        String searchId = resultSet.getString(SEARCH_ID);
        String ownerId = resultSet.getString(OWNER_ID);
        String searchName = resultSet.getString(SEARCH_NAME);
        long searchDate = resultSet.getLong(SEARCH_DATE);
        String comment = resultSet.getString(SEARCH_COMMENT);
        long shareDate = Optional.ofNullable(resultSet.getTimestamp(DATE_SHARED))
                .orElseGet(() -> new Timestamp(-1))
                .getTime();

        User sharer = null;
        String sharerId = resultSet.getString(SHARER_ID);
        if (sharerId != null) {
            String sharerEmail = resultSet.getString(EMAIL);
            String sharerName = resultSet.getString(NAME);
            long sharerJoinDate = resultSet.getDate(JOIN_DATE).getTime();
            sharer = new User(sharerId, sharerEmail, sharerName, sharerJoinDate);
        }

        return new SavedList(searchId, ownerId, searchName, searchDate, comment, sharer, shareDate);
    }

    private static String getPredicateForListByUserId() {
        return "(" + OWNER_ID + " = ? OR " + RECEIVER_ID + " = ?)";
    }

}
