https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.account.Account;
import model.account.AccountNotification;
import model.account.MessageNotification;
import model.user.BugReport;
import model.user.UserQuote;
import model.user.Suggestion;
import model.user.User;
import play.Logger;
import play.db.Database;
import utilities.RandomStringGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._BugReport.*;
import static database.TablesContract._User.*;
import static database.TablesContract._UserQuote.*;
import static database.TablesContract._UserSuggestion.*;
import static database.UserDBAccessor.getUserFromResultSet;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class AdminDBAccessor extends DBAccessor {

    private static final long ONE_WEEK_MILLIS = 1000 * 60 * 60 * 24 * 7;

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public AdminDBAccessor(Database database) {
        super(database);
    }

    public List<String> getAllCompanyNames() {
        return getDatabase().withConnection((connection -> {
            String sql = "SELECT company_name " +
                    "FROM company_model";
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            List<String> companyNames = new ArrayList<>();
            while (resultSet.next()) {
                companyNames.add(resultSet.getString(1));
            }

            return companyNames;
        }));
    }

    public List<Suggestion> getSuggestions() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT suggestion_id, suggestion_date, text, _user.user_id AS user_id, name, email, join_date " +
                    "FROM user_suggestion JOIN _user ON user_suggestion.user_id = _user.user_id " +
                    "ORDER BY suggestion_date DESC";
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();

            List<Suggestion> suggestionList = new ArrayList<>();
            while (resultSet.next()) {
                String suggestionId = resultSet.getString(SUGGESTION_ID);
                long suggestionDate = resultSet.getTimestamp(SUGGESTION_DATE).getTime();
                String text = resultSet.getString(SUGGESTION_TEXT);

                String userId = resultSet.getString(USER_ID);
                String name = resultSet.getString(NAME);
                String email = resultSet.getString(EMAIL);
                long joinDate = resultSet.getDate(JOIN_DATE).getTime();
                suggestionList.add(new Suggestion(suggestionId, new User(userId, email, name, joinDate), text, suggestionDate));
            }
            return suggestionList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<BugReport> getBugReports() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT bug_id, bug_date, text, _user.user_id AS user_id, name, email, join_date " +
                    "FROM bug_report JOIN _user ON bug_report.user_id = _user.user_id " +
                    "ORDER BY bug_date DESC";
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();

            List<BugReport> bugList = new ArrayList<>();
            while (resultSet.next()) {
                String bugId = resultSet.getString(BUG_ID);
                long bugDate = resultSet.getTimestamp(BUG_DATE).getTime();
                String text = resultSet.getString(BUG_TEXT);

                String userId = resultSet.getString(USER_ID);
                String name = resultSet.getString(NAME);
                String email = resultSet.getString(EMAIL);
                long joinDate = resultSet.getDate(JOIN_DATE).getTime();
                bugList.add(new BugReport(bugId, new User(userId, email, name, joinDate), text, bugDate));
            }
            return bugList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<UserQuote> getUserQuotes() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT quote_id, quote_date, author, text, _user.user_id AS user_id, name, email, join_date " +
                    "FROM user_quote JOIN _user ON user_quote.user_id = _user.user_id " +
                    "ORDER BY quote_date DESC";
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();

            List<UserQuote> userQuoteList = new ArrayList<>();
            while (resultSet.next()) {
                String quoteId = resultSet.getString(QUOTE_ID);
                long quoteDate = resultSet.getTimestamp(QUOTE_DATE).getTime();
                String author = resultSet.getString(QUOTE_AUTHOR);
                String text = resultSet.getString(QUOTE_TEXT);

                String userId = resultSet.getString(USER_ID);
                String name = resultSet.getString(NAME);
                String email = resultSet.getString(EMAIL);
                long joinDate = resultSet.getDate(JOIN_DATE).getTime();
                userQuoteList.add(new UserQuote(quoteId, new User(userId, email, name, joinDate), text, author, quoteDate));
            }
            return userQuoteList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * @param userId The ID of the user to get
     */
    public User getUser(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT user_id, name, email, join_date FROM _user WHERE user_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return getUserFromResultSet(resultSet);
            } else {
                return new User(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<User> getAllUsers(String adminId) {
        return getDatabase().withConnection(connection -> {
            List<User> users = new ArrayList<>();
            PreparedStatement statement;
            ResultSet resultSet;

            String sql = "SELECT * FROM _user WHERE exists (SELECT * FROM _user WHERE is_admin = TRUE AND user_id = ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, adminId);
            resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                return null;
            }

            while (resultSet.next()) {
                users.add(getUserFromResultSet(resultSet));
            }

            return users;
        });
    }

    public List<Account> getAllAccounts(String adminId) {
        return getDatabase().withConnection(connection -> {
            List<Account> accounts = new ArrayList<>();
            PreparedStatement statement;
            ResultSet resultSet;

            String sql = AccountDBAccessor.getProjectionForAccount() +
                    " WHERE exists (SELECT * FROM _user WHERE is_admin = TRUE AND user_id = ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, adminId);
            resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                return null;
            }

            while (resultSet.next()) {
                String token = "foo";
                accounts.add(AccountDBAccessor.getAccountFromResultSet(token, resultSet));
            }

            return accounts;
        });
    }

    public List<User> getAdministratorAccounts() {
        return execute(connection -> {
            String sql = "SELECT user_id, name, email, join_date FROM _user WHERE is_admin = TRUE";
            PreparedStatement statement = connection.prepareStatement(sql);

            List<User> administrators = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                administrators.add(UserDBAccessor.getUserFromResultSet(resultSet));
            }
            return administrators;
        });
    }

    public User getSuperAdminAccount() {
        return execute(connection -> {
            String sql = "SELECT user_id, name, email, join_date FROM _user WHERE is_super_admin = TRUE";
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return UserDBAccessor.getUserFromResultSet(resultSet);
            } else {
                return null;
            }
        });
    }

}
