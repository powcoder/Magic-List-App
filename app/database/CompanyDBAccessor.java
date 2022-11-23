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

public class CompanyDBAccessor extends DBAccessor {

    public CompanyDBAccessor(Database database) {
        super(database);
    }

    public boolean isCompanyExist(String companyName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * FROM company_model WHERE company_name = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, companyName);
            resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

}
