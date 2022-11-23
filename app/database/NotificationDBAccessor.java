https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.prospect.Notification;
import model.PagedList;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import play.Logger;
import play.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._DialSheetContact.CONTACT_TIME;
import static database.TablesContract._Notification.*;
import static database.TablesContract._PersonProspect.*;

public class NotificationDBAccessor extends AbstractPersonDatabase {

    private static final int AMOUNT_PER_PAGE = 25;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public NotificationDBAccessor(Database database) {
        super(database);
    }

    public Notification insertNotification(Notification notification) {
        return getDatabase().withConnection(connection -> {
            PreparedStatement statement;
            String sql = "INSERT INTO notification (notification_id, user_id, is_archived, message, notification_date, " +
                    "person_id) VALUES (?, ?, ?, ?, ?, ?)";
            statement = connection.prepareStatement(sql);
            statement.setString(1, notification.getNotificationId());
            statement.setString(2, notification.getUserId());
            statement.setBoolean(3, false);
            statement.setString(4, notification.getMessage());
            statement.setTimestamp(5, new Timestamp(notification.getNotificationDate()));
            statement.setString(6, notification.getProspect().getId());

            if (statement.executeUpdate() != 1) {
                String error = String.format("Could not insert notification: [user: %s]", notification.getUserId());
                logger.error(error, new IllegalStateException());
                return null;
            }

            return getNotificationById(notification.getUserId(), notification.getNotificationId(), connection);
        });
    }

    public boolean editNotification(String userId, Notification notification) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE notification SET message = ?, notification_date = ? " +
                    "WHERE user_id = ? AND notification_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, notification.getMessage());
            statement.setTimestamp(2, new Timestamp(notification.getNotificationDate()));
            statement.setString(3, userId);
            statement.setString(4, notification.getNotificationId());

            return statement.executeUpdate() == 1;
        });
    }

    public boolean setPersonStateFromNotificationStatus(String userId, String notificationId, ProspectState state) {
        return getDatabase().withConnection(false, connection -> {
            ResultSet resultSet;
            PreparedStatement statement;
            String sql;

            sql = "SELECT person_id FROM notification WHERE notification_id = ? AND user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, notificationId);
            statement.setString(2, userId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                logger.error("Could not get person from notification: [notification_id: {}, user_id: {}]", notificationId, userId);
                return false;
            }

            String personId = resultSet.getString(PERSON_ID);

            sql = "UPDATE notification SET notification_status = ? WHERE user_id = ? AND notification_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, state.getStateType());
            statement.setString(2, userId);
            statement.setString(3, notificationId);
            if (statement.executeUpdate() != 1) {
                logger.error("Could not update notification status: [user: {}, notification_id: {}]", userId, notificationId);
                connection.rollback();
                return false;
            }

            sql = "UPDATE person_state SET state = ? WHERE person_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, state.getStateType());
            statement.setString(2, personId);

            if (statement.executeUpdate() != 1) {
                logger.error("Could not update person status: [user: {}, notification_id: {}]", userId, notificationId);
                connection.rollback();
                return false;
            }

            DialSheetContactDBAccessor dialSheetContactDBAccessor = new DialSheetContactDBAccessor(getDatabase());

            Date contactTime = new Date();
            if (dialSheetContactDBAccessor.createDialSheetActivity(connection, state, userId, personId, contactTime)) {
                connection.commit();
                return true;
            } else {
                return false;
            }
        });
    }

    public boolean batchArchiveNotifications(String userId, List<String> notificationIdList) {
        return getDatabase().withConnection(connection -> {
            String notificationIdParameters = getQuestionMarkParametersForList(notificationIdList, "?");
            String sql = "UPDATE notification SET is_archived = ? " +
                    "WHERE user_id = ? AND notification_id IN (" + notificationIdParameters + ");";

            PreparedStatement statement = connection.prepareStatement(sql);

            int counter = 1;
            statement.setBoolean(counter++, true);
            statement.setString(counter++, userId);

            for (String notificationId : notificationIdList) {
                statement.setString(counter++, notificationId);
            }

            return statement.executeUpdate() >= 1;
        });
    }

    public Optional<Boolean> archiveNotification(String userId, String notificationId) {
        return performArchive(userId, notificationId, true);
    }

    public Optional<Boolean> unarchiveNotification(String userId, String notificationId) {
        return performArchive(userId, notificationId, false);
    }

    public boolean deleteNotification(String userId, String notificationId) {
        return getDatabase().withConnection(connection -> {
            String sql = "DELETE FROM notification WHERE user_id = ? AND notification_id = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, notificationId);

            return statement.executeUpdate() == 1;
        });
    }

    public int getNotificationCount(String userId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT COUNT(*) " +
                    "FROM notification " +
                    "WHERE user_id = ? " +
                    "AND is_archived = FALSE " +
                    "AND notification_date <= current_timestamp;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return -1;
            }
        });
    }

    public PagedList<Notification> getUnarchivedAndCompletedNotifications(String userId, int currentPage,
                                                                          Notification.Sorter sorter,
                                                                          boolean isAscending) {
        return getDatabase().withConnection(connection -> {
            String sortColumn = convertNotificationSorterToColumn(sorter);
            String sortOrder = isAscending ? "ASC" : "DESC";
            int offset = (currentPage - 1) * AMOUNT_PER_PAGE;

            String sql = "SELECT " + getProjectionForNotification(false) + " " +
                    "FROM notification " +
                    "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                    "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                    "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                    "WHERE notification.user_id = ? AND is_archived = ? AND notification_status != ? " +
                    "GROUP BY " + getProjectionForNotification(true) + " " +
                    "ORDER BY " + sortColumn + " " + sortOrder + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + offset;
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setBoolean(2, false);
            statement.setString(3, ProspectState.NOT_CONTACTED.getStateType());

            ResultSet resultSet = statement.executeQuery();
            PagedList<Notification> notifications = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
            notifications.setTotalNumberOfItems(getTotalUnarchivedAndCompleteNotifications(connection, userId));
            while (resultSet.next()) {
                notifications.add(getNotificationFromResultSet(resultSet));
            }

            return notifications;
        });
    }

    public PagedList<Notification> getNotificationsForToday(String userId, Date currentDate, Notification.Sorter sorter,
                                                            boolean isAscending, int page) {
        return getDatabase().withConnection(connection -> {
            String sortOrder = isAscending ? "ASC" : "DESC";
            String sql = "SELECT " + getProjectionForNotification(false) + ", " +
                    "count(*) OVER() AS TOTAL_COUNT " +
                    "FROM notification " +
                    "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                    "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                    "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                    "WHERE notification.user_id = ? AND " +
                    "date_trunc('day', notification_date) = date_trunc('day', ? :: TIMESTAMP) " +
                    "GROUP BY " + getProjectionForNotification(true) + " " +
                    "ORDER BY " + convertNotificationSorterToColumn(sorter) + " " + sortOrder + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + ((page - 1) * AMOUNT_PER_PAGE);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setTimestamp(2, new Timestamp(currentDate.getTime()));

            ResultSet resultSet = statement.executeQuery();
            PagedList<Notification> notifications = new PagedList<>(1, AMOUNT_PER_PAGE);
            while (resultSet.next()) {
                setMaxPage(notifications, resultSet);
                notifications.add(getNotificationFromResultSet(resultSet));
            }

            return notifications;
        });
    }

    public int getTotalUnarchivedAndCompleteNotifications(String userId) {
        return getDatabase().withConnection(connection -> {
            return getTotalUnarchivedAndCompleteNotifications(connection, userId);
        });
    }

    /**
     * @param currentPage 1-based index of the page
     */
    public PagedList<Notification> getPastNotifications(String userId, int currentPage, Notification.Sorter sorter,
                                                        boolean isAscending) {
        return getNotificationsFromType(userId, currentPage, sorter, isAscending, Notification.Type.PAST);
    }

    /**
     * @param currentPage 1-based index of the page
     */
    public PagedList<Notification> getCurrentNotifications(String userId, int currentPage, Notification.Sorter sorter,
                                                           boolean isAscending) {
        return getNotificationsFromType(userId, currentPage, sorter, isAscending, Notification.Type.CURRENT);
    }

    /**
     * @param currentPage 1-based index of the page
     */
    public PagedList<Notification> getUpcomingNotifications(String userId, int currentPage, Notification.Sorter sorter,
                                                            boolean isAscending) {
        return getNotificationsFromType(userId, currentPage, sorter, isAscending, Notification.Type.UPCOMING);
    }

    public List<Notification> getPersonPastNotifications(String userId, String personId) {
        return getPersonNotifications(userId, personId, Notification.Type.PAST);
    }

    public List<Notification> getPersonCurrentNotifications(String userId, String personId) {
        return getPersonNotifications(userId, personId, Notification.Type.CURRENT);
    }

    public List<Notification> getPersonUpcomingNotifications(String userId, String personId) {
        return getPersonNotifications(userId, personId, Notification.Type.UPCOMING);
    }

    // Mark - Private Methods

    private Notification getNotificationById(String userId, String notificationId, Connection connection) throws SQLException {
        String sql = "SELECT " + getProjectionForNotification(false) + " " +
                "FROM notification " +
                "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                "WHERE notification.user_id = ? AND notification_id = ? " +
                "GROUP BY " + getProjectionForNotification(true);
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, notificationId);

        ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? getNotificationFromResultSet(resultSet) : null;
    }

    /**
     * @param isGroupBy True if the projection is being used in a GROUP BY clause, false for a SELECT clause
     * @return A SQL projection used to reference columns in a notification
     */
    private String getProjectionForNotification(boolean isGroupBy) {
        String aggregateFunctionProjection = "";
        if (!isGroupBy) {
            aggregateFunctionProjection = "MAX(" + CONTACT_TIME + ") AS " + LAST_CONTACTED + ", ";
        }

        String personIdTablePrefix = PERSON_PROSPECT + ".";
        String notificationStatePrefix = "n_state.";
        String personStatePrefix = "p_state.";

        String notificationId = NOTIFICATION + "." + NOTIFICATION_ID;
        String personStateColumns = ProspectStateDBAccessor.getProjectionForProspectState(notificationStatePrefix, isGroupBy);

        return getFormattedColumns(notificationId, IS_ARCHIVED, MESSAGE, NOTIFICATION_DATE,
                USER_ID, personStateColumns) + ", " + aggregateFunctionProjection +
                getSelectionForPerson(personIdTablePrefix, personStatePrefix, isGroupBy);
    }

    private int getTotalUnarchivedAndCompleteNotifications(Connection connection, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) OVER(), user_id, notification_id, notification.person_id  " +
                "FROM notification " +
                "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                "WHERE notification.user_id = ? AND is_archived = ? AND notification_status != ? " +
                "GROUP BY user_id, notification_id, notification.person_id";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setBoolean(2, false);
        statement.setString(3, ProspectState.NOT_CONTACTED.getStateType());
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            return 0;
        }
    }

    private PagedList<Notification> getNotificationsFromType(String userId, int currentPage, Notification.Sorter sorter,
                                                             boolean isAscending, Notification.Type notificationType) {
        return getDatabase().withConnection(connection -> {
            int offset = (currentPage - 1) * AMOUNT_PER_PAGE;
            String notificationDatePredicate = notificationType == Notification.Type.UPCOMING ? " > " : " <= ";
            String sortOrder = isAscending ? "ASC" : "DESC";
            String dateCreatedSortOrder = notificationType == Notification.Type.UPCOMING ? "ASC" : "DESC";

            String sql = "SELECT " + getProjectionForNotification(false) + ", " +
                    "count(*) over() AS " + TOTAL_COUNT + " " +
                    "FROM notification " +
                    "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                    "JOIN person_state ON person_prospect.person_id = person_state.person_id " +
                    "JOIN person_state_type p_state ON person_state.state = p_state.state_type " +
                    "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                    "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                    "WHERE notification.user_id = ? " +
                    "AND notification_date " + notificationDatePredicate + " current_timestamp " +
                    "AND " + IS_ARCHIVED + " = ? " +
                    "GROUP BY " + getProjectionForNotification(true) + " " +
                    "ORDER BY " + convertNotificationSorterToColumn(sorter) + " " + sortOrder + ", " +
                    DATE_CREATED + " " + dateCreatedSortOrder + " " +
                    "LIMIT " + AMOUNT_PER_PAGE + " OFFSET " + offset;
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setBoolean(2, notificationType == Notification.Type.PAST);
            ResultSet resultSet = statement.executeQuery();

            PagedList<Notification> notificationList = new PagedList<>(currentPage, AMOUNT_PER_PAGE);

            while (resultSet.next()) {
                setMaxPage(notificationList, resultSet);
                notificationList.add(getNotificationFromResultSet(resultSet));
            }

            return notificationList;
        });
    }

    private Optional<Boolean> performArchive(String userId, String notificationId, boolean isArchived) {
        return getDatabase().withConnection(connection -> {
            String sql = "UPDATE notification SET is_archived = ? " +
                    "WHERE user_id = ? AND notification_id = ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setBoolean(1, isArchived);
            statement.setString(2, userId);
            statement.setString(3, notificationId);

            return Optional.of(statement.executeUpdate() == 1);
        });
    }

    private List<Notification> getPersonNotifications(String userId, String personId, Notification.Type notificationType) {
        return getDatabase().withConnection(connection -> {
            String notificationDatePredicate = notificationType == Notification.Type.UPCOMING ? ">" : "<=";
            String orderBy = notificationType == Notification.Type.UPCOMING ? "ASC" : "DESC";
            String dateCreatedOrderBy = notificationType == Notification.Type.UPCOMING ? "ASC" : "DESC";

            String sql = "SELECT " + getProjectionForNotification(false) + ", " +
                    "count(*) OVER() AS " + TOTAL_COUNT + " " +
                    "FROM notification " +
                    "JOIN person_state_type n_state ON notification.notification_status = n_state.state_type " +
                    "JOIN person_prospect ON notification.person_id = person_prospect.person_id " +
                    "JOIN person_state p ON person_prospect.person_id = p.person_id " +
                    "JOIN person_state_type p_state ON p.state = p_state.state_type " +
                    "LEFT JOIN dial_sheet_contact ON person_prospect.person_id = dial_sheet_contact.person_id " +
                    "WHERE notification.user_id = ? " +
                    "AND notification.person_id = ? " +
                    "AND notification.is_archived = ? " +
                    "AND notification_date " + notificationDatePredicate + " current_timestamp " +
                    "GROUP BY " + getProjectionForNotification(true) + " " +
                    "ORDER BY " + NOTIFICATION_DATE + " " + orderBy + ", " +
                    DATE_CREATED + " " + dateCreatedOrderBy + ";";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, personId);
            statement.setBoolean(3, notificationType == Notification.Type.PAST);
            ResultSet resultSet = statement.executeQuery();

            List<Notification> notificationList = new ArrayList<>();
            while (resultSet.next()) {
                notificationList.add(getNotificationFromResultSet(resultSet));
            }

            return notificationList;
        });
    }

    private Notification getNotificationFromResultSet(ResultSet resultSet) throws SQLException {
        String notificationId = resultSet.getString(NOTIFICATION_ID);
        String userId = resultSet.getString(USER_ID);
        String message = resultSet.getString(MESSAGE);
        long notificationDate = resultSet.getTimestamp(NOTIFICATION_DATE).getTime();

        long lastContactedTime = Optional.ofNullable(resultSet.getTimestamp(LAST_CONTACTED))
                .orElse(new Timestamp(-1))
                .getTime();

        boolean isArchived = resultSet.getBoolean(IS_ARCHIVED);

        String personIdTablePrefix = PERSON_PROSPECT + ".";
        String notificationStateTablePrefix = "n_state.";
        String personStateTablePrefix = "p_state.";

        ProspectState state = ProspectStateDBAccessor.getProspectStateFromResultSet(resultSet, notificationStateTablePrefix);
        Prospect person = AbstractPersonDatabase.getPersonFromResultSet(resultSet, personIdTablePrefix,
                personStateTablePrefix);

        return new Notification(notificationId, userId, person, message, notificationDate, lastContactedTime,
                isArchived, state);
    }

    private String convertNotificationSorterToColumn(Notification.Sorter sorter) {
        if (sorter == Notification.Sorter.AREA_CODE) {
            return "SUBSTRING(" + PERSON_PHONE + ", " + "\'\\(?(\\d{3})\\)?\')";
        } else if (sorter == Notification.Sorter.COMPANY_NAME) {
            return PERSON_COMPANY_NAME;
        } else {
            return sorter.getRawText();
        }
    }

}
