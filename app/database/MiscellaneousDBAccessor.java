https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import play.db.Database;
import utilities.RandomStringGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Corey on 3/12/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class MiscellaneousDBAccessor extends DBAccessor {

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public MiscellaneousDBAccessor(Database database) {
        super(database);
    }

    public boolean insertSuggestion(String userId, String suggestionText) {
        String suggestionId = RandomStringGenerator.getInstance().getNextRandomSuggestionId();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "INSERT INTO user_suggestion(suggestion_id, user_id, text) VALUES (?, ?, ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, suggestionId);
            statement.setString(2, userId);
            statement.setString(3, suggestionText);

            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean insertBugReport(String userId, String bugText) {
        String bugReportId = RandomStringGenerator.getInstance().getNextRandomBugReportId();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "INSERT INTO bug_report(bug_id, user_id, text) VALUES (?, ?, ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, bugReportId);
            statement.setString(2, userId);
            statement.setString(3, bugText);

            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean insertUserQuote(String userId, String quoteText, String author) {
        String quoteId = RandomStringGenerator.getInstance().getNextRandomQuoteId();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "INSERT INTO user_quote(quote_id, user_id, author, text) VALUES (?, ?, ?, ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, quoteId);
            statement.setString(2, userId);
            statement.setString(3, author);
            statement.setString(4, quoteText);

            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

}
