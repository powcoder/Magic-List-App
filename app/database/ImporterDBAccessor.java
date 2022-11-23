https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.ImporterLimit;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static database.TablesContract._ImporterLimitModel.*;

/**
 * Created by Corey on 3/14/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ImporterDBAccessor extends DBAccessor {

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public ImporterDBAccessor(Database database) {
        super(database);
    }

    public ImporterLimit getImporterLimit(String userId) {
        Connection connection = null;
        try {
            connection = getDatabase().getConnection();
            return getImporterLimit(connection, userId);
        } finally {
            closeConnections(connection);
        }
    }

    // Package-private Methods

    ImporterLimit getImporterLimit(Connection connection, String userId) {
        PreparedStatement retrievalStatement = null;
        PreparedStatement creationStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "WITH daily_max AS (" +
                    "   SELECT import_count AS daily_total " +
                    "   FROM importer_limit " +
                    "   WHERE user_id = ? AND import_date = current_date" +
                    "), " +
                    "monthly_max AS (" +
                    "   SELECT sum(import_count) AS monthly_total " +
                    "   FROM importer_limit " +
                    "   WHERE user_id = ? AND date_trunc(\'month\', current_date) = date_trunc(\'month\', import_date) " +
                    "), " +
                    "model_limits AS (SELECT daily_max, monthly_max FROM importer_limit_model" +
                    ") " +
                    "SELECT * FROM daily_max, monthly_max, model_limits;";
            retrievalStatement = connection.prepareStatement(sql);
            retrievalStatement.setString(1, userId);
            retrievalStatement.setString(2, userId);

            resultSet = retrievalStatement.executeQuery();
            if (resultSet.next()) {
                int dailyTotal = resultSet.getInt("daily_total");
                int monthlyTotal = resultSet.getInt("monthly_total");
                int dailyMax = resultSet.getInt(DAILY_MAX);
                int monthlyMax = resultSet.getInt(MONTHLY_MAX);
                return new ImporterLimit(dailyTotal, monthlyTotal, dailyMax, monthlyMax);
            } else {
                // There is no data yet for the day, let's create it
                sql = "INSERT INTO importer_limit(user_id, import_count, import_date) VALUES (?, 0, current_date);";
                creationStatement = connection.prepareStatement(sql);
                creationStatement.setString(1, userId);
                creationStatement.executeUpdate();

                closeConnections(creationStatement);

                return getImporterLimit(connection, userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(retrievalStatement, creationStatement, resultSet);
        }
    }

    boolean addImportCount(Connection connection, String userId, int importCount) {
        PreparedStatement statement = null;
        try {
            String sql = "UPDATE importer_limit SET import_count = import_count + ? " +
                    "WHERE user_id = ? AND import_date = current_date;";
            statement = connection.prepareStatement(sql);
            statement.setInt(1, importCount);
            statement.setString(2, userId);
            return statement.executeUpdate() >= 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(statement);
        }
    }

}
