https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.account.AccountNotification;
import model.account.ManagerRequestNotification;
import model.account.MessageNotification;
import model.user.User;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static database.TablesContract._ManagerRequestsEmployee.*;
import static database.TablesContract._User.*;
import static database.TablesContract._UserMessageNotification.*;
import static database.TablesContract._UserNotification.*;

/**
 * Created by Corey on 6/17/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class UserNotificationDBAccessor extends DBAccessor {

    public UserNotificationDBAccessor(Database database) {
        super(database);
    }

    public Optional<Boolean> createManagerRequestNotification(ManagerRequestNotification managerRequestNotification) {
        return getDatabase().withConnection(false, connection -> {
            createAccountNotification(Collections.singletonList(managerRequestNotification), connection);

            String sql = "INSERT INTO manager_requests_employee(manager_id, employee_id, request_token) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, managerRequestNotification.getManagerId());
            statement.setString(2, managerRequestNotification.getEmployeeId());
            statement.setString(3, managerRequestNotification.getNotificationId());
            statement.executeUpdate();

            connection.commit();

            return Optional.of(true);
        });
    }

    public Optional<Boolean> createMessageNotification(List<MessageNotification> messageNotificationList) {
        Connection connection = null;
        PreparedStatement statement = null;
        String sql;
        try {
            connection = getDatabase().getConnection();
            connection.setAutoCommit(false);

            createAccountNotification(messageNotificationList, connection);

            String parameters = getQuestionMarkParametersForList(messageNotificationList, "(?, ?, ?)");
            sql = "INSERT INTO user_message_notification(notification_id, message, sender_id) " +
                    "VALUES " + parameters;
            statement = connection.prepareStatement(sql);

            int counter = 1;
            for (MessageNotification messageNotification : messageNotificationList) {
                statement.setString(counter++, messageNotification.getNotificationId());
                statement.setString(counter++, messageNotification.getMessage());
                statement.setString(counter++, messageNotification.getSenderId());
            }

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

    public Optional<Boolean> createMessageNotification(MessageNotification messageNotification) {
        return createMessageNotification(Collections.singletonList(messageNotification));
    }

    public Optional<Boolean> setAllNotificationsAsSeen(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE user_notification SET is_seen = TRUE WHERE user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            return Optional.of(statement.executeUpdate() >= 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement);
        }
    }

    public Optional<Boolean> fulfillNotification(String userId, String notificationId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE user_notification SET is_fulfilled = TRUE WHERE user_id = ? AND notification_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, notificationId);

            return Optional.of(statement.executeUpdate() >= 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        } finally {
            closeConnections(connection, statement);
        }
    }

    public List<AccountNotification> getAllPagesNotifications(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            // manager requests
            String sql = "SELECT * " +
                    "FROM user_notification " +
                    "JOIN manager_requests_employee ON  request_token = notification_id " +
                    "JOIN _user ON manager_requests_employee.manager_id = _user.user_id " +
                    "WHERE user_notification.user_id = ? " +
                    "AND (current_date < expiration_date OR expiration_date IS NULL) " +
                    "AND is_fulfilled = FALSE " +
                    "ORDER BY notification_date DESC";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            resultSet = statement.executeQuery();

            List<AccountNotification> accountNotificationList = new ArrayList<>();
            while (resultSet.next()) {
                accountNotificationList.add(getManagerRequestNotificationFromResultSet(resultSet));
            }

            closeConnections(statement, resultSet);

            sql = "SELECT * " +
                    "FROM user_message_notification " +
                    "JOIN user_notification ON user_message_notification.notification_id = user_notification.notification_id " +
                    "JOIN _user ON _user.user_id = sender_id " +
                    "WHERE user_notification.user_id = ? " +
                    "AND (current_date < expiration_date OR expiration_date IS NULL) " +
                    "ORDER BY notification_date DESC " +
                    "LIMIT 5";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                accountNotificationList.add(getMessageNotificationFromResultSet(resultSet));
            }

            return accountNotificationList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<AccountNotification> getAllNotifications(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * " +
                    "FROM user_message_notification " +
                    "JOIN user_notification ON user_message_notification.notification_id = user_notification.notification_id " +
                    "JOIN _user ON _user.user_id = sender_id " +
                    "WHERE user_notification.user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            List<AccountNotification> accountNotificationList = new ArrayList<>();
            while (resultSet.next()) {
                accountNotificationList.add(getMessageNotificationFromResultSet(resultSet));
            }

            closeConnections(statement, resultSet);

            sql = "SELECT * " +
                    "FROM user_notification " +
                    "JOIN manager_requests_employee ON  request_token = notification_id " +
                    "JOIN _user ON manager_requests_employee.manager_id = _user.user_id " +
                    "WHERE user_notification.user_id = ? ";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                accountNotificationList.add(getManagerRequestNotificationFromResultSet(resultSet));
            }

            return accountNotificationList.parallelStream()
                    .sorted(AccountNotification::sortByDateDescending)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    private <T extends AccountNotification> void createAccountNotification(List<T> accountNotificationList, Connection connection) throws SQLException {
        PreparedStatement statement = null;
        String sql;
        try {
            String parameters = getQuestionMarkParametersForList(accountNotificationList, "(?, ?, ?, ?)");
            sql = "INSERT INTO user_notification(notification_id, user_id, notification_type, expiration_date) " +
                    "VALUES " + parameters;
            statement = connection.prepareStatement(sql);

            int counter = 1;
            for (AccountNotification notification : accountNotificationList) {
                statement.setString(counter++, notification.getNotificationId());
                statement.setString(counter++, notification.getUserId());
                statement.setString(counter++, notification.getNotificationType());
                if (notification.getExpirationDate() != -1) {
                    statement.setTimestamp(counter++, new Timestamp(notification.getNotificationDate()));
                } else {
                    statement.setNull(counter++, Types.TIMESTAMP);
                }

            }

            statement.executeUpdate();
        } finally {
            closeConnections(statement);
        }
    }

    private static MessageNotification getMessageNotificationFromResultSet(ResultSet resultSet) throws SQLException {
        AccountNotification n = getAccountNotificationFromResultSet(resultSet);
        String message = resultSet.getString(MESSAGE);
        String senderName = resultSet.getString(NAME);
        String senderId = resultSet.getString(SENDER_ID);
        return new MessageNotification(n.getNotificationId(), n.getUserId(), n.getNotificationDate(),
                n.getExpirationDate(), n.isFulfilled(), n.isSeen(), message, senderId, senderName);
    }

    private static ManagerRequestNotification getManagerRequestNotificationFromResultSet(ResultSet resultSet) throws SQLException {
        AccountNotification n = getAccountNotificationFromResultSet(resultSet);
        String employeeId = resultSet.getString(EMPLOYEE_ID);
        String managerId = resultSet.getString(MANAGER_ID);
        String managerName = resultSet.getString(NAME);
        String managerEmail = resultSet.getString(EMAIL);

        return new ManagerRequestNotification(n.getNotificationId(), n.getNotificationType(), n.getUserId(),
                n.getNotificationDate(), n.getExpirationDate(), n.isFulfilled(), n.isSeen(), employeeId, managerId,
                managerName, managerEmail);
    }

    private static AccountNotification getAccountNotificationFromResultSet(ResultSet resultSet) throws SQLException {
        String notificationId = resultSet.getString(NOTIFICATION_ID);
        String type = resultSet.getString(NOTIFICATION_TYPE);
        String userId = resultSet.getString(USER_ID);
        long notificationDate = resultSet.getTimestamp(NOTIFICATION_DATE).getTime();

        Timestamp expirationTimestamp = resultSet.getTimestamp(EXPIRATION_DATE);
        long expirationDate = expirationTimestamp == null ? -1 : expirationTimestamp.getTime();
        boolean isFulfilled = resultSet.getBoolean(IS_FULFILLED);
        boolean isSeen = resultSet.getBoolean(IS_SEEN);
        return new AccountNotification(notificationId, type, userId, notificationDate, expirationDate, isFulfilled, isSeen);
    }

}
