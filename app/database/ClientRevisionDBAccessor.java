https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static database.TablesContract._ClientVersion.*;

/**
 *
 */
public class ClientRevisionDBAccessor extends DBAccessor {

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public ClientRevisionDBAccessor(Database database) {
        super(database);
    }

    public double getWindowsCurrentVersion() {
        return getCurrentVersion("windows");
    }

    public double getMacCurrentVersion() {
        return getCurrentVersion("mac");
    }

    private double getCurrentVersion(String operatingSystemName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT MAX(version) FROM client_revision WHERE operating_system = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, operatingSystemName);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            closeConnections(connection, preparedStatement, resultSet);
        }
    }

}
