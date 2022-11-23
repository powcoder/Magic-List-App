https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.oauth.OAuthAccount;
import model.oauth.OAuthProvider;
import model.oauth.OAuthToken;
import play.Logger;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static database.TablesContract._OAuthAccount.*;

/**
 * Created by Corey on 4/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class OAuthDBAccessor extends DBAccessor {

    public OAuthDBAccessor(Database database) {
        super(database);
    }

    public boolean createAccount(String userId, OAuthAccount account, OAuthToken authToken) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "INSERT INTO oauth_account(user_id, oauth_account_id, oauth_email, provider, access_token, " +
                    "refresh_token, access_expiration_time) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(oauth_account_id) DO UPDATE " +
                    "SET access_token = EXCLUDED.access_token, refresh_token = EXCLUDED.refresh_token, " +
                    "access_expiration_time = EXCLUDED.access_expiration_time, last_refresh = now();";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, account.getAccountId());
            statement.setString(3, account.getEmail());
            statement.setString(4, account.getProvider().getRawText());
            statement.setString(5, authToken.getAccessToken());
            statement.setString(6, authToken.getRefreshToken());
            statement.setLong(7, authToken.getExpirationTimeInMillis());
            statement.executeUpdate();

            Logger.debug("Created Oauth Account for user: {}", userId);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean deleteAccount(String userId, String oauthAccountId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "DELETE FROM oauth_account WHERE user_id = ? AND oauth_account_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, oauthAccountId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean saveAuthTokenForAccount(String userId, String oauthAccountId, OAuthToken authToken) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE oauth_account SET access_token = ?, access_expiration_time = ?, refresh_token = ? " +
                    "WHERE user_id = ? AND oauth_account_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, authToken.getAccessToken());
            statement.setLong(2, authToken.getExpirationTimeInMillis());
            statement.setString(3, authToken.getRefreshToken());
            statement.setString(4, userId);
            statement.setString(5, oauthAccountId);
            boolean isSuccessful =  statement.executeUpdate() == 1;
            if(isSuccessful) {
                Logger.debug("Successfully saved auth token for account: {}, oauth account: {}", userId,
                        oauthAccountId);
            } else {
                Logger.error("Could not save auth token to account: {}, oauth account: {}",
                        userId, oauthAccountId, new IllegalStateException());
            }
            return isSuccessful;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public List<OAuthAccount> getAccounts(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * FROM oauth_account WHERE user_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            List<OAuthAccount> accounts = new ArrayList<>();
            while (resultSet.next()) {
                String accountId = resultSet.getString(OAUTH_ACCOUNT_ID);
                String email = resultSet.getString(OAUTH_EMAIL);
                OAuthProvider provider = OAuthProvider.parse(resultSet.getString(OAUTH_PROVIDER));
                accounts.add(new OAuthAccount(accountId, email, provider));
            }

            return accounts;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public boolean hasOutlookAccounts(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * FROM oauth_account WHERE user_id = ? AND provider = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, OAuthProvider.OUTLOOK.getRawText());
            resultSet = statement.executeQuery();

            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public OAuthToken getAuthTokenForAccount(String userId, String oauthAccountId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * FROM oauth_account WHERE user_id = ? AND oauth_account_id = ?;";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, oauthAccountId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String accessToken = resultSet.getString(ACCESS_TOKEN);
                String refreshToken = resultSet.getString(REFRESH_TOKEN);
                long expirationTimeMillis = resultSet.getLong(ACCESS_EXPIRATION_TIME);
                return new OAuthToken(accessToken, refreshToken, expirationTimeMillis);
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

}
