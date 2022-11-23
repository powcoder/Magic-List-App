https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package logic;

import clients.OAuthClient;
import clients.OutlookAuthClient;
import database.OAuthDBAccessor;
import model.account.Account;
import model.calendar.CalendarEvent;
import model.oauth.OAuthProvider;
import model.oauth.OAuthToken;
import model.prospect.Appointment;
import play.Logger;
import play.db.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utilities.FutureUtility.getFromFutureQuietly;

/**
 *
 */
public class AppointmentToCalendarEventsLinker {

    private final Logger.ALogger logger = Logger.of(AppointmentToCalendarEventsLinker.class);
    private final Database database;
    private final OutlookAuthClient outlookAuthClient;

    private final Map<String, Appointment> calendarEventsToAppointments = new HashMap<>();
    private final Map<String, OAuthToken> oauthAccountIdsToAuthTokens = new HashMap<>();
    private final Map<String, List<String>> oauthAccountIdsToEventIds = new HashMap<>();

    public AppointmentToCalendarEventsLinker(Database database, OutlookAuthClient client) {
        this.database = database;
        this.outlookAuthClient = client;
    }

    /**
     * @return True if successful, false otherwise. Could fail because the oauth account wasn't linked to a provider,
     * the oauth token could not be refreshed, or it could not be retrieved from the database.
     */
    public boolean linkCalendarEventsToAppointments(List<Appointment> appointmentList) {
        Account account = Account.getAccountFromSession();
        OAuthDBAccessor oAuthDBAccessor = new OAuthDBAccessor(database);

        for (Appointment appointment : appointmentList) {
            for (CalendarEvent event : appointment.getCalendarEvents()) {
                calendarEventsToAppointments.put(event.getEventId(), appointment);

                String oauthAccountId = event.getOauthAccountId();
                String hyperLink = event.getHyperLink();
                if (hyperLink != null) {
                    // skip... we already have the hyperlink for the given calendar event
                    continue;
                }

                OAuthToken oAuthToken = oauthAccountIdsToAuthTokens.get(oauthAccountId);

                if (oAuthToken == null) {
                    // The token is not in the map; get it
                    oAuthToken = oAuthDBAccessor.getAuthTokenForAccount(account.getUserId(), oauthAccountId);
                    if (oAuthToken != null && oAuthToken.isExpired()) {

                        OAuthClient client = getOAuthClientForProvider(OAuthProvider.OUTLOOK);

                        if(client == null) {
                            logger.error("OAuth client could not be retrieved: [user: {}]", account.getUserId());
                            logger.error("Error: ", new IllegalStateException());
                            return false;
                        }

                        oAuthToken = getFromFutureQuietly(client.refreshAccessToken(oauthAccountId, oAuthToken));
                        if (oAuthToken != null) {
                            oAuthDBAccessor.saveAuthTokenForAccount(account.getUserId(), oauthAccountId, oAuthToken);
                        } else {
                            // The auth token couldn't be refreshed from the provider
                            logger.error("The Auth token could not be retrieved/updated for account: {}", oauthAccountId);
                            logger.error("Error: ", new IllegalStateException());
                            return false;
                        }
                    } else if (oAuthToken == null) {
                        // The auth token couldn't be retrieved from the DB
                        logger.error("The Auth token could not be retrieved from the database for account: {}",
                                oauthAccountId);
                        logger.error("Error: ", new IllegalStateException());
                        return false;
                    }
                }

                oauthAccountIdsToAuthTokens.put(oauthAccountId, oAuthToken);

                List<String> eventIds;
                if (!oauthAccountIdsToEventIds.containsKey(oauthAccountId)) {
                    eventIds = new ArrayList<>();
                    oauthAccountIdsToEventIds.put(oauthAccountId, eventIds);
                } else {
                    eventIds = oauthAccountIdsToEventIds.get(oauthAccountId);
                }

                eventIds.add(event.getEventId());
            }
            // end calendar event for loop
        }

        return true;
    }

    public Map<String, Appointment> getCalendarEventsToAppointments() {
        return calendarEventsToAppointments;
    }

    public Map<String, OAuthToken> getOauthAccountIdsToAuthTokens() {
        return oauthAccountIdsToAuthTokens;
    }

    public Map<String, List<String>> getOauthAccountIdsToEventIds() {
        return oauthAccountIdsToEventIds;
    }

    // Private Methods

    private OAuthClient getOAuthClientForProvider(OAuthProvider provider) {
        if (provider == OAuthProvider.OUTLOOK) {
            return outlookAuthClient;
        } else {
            return null;
        }
    }

}
