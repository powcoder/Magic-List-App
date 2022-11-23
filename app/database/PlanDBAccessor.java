https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.stripe.Plan;
import play.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static database.TablesContract._StripePlan.*;
import static database.TablesContract._StripePlan.STRIPE_PLAN_ID;

/**
 *
 */
public class PlanDBAccessor extends DBAccessor {

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public PlanDBAccessor(Database database) {
        super(database);
    }

    public Plan getPlanById(String stripePlanId) {
        Connection connection = getDatabase().getConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            // language=PostgreSQL
            String sql = "SELECT * FROM stripe_plan WHERE stripe_plan_id = ? AND is_hidden = FALSE;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, stripePlanId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                double amount = resultSet.getInt(PRICE_AMOUNT);
                double frequency = resultSet.getDouble(FREQUENCY);
                boolean isSubscription = resultSet.getBoolean(IS_SUBSCRIPTION);
                return new Plan(stripePlanId, amount, frequency, isSubscription);
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<Plan> getPlansFromDatabase(boolean shouldShowOptionToCancel) {
        Connection connection = getDatabase().getConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql;
            if (shouldShowOptionToCancel) {
                sql = "SELECT * " +
                        "FROM stripe_plan " +
                        "WHERE _index >= 0 AND is_hidden = FALSE " +
                        "ORDER BY _index ASC;";
            } else {
                sql = "SELECT * " +
                        "FROM stripe_plan " +
                        "WHERE _index >= 0 AND _index < 999 AND is_hidden = FALSE  " +
                        "ORDER BY _index ASC;";
            }
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            List<Plan> planList = new ArrayList<>();

            while (resultSet.next()) {
                double amount = resultSet.getInt(PRICE_AMOUNT);
                double frequency = resultSet.getDouble(FREQUENCY);
                boolean isSubscription = resultSet.getBoolean(IS_SUBSCRIPTION);
                String stripePlanId = resultSet.getString(STRIPE_PLAN_ID);
                planList.add(new Plan(stripePlanId, amount, frequency, isSubscription));
            }

            return planList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public boolean isPlanIdValid(String stripePlanId) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT * FROM stripe_plan WHERE stripe_plan_id = ? AND is_hidden = FALSE;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, stripePlanId);
            return statement.executeQuery()
                    .isBeforeFirst();
        });
    }

    public boolean updateUserPlanId(String stripeCustomerId, String stripePlanId, String subscriptionId,
                                    String status) {
        Connection connection = getDatabase().getConnection();
        PreparedStatement statement = null;
        try {
            // language=PostgreSQL
            String sql = "UPDATE stripe_user_info SET stripe_plan_id = ?, stripe_subscription_id = ?, " +
                    "subscription_status = ? " +
                    "WHERE stripe_customer_id = ?;";
            statement = connection.prepareStatement(sql);
            setStringOrNull(statement, 1, stripePlanId);
            setStringOrNull(statement, 2, subscriptionId);
            setStringOrNull(statement, 3, status);
            statement.setString(4, stripeCustomerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

}
