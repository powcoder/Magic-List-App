https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.PagedList;
import play.cache.SyncCacheApi;
import play.db.ConnectionCallable;
import play.db.Database;
import play.mvc.Controller;

import javax.inject.Inject;
import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * An abstract class that
 */
abstract class DBAccessor {

    static final String TOTAL_COUNT = "total_count";

    private final Database database;

    enum DateTruncateValue {
        DAY, WEEK, MONTH, QUARTER, YEAR, CENTURY;

        String getValue() {
            return this.toString().toLowerCase();
        }
    }

    public DBAccessor(Database database) {
        this.database = database;
    }

    /**
     * @return The database used by the Buzz to persist information.
     */
    protected Database getDatabase() {
        return database;
    }

    static void setStringOrNull(PreparedStatement statement, int parameterIndex, String value) throws SQLException {
        if (value != null && value.length() > 0) {
            statement.setString(parameterIndex, value);
        } else {
            statement.setNull(parameterIndex, Types.VARCHAR);
        }
    }

    static void setMaxPage(PagedList pagedList, ResultSet resultSet) throws SQLException {
        if (!pagedList.isMaxPageSet()) {
            int totalCount = resultSet.getInt(TOTAL_COUNT);
            pagedList.setTotalNumberOfItems(totalCount);
        }
    }

    /**
     * @param columns A list of columns to organize for a SQL query
     * @return The list of columns, like "A, B, C"
     */
    static String getFormattedColumns(String... columns) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            builder.append(columns[i]);
            if (i < columns.length - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    static void rollbackQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void enableAutoCommitQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param autoCloseableArray Any closeable connections that were established in a given query, and need to be
     *                           closed.
     */
    static void closeConnections(AutoCloseable... autoCloseableArray) {
        for (AutoCloseable autoCloseable : autoCloseableArray) {
            if (autoCloseable != null) {
                try {
                    autoCloseable.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static String getQuestionMarkParametersForList(Collection list, String questionMarks) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(questionMarks);
            if (i < list.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    /**
     * @param resultSet A result set object that has not had #next called on it yet.
     * @return The number in the first column index or 0 if the call to #next returned false
     */
    static int getIntegerFromResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            return 0;
        }
    }

    <T> T execute(ConnectionCallable<T> function) {
        return getDatabase().withConnection(function);
    }

    <T> CompletionStage<T> executeAsync(ConnectionCallable<T> function) {
        return CompletableFuture.supplyAsync(() -> getDatabase().withConnection(function));
    }

    <T> T executeTransaction(ConnectionCallable<T> function) {
        return getDatabase().withConnection(false, function);
    }

    <T> CompletionStage<T> executeTransactionAsync(ConnectionCallable<T> function) {
        return CompletableFuture.supplyAsync(() -> getDatabase().withConnection(false, function));
    }

}
