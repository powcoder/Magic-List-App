https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.account.Account;
import model.account.AccountMetaData;
import play.Logger;
import play.db.Database;
import utilities.PasswordEncryption;
import utilities.RandomStringGenerator;

import java.sql.*;

import static database.TablesContract._CompanyModel.*;
import static database.TablesContract._StripeUserInfo.*;
import static database.TablesContract._User.*;
import static database.TablesContract._User.USER_ID;

/**
 * The database class that's used to perform operations related to individual users.
 */
public class AccountDBAccessor extends DBAccessor {

    private static final Logger.ALogger logger = Logger.of(AccountDBAccessor.class);

    /**
     * @inheritDoc
     */
    public AccountDBAccessor(Database database) {
        super(database);
    }

    /**
     * @param email The email to check and see if it's already in use
     * @return True if it's in use, false if it's not. Note, if an SQL error occurs, this method has a false-positive
     * and will return true, to be safe.
     */
    public boolean isEmailAlreadyInDatabase(String email) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            statement = connection.prepareStatement("SELECT email FROM _user WHERE email = ?;");
            statement.setString(1, email);
            resultSet = statement.executeQuery();

            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * Creates a given user and inserts him/her into the database.
     *
     * @param account         The account to be inputted to the database
     * @param password        The user's password, that has not been hashed yet
     * @param customerId      The user's stripe customer ID that he/she received at sign up.
     * @param couponId        The ID of the coupon that the user applied at sign up or null if one does not exist
     * @param verifyEmailLink The email verification link
     * @return A {@link Account} object that was just created or null if an exception is thrown.
     */
    public Account createAccount(Account account, String password, String customerId, String couponId, String verifyEmailLink) {
        RandomStringGenerator stringGenerator = RandomStringGenerator.getInstance();
        String token = stringGenerator.getNextRandomToken();
        String userId = stringGenerator.getNextRandomUserId();
        Connection connection = getDatabase().getConnection();
        PreparedStatement createUserStatement = null;
        PreparedStatement createStripeUserStatement = null;
        try {
            connection.setAutoCommit(false);
            password = PasswordEncryption.createHash(password.toCharArray());

            String sql = "INSERT INTO _user (user_id, name, token, email, password, verify_email_link, company_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);";
            createUserStatement = connection.prepareStatement(sql);
            createUserStatement.setString(1, userId);
            createUserStatement.setString(2, account.getName());
            createUserStatement.setString(3, token);
            createUserStatement.setString(4, account.getEmail());
            createUserStatement.setString(5, password);
            createUserStatement.setString(6, verifyEmailLink);
            createUserStatement.setString(7, account.getCompanyName());
            if (createUserStatement.executeUpdate() != 1) {
                logger.error("Could not create user", new IllegalStateException());
                connection.rollback();
                return null;
            }

            sql = "INSERT INTO stripe_user_info (user_id, coupon_id, stripe_customer_id, stripe_plan_id) " +
                    "VALUES (?, ?, ?, ?);";
            createStripeUserStatement = connection.prepareStatement(sql);
            createStripeUserStatement.setString(1, userId);
            setStringOrNull(createStripeUserStatement, 2, couponId);
            createStripeUserStatement.setString(3, customerId);
            createStripeUserStatement.setString(4, account.getStripePlanId());
            if (createStripeUserStatement.executeUpdate() != 1) {
                logger.error("Could not create stripe user", new IllegalStateException());
                connection.rollback();
                return null;
            }

            if (account.isManager()) {
                logger.debug("Creating manager for new user: {}", userId);
                ManagerMiscDBAccessor managerMiscDBAccessor = new ManagerMiscDBAccessor(getDatabase());
                boolean isSuccessful = managerMiscDBAccessor.setIsUserManager(connection, userId, true);
                if(!isSuccessful) {
                    logger.error("Could not create manager", new IllegalStateException());
                    connection.rollback();
                    return null;
                }
            }

            connection.commit();
            return Account.createAccountFromDatabase(userId, account.getName(), account.getEmail(), token,
                    null, account.getCustomerId(), account.getStripePlanId(), account.getCompanyName(),
                    false, false, account.isManager(), false, false);
        } catch (Exception e) {
            e.printStackTrace();
            rollbackQuietly(connection);
            return null;
        } finally {
            enableAutoCommitQuietly(connection);
            closeConnections(connection, createUserStatement, createStripeUserStatement);
        }
    }

    public boolean verifyEmail(String userId, String verifyEmailToken, String subscriptionId) throws SQLException {
        Connection connection = null;
        PreparedStatement subscriptionStatement = null;
        PreparedStatement verifyEmailStatement = null;
        int updateCount;
        String sql;
        try {
            connection = getDatabase().getConnection();
            connection.setAutoCommit(false);
            sql = "UPDATE stripe_user_info " +
                    "SET stripe_subscription_id = ? " +
                    "WHERE user_id = " +
                    "(SELECT user_id FROM _user WHERE _user.user_id = ? AND verify_email_link = ?)";
            subscriptionStatement = connection.prepareStatement(sql);
            subscriptionStatement.setString(1, subscriptionId);
            subscriptionStatement.setString(2, userId);
            subscriptionStatement.setString(3, verifyEmailToken);

            updateCount = subscriptionStatement.executeUpdate();
            if (updateCount != 1) {
                logger.error("Could not create subscription. Info: {updates: {}, user_id: {}, " +
                        "verify_link: {}}", updateCount, userId, verifyEmailToken);
                connection.rollback();
                return false;
            }

            sql = "UPDATE _user " +
                    "SET is_email_verified = ? " +
                    "WHERE user_id = ? AND verify_email_link = ?";
            verifyEmailStatement = connection.prepareStatement(sql);
            verifyEmailStatement.setBoolean(1, true);
            verifyEmailStatement.setString(2, userId);
            verifyEmailStatement.setString(3, verifyEmailToken);

            updateCount = verifyEmailStatement.executeUpdate();
            if (updateCount != 1) {
                logger.error("Could not verify user\'s email. Info: {updates: {}, user_id: {}, " +
                        "verify_link: {}}", updateCount, userId, verifyEmailToken);
                connection.rollback();
                return false;
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            rollbackQuietly(connection);
            throw new SQLException(e);
        } finally {
            enableAutoCommitQuietly(connection);
            closeConnections(connection, subscriptionStatement, verifyEmailStatement);
        }

    }

    public boolean createVerifyEmailLink(String email, String verifyEmailToken) {
        return saveLink(VERIFY_EMAIL_LINK, verifyEmailToken, email);
    }

    public boolean getAccountMetaData(Account account) {
        Connection connection = getDatabase().getConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT * " +
                    "FROM _user " +
                    "LEFT JOIN stripe_user_info ON _user.user_id = stripe_user_info.user_id " +
                    "WHERE _user.user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, account.getUserId());
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                // There were no results found
                return false;
            } else {
                String couponId = resultSet.getString(COUPON_ID);
                String verifyEmailToken = resultSet.getString(VERIFY_EMAIL_LINK);
                String subscriptionStatus = resultSet.getString(SUBSCRIPTION_STATUS);
                account.setMetaData(new AccountMetaData(couponId, verifyEmailToken, subscriptionStatus));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * Logs the user into the getDatabase() and returns his/her corresponding {@link Account} object.
     *
     * @param email    The email of the user.
     * @param password The user's password.
     * @param isWebApp True if logging in from the web app, false for the importer.
     * @return The {@link Account} of the given user or null if the credentials don't match.
     */
    public Account loginToAccount(String email, String password, boolean isWebApp) throws SQLException {
        email = email.toLowerCase();
        Connection connection = getDatabase().getConnection();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = getProjectionForAccount() + " WHERE email = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, email);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                // There were no results found
                return null;
            }

            String correctPassword = resultSet.getString(PASSWORD);
            try {
                if (PasswordEncryption.verifyPassword(password.toCharArray(), correctPassword)) {
                    return getAccountAndUpdateToken(connection, resultSet, isWebApp);
                } else {
                    return null;
                }
            } catch (PasswordEncryption.CannotPerformOperationException | PasswordEncryption.InvalidHashException e) {
                throw new SQLException(e);
            }
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * Logs the user into the getDatabase() and returns his/her corresponding {@link Account} object.
     *
     * @param importerToken The importer's token
     * @return The {@link Account} of the given user or null if the credentials don't match.
     */
    public Account loginToAccount(String importerToken) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = getProjectionForAccount() + " WHERE importer_token = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, importerToken);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                // There were no results found
                return null;
            }

            return getAccountAndUpdateToken(connection, resultSet, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public Account getAccountFromToken(String token) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = getProjectionForAccount() + " WHERE token = ? OR importer_token = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, token);
            statement.setString(2, token);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getAccountFromResultSet(token, resultSet);
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

    public Account getAccountFromEmail(String email) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = getProjectionForAccount() + " WHERE _user.email = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, email);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return getAccountFromResultSet(null, resultSet);
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

    /**
     * Changes the given user's password from the old value to the new one.
     *
     * @param token       The token of the user whose password will be changed.
     * @param oldPassword The user's old password. It must be correct in order for it to work.
     * @param newPassword The user's new password.
     * @return 1 if the operation was successful and the user's old password was correct, 0 if the user's password was
     * incorrect, or -1 if there was an SQL error.
     */
    public int changePassword(String token, String oldPassword, String newPassword) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT password FROM _user WHERE token = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, token);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                // This should never happen considering we already verified the user's ID
                return -1;
            }

            String correctPassword = resultSet.getString(PASSWORD);

            // Verify that the password the user entered is correct
            if (!PasswordEncryption.verifyPassword(oldPassword.toCharArray(), correctPassword)) {
                return 0;
            }

            statement.close();

            // Update the user's password now
            newPassword = PasswordEncryption.createHash(newPassword.toCharArray());
            sql = "UPDATE _user SET password = ? WHERE token = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, newPassword);
            statement.setString(2, token);
            statement.executeUpdate();

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    /**
     * Changes the given user's password from the old value to the new one.
     *
     * @param forgotPasswordLink The link that points to the user who requested a password reset.
     * @param email              The email of the user who needs a password reset.
     * @return 1 if the operation was successful and the user's old password was correct, 0 if the user's password was
     * incorrect, or -1 if there was an SQL error.
     */
    public boolean saveForgotPasswordLink(String forgotPasswordLink, String email) {
        return saveLink(FORGOT_PASSWORD_LINK, forgotPasswordLink, email);
    }

    /**
     * Changes the given user's password from the old value to the new one.
     *
     * @param forgotPasswordLink The link that points to the user who requested a password reset.
     * @param newPassword        The user's new password.
     * @return 1 if the operation was successful and the user's old password was correct, 0 if the user's password was
     * incorrect, or -1 if there was an SQL error.
     */
    public boolean resetPassword(String userId, String forgotPasswordLink, String newPassword) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();

            // Update the user's password now
            String hashedNewPassword = PasswordEncryption.createHash(newPassword.toCharArray());
            String sql = "UPDATE _user SET password = ?, forgot_password_link = ? " +
                    "WHERE forgot_password_link = ? AND user_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, hashedNewPassword);
            statement.setNull(2, Types.VARCHAR);
            statement.setString(3, forgotPasswordLink);
            statement.setString(4, userId);

            return statement.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    /**
     * @param userId        The ID of the user
     * @param importerToken The token that the importer uses to remain authenticated
     * @return The user's web app token, "null" if the web app has no token currently, or null if an SQL exception
     * occurred
     */
    public String getWebAppTokenFromImporterToken(String userId, String importerToken) {
        final String NULL = "null";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            // language=PostgreSQL
            String sql = "SELECT token FROM _user WHERE user_id = ? AND importer_token = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, importerToken);

            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String webAppToken = resultSet.getString(TOKEN);
                if (webAppToken == null) {
                    return NULL;
                } else {
                    return webAppToken;
                }
            } else {
                return NULL;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public boolean logout(String userId, String token) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql;
            if (token.contains("zm-imptr-tok_")) {
                // language=PostgreSQL
                sql = "UPDATE _user SET importer_token = ? WHERE importer_token = ? AND user_id = ?;";
            } else {
                // language=PostgreSQL
                sql = "UPDATE _user SET token = ? WHERE token = ? AND user_id = ?;";
            }
            statement = connection.prepareStatement(sql);
            statement.setNull(1, Types.VARCHAR);
            statement.setString(2, token);
            statement.setString(3, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }


//    MARK - Package-Private Methods

    private boolean saveLink(String columnNameThatHoldsTheLink, String link, String email) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE _user SET " + columnNameThatHoldsTheLink + " = ? WHERE email = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, link);
            statement.setString(2, email);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    private Account getAccountAndUpdateToken(Connection connection, ResultSet resultSet, boolean isWebApp) throws SQLException {
        String token;
        if (isWebApp) {
            token = RandomStringGenerator.getInstance().getNextRandomToken();
        } else {
            token = RandomStringGenerator.getInstance().getNextRandomImporterToken();
        }

        Account account = getAccountFromResultSet(token, resultSet);

        resultSet.close();

        if (!updateToken(connection, account.getUserId(), token, isWebApp)) {
            return null;
        } else {
            return account;
        }
    }

    private boolean updateToken(Connection connection, String userId, String newToken, boolean isWebApp) {
        PreparedStatement statement = null;
        try {
            String sql;
            if (isWebApp) {
                sql = "UPDATE _user SET token = ? WHERE user_id = ?;";
            } else {
                sql = "UPDATE _user SET importer_token = ? WHERE user_id = ?;";
            }
            statement = connection.prepareStatement(sql);
            statement.setString(1, newToken);
            statement.setString(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    static Account getAccountFromResultSet(String token, ResultSet resultSet) throws SQLException {
        String userId = resultSet.getString(USER_ID);
        String name = resultSet.getString(NAME);
        String email = resultSet.getString(EMAIL);
        String stripeSubscriptionId = resultSet.getString(SUBSCRIPTION_ID);
        String customerId = resultSet.getString(CUSTOMER_ID);
        String stripePlanId = resultSet.getString(STRIPE_PLAN_ID);
        String companyName = resultSet.getString(COMPANY_NAME);
        boolean isEmailVerified = resultSet.getBoolean(IS_EMAIL_VERIFIED);
        boolean isAdmin = resultSet.getBoolean(IS_ADMIN);
        boolean isManager = resultSet.getBoolean(IS_MANAGER);
        boolean isSuperAdmin = resultSet.getBoolean(IS_SUPER_ADMIN);
        boolean isBetaTester = resultSet.getBoolean(IS_BETA_TESTER);

        return Account.createAccountFromDatabase(userId, name, email, token, stripeSubscriptionId, customerId,
                stripePlanId, companyName, isEmailVerified, isAdmin, isManager, isSuperAdmin, isBetaTester);
    }

    static String getProjectionForAccount() {
        // language=PostgreSQL
        return "SELECT * " +
                "FROM _user " +
                "LEFT JOIN stripe_user_info ON _user.user_id = stripe_user_info.user_id ";
    }

}
