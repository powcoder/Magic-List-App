https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.user.User;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._SharesSearchResult.SHARER_ID;
import static database.TablesContract._User.*;

/**
 * Created by Corey on 7/5/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class UserDBAccessor extends DBAccessor {

    public UserDBAccessor(Database database) {
        super(database);
    }

    public List<User> getSharedUsersForProspect(final String personId) {
        return getDatabase().withConnection((connection -> {
            String sql = "SELECT pu.user_id, email, name, join_date " +
                    "FROM person_prospect " +
                    "JOIN _user pu ON person_prospect.owner_id = pu.user_id " +
                    "WHERE person_id = ? " +
                    "UNION " +
                    "SELECT su.user_id, email, name, join_date " +
                    "FROM shares_prospect " +
                    "JOIN _user su ON shares_prospect.receiver_id = su.user_id " +
                    "WHERE person_id = ? " +
                    "ORDER BY name ASC;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, personId);
            statement.setString(2, personId);

            ResultSet resultSet = statement.executeQuery();

            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(getUserFromResultSet(resultSet));
            }

            return users;
        }));
    }

    // Mark - Package-Private Methods

    static String getProjectionForUser(String userTablePrefix, boolean isGroupBy) {
        userTablePrefix = Optional.of(userTablePrefix).orElse("");
        if (userTablePrefix.length() > 0 && !userTablePrefix.endsWith(".")) {
            userTablePrefix += ".";
        }

        String userTablePrefixNoPeriod = "";
        if (userTablePrefix.length() > 0) {
            userTablePrefixNoPeriod = userTablePrefix.substring(0, userTablePrefix.length() - 1);
        }

        String userId = userTablePrefix + USER_ID;
        if (!isGroupBy) {
            userId += " AS " + userTablePrefixNoPeriod + USER_ID;
        }

        String email = userTablePrefix + EMAIL;
        if (!isGroupBy) {
            email += " AS " + userTablePrefixNoPeriod + EMAIL;
        }

        String name = userTablePrefix + NAME;
        if (!isGroupBy) {
            name += " AS " + userTablePrefixNoPeriod + NAME;
        }

        String joinDate = userTablePrefix + JOIN_DATE;
        if (!isGroupBy) {
            joinDate += " AS " + userTablePrefixNoPeriod + JOIN_DATE;
        }

        return getFormattedColumns(userId, email, name, joinDate);
    }

    static User getSharerFromResultSet(ResultSet resultSet) throws SQLException {
        String sharerId = resultSet.getString(SHARER_ID);
        if (sharerId != null) {
            String sharerEmail = resultSet.getString(EMAIL);
            String sharerName = resultSet.getString(NAME);
            long sharerJoinDate = resultSet.getDate(JOIN_DATE).getTime();
            return new User(sharerId, sharerEmail, sharerName, sharerJoinDate);
        } else {
            return null;
        }
    }

    static User getUserFromResultSet(ResultSet resultSet) throws SQLException {
        return getUserFromResultSet(resultSet, "");
    }

    static User getUserFromResultSet(ResultSet resultSet, String userTablePrefix) throws SQLException {
        userTablePrefix = Optional.ofNullable(userTablePrefix).orElse("");
        if (userTablePrefix.length() > 0) {
            userTablePrefix = userTablePrefix.substring(0, userTablePrefix.length() - 1);
        }

        String userId = resultSet.getString(userTablePrefix + USER_ID);
        String email = resultSet.getString(userTablePrefix + EMAIL);
        String name = resultSet.getString(userTablePrefix + NAME);
        long joinDate = resultSet.getDate(userTablePrefix + JOIN_DATE).getTime();
        return new User(userId, email, name, joinDate);
    }

}
