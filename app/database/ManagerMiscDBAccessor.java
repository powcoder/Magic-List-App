https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.account.Account;
import model.manager.Employee;
import model.user.User;
import play.Logger;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._Manages.*;
import static database.TablesContract._User.*;

/**
 * Created by Corey on 6/8/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerMiscDBAccessor extends DBAccessor {

    public ManagerMiscDBAccessor(Database database) {
        super(database);
    }

    public boolean setIsUserManager(String userId, boolean isManager) {
        return getDatabase().withConnection(false, connection -> {
            boolean isSuccessful = setIsUserManager(connection, userId, isManager);
            if(isSuccessful) {
                connection.commit();
                return true;
            } else {
                return false;
            }
        });
    }

    public Optional<String> getManagerRequestEmployeeToken(String managerId, String employeeId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql;

            sql = "SELECT request_token " +
                    "FROM manager_requests_employee " +
                    "WHERE manager_id = ? AND employee_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, managerId);
            statement.setString(2, employeeId);

            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(resultSet.getString(1));
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public Optional<Boolean> isUserOnATeamAlready(String employeeId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql;

            sql = "SELECT * " +
                    "FROM manages " +
                    "WHERE employee_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, employeeId);

            return Optional.of(statement.executeQuery().isBeforeFirst());
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement);
        }
    }

    public Optional<Boolean> addUserAsEmployee(String managerId, String employeeId, String requestId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            connection.setAutoCommit(false);
            String sql;

            sql = "SELECT * " +
                    "FROM manager_requests_employee " +
                    "WHERE manager_id = ? AND employee_id = ? AND request_token = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, managerId);
            statement.setString(2, employeeId);
            statement.setString(3, requestId);

            if (!statement.executeQuery().isBeforeFirst()) {
                Logger.debug("No manager requests found for manager: " + managerId + " with employee: " + employeeId);
                return Optional.of(false);
            }

            closeConnections(statement);

            sql = "INSERT INTO manages(manager_id, employee_id, team_join_date) " +
                    "SELECT ?, ?, current_date " +
                    "FROM manages " +
                    "WHERE manager_id = ? AND " +
                    "NOT EXISTS (SELECT * FROM manages WHERE employee_id = ?) " +
                    "LIMIT 1;";
            statement = connection.prepareStatement(sql);

            statement.setString(1, managerId);
            statement.setString(2, employeeId);
            statement.setString(3, managerId);
            statement.setString(4, employeeId);

            statement.executeUpdate();

            sql = "UPDATE user_notification SET is_fulfilled = TRUE, is_seen = TRUE WHERE notification_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, requestId);

            statement.executeUpdate();
            connection.commit();

            return Optional.of(true);
        } catch (SQLException e) {
            e.printStackTrace();
            rollbackQuietly(connection);
            return Optional.empty();
        } finally {
            enableAutoCommitQuietly(connection);
            closeConnections(connection, statement);
        }
    }

    public Optional<Boolean> deleteEmployee(String userId, String employeeId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "DELETE FROM manages WHERE manager_id = ? AND employee_id = ?";
            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);
            statement.setString(2, employeeId);

            return Optional.of(statement.executeUpdate() == 1);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement);
        }
    }

    public Optional<Employee> getEmployeeById(String managerId, String employeeId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * " +
                    "FROM manages JOIN _user ON manages.employee_id = _user.user_id " +
                    "WHERE manager_id = ? AND employee_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, managerId);
            statement.setString(2, employeeId);

            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String name = resultSet.getString(NAME);
                String email = resultSet.getString(NAME);
                long teamJoinDate = resultSet.getDate(TEAM_JOIN_DATE).getTime();
                long magicListJoinDate = resultSet.getDate(JOIN_DATE).getTime();

                return Optional.of(new Employee(employeeId, name, email, teamJoinDate, magicListJoinDate, managerId));
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public Optional<List<String>> getManagerIdFromUserId(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT manager_id " +
                    "FROM manages " +
                    "WHERE employee_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(Collections.singletonList(resultSet.getString(MANAGER_ID)));
            } else {
                return Optional.of(new ArrayList<>());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<User> searchForEmployeeToAddToTeam(Account account, String key) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT name, email, join_date, user_id " +
                    "FROM _user " +
                    "WHERE company_name = ? AND " +
                    "(lower(name) LIKE lower(?) OR lower(email) LIKE lower(?)) AND " +
                    "user_id != ? AND is_test_account = FALSE " +
                    "ORDER BY name " +
                    "LIMIT 25";
            statement = connection.prepareStatement(sql);
            statement.setString(1, account.getCompanyName());
            statement.setString(2, key + "%");
            statement.setString(3, key + "%");
            statement.setString(4, account.getUserId());

            resultSet = statement.executeQuery();

            return getUsersFromResultSet(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<Employee> getManagerEmployees(String managerId, boolean includeSelf) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql;
            if (!includeSelf) {
                sql = "SELECT * " +
                        "FROM manages JOIN _user ON employee_id = user_id " +
                        "WHERE manager_id = ? " +
                        "AND manager_id != employee_id " +
                        "ORDER BY name, email ASC";
            } else {
                sql = "SELECT * " +
                        "FROM manages JOIN _user ON employee_id = user_id " +
                        "WHERE manager_id = ? " +
                        "ORDER BY name, email ASC";
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, managerId);

            resultSet = statement.executeQuery();

            return getEmployeesFromResultSet(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * @return The list of employees that are this user's coworker, minus the employee entered to be ignored
     */
    public List<Employee> getCoWorkers(String userId, String employeeIdToIgnore) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * " +
                    "FROM manages JOIN _user ON employee_id = user_id " +
                    "WHERE manager_id = (" +
                    "   SELECT manager_id FROM manages WHERE employee_id = ?" +
                    ") AND employee_id != ? " +
                    "ORDER BY name, email ASC;";

            statement = connection.prepareStatement(sql);

            statement.setString(1, userId);
            statement.setString(2, employeeIdToIgnore);

            resultSet = statement.executeQuery();

            return getEmployeesFromResultSet(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

//    Package-Private Methods

    boolean setIsUserManager(Connection connection, String userId, boolean isManager) throws SQLException {
        String sql = "UPDATE _user SET is_manager = ? WHERE user_id = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setBoolean(1, isManager);
        statement.setString(2, userId);
        if (statement.executeUpdate() != 1) {
            connection.rollback();
            return false;
        }
        if (isManager) {
            sql = "INSERT INTO manager(manager_id) VALUES (?) ON CONFLICT DO NOTHING;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.executeUpdate(); // the update count may be 0 after the user re-enables the portal

            sql = "INSERT INTO manages(manager_id, employee_id) VALUES (?, ?) ON CONFLICT DO NOTHING;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, userId);
            statement.executeUpdate(); // the update count may be 0 after the user re-enables the portal
        } else {
            sql = "delete from manages where manager_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.executeUpdate();
        }
        return true;
    }

//    Private Methods

    private List<User> getUsersFromResultSet(ResultSet resultSet) throws SQLException {
        List<User> userList = new ArrayList<>();
        while (resultSet.next()) {
            String userId = resultSet.getString(USER_ID);
            String employeeEmail = resultSet.getString(EMAIL);
            String employeeName = resultSet.getString(NAME);
            long joinDate = resultSet.getDate(JOIN_DATE).getTime();
            userList.add(new User(userId, employeeEmail, employeeName, joinDate));
        }

        return userList;
    }

    private List<Employee> getEmployeesFromResultSet(ResultSet resultSet) throws SQLException {
        List<Employee> employeeList = new ArrayList<>();
        String managerId = null;
        while (resultSet.next()) {
            if (managerId == null) {
                managerId = resultSet.getString(MANAGER_ID);
            }
            String employeeId = resultSet.getString(EMPLOYEE_ID);
            String employeeName = resultSet.getString(NAME);
            String employeeEmail = resultSet.getString(EMAIL);
            long teamJoinDate = resultSet.getDate(TEAM_JOIN_DATE).getTime();
            long magicListJoinDate = resultSet.getDate(JOIN_DATE).getTime();
            employeeList.add(new Employee(employeeId, employeeName, employeeEmail, teamJoinDate, magicListJoinDate, managerId));
        }

        return employeeList;
    }

}
