https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import java.sql.Connection;
import java.util.function.Function;

/**
 *
 */
public class Transaction {

    private final DBAccessor accessor;

    public Transaction(DBAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * @param function A function that takes a connection object and returns true if the transaction was successful,
     *                 or false otherwise.
     */
    public void performTransaction(Function<Connection, Boolean> function) {
        accessor.getDatabase().withConnection(false, connection -> {
            if (function.apply(connection)) {
                connection.commit();
            } else {
                connection.rollback();
            }
        });
    }

}
