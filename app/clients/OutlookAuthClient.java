https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.BaseController;
import model.calendar.CalendarEvent;
import model.calendar.CalendarProvider;
import model.oauth.OAuthAccount;
import model.oauth.OAuthProvider;
import model.oauth.OAuthToken;
import model.outlook.CalendarAppointment;
import model.prospect.Appointment;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import utilities.FutureUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class OutlookAuthClient extends OAuthClient {

    private Logger.ALogger logger = Logger.of(this.getClass());

    public static OutlookAuthClient getInstance(WSClient wsClient, String clientId, String clientSecret) {
        if (outlookAuthClient == null) {
            outlookAuthClient = new OutlookAuthClient(wsClient, clientId, clientSecret);
        }
        return outlookAuthClient;
    }

    private static OutlookAuthClient outlookAuthClient;

    private static final String OUTLOOK_ACCESS_TOKEN_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String OUTLOOK_PROFILE_ENDPOINT = "https://graph.microsoft.com/v1.0/me";
    private static final String OUTLOOK_CALENDAR_ENDPOINT = "https://graph.microsoft.com/v1.0/me/events";
    private static final String OUTLOOK_BATCH_REQUEST_ENDPOINT = "https://graph.microsoft.com/beta/$batch";
    private static final String OUTLOOK_CALENDAR_GET_EVENT_ENDPOINT = "https://graph.microsoft.com/v1.0/me/events/";

    /**
     * The Microsoft Graph batch API can only handle, at most, 5 requests at a time.
     */
    private static final int BATCH_LIMIT = 5;

    private OutlookAuthClient(WSClient wsClient, String clientId, String clientSecret) {
        super(wsClient, clientId, clientSecret);
    }

    public OAuthProvider getProvider() {
        return OAuthProvider.OUTLOOK;
    }

    public CompletionStage<OAuthToken> exchangeCodeForAuthToken(String code) {
        String redirectUrl = controllers.routes.OAuthController.loginWithOutlookCallback()
                .absoluteURL(Http.Context.current().request(), BaseController.isSecure);
        return super.exchangeCodeForAuthToken(redirectUrl, OUTLOOK_ACCESS_TOKEN_ENDPOINT, code);
    }

    public CompletionStage<OAuthToken> refreshAccessToken(String oauthAccountId, OAuthToken authToken) {
        return super.refreshAccessToken(oauthAccountId, OUTLOOK_ACCESS_TOKEN_ENDPOINT, authToken);
    }

    /**
     * @param authToken An auth token that is already refreshed and should <b>never</b> be expired.
     */
    public CompletionStage<OAuthAccount> getAccountInformation(OAuthToken authToken) {
        WSRequest request = getWsClient().url(OUTLOOK_PROFILE_ENDPOINT)
                .addHeader("Authorization", "Bearer " + authToken.getAccessToken())
                .addHeader("Content-Type", "application/json");

        return performRequest(request.get())
                .thenApply(wsResponse -> {
                    if (wsResponse.getStatus() == 200) {
                        return new OAuthAccount.OutlookConverter()
                                .deserializeFromJson((ObjectNode) wsResponse.asJson());
                    } else {
                        return null;
                    }
                });
    }

    /**
     * @return A completion stage containing the {@link CalendarEvent}, or null if an error occurred
     */
    public CompletionStage<CalendarEvent> createOutlookEvent(OAuthToken authToken, String outlookAccountId,
                                                             CalendarAppointment calendarAppointment) {
        WSRequest request = getWsClient().url(OUTLOOK_CALENDAR_ENDPOINT)
                .addHeader(BaseController.AUTHORIZATION, "Bearer " + authToken.getAccessToken())
                .addHeader(BaseController.CONTENT_TYPE, "application/json");

        return performRequest(request.post(new CalendarAppointment.Converter(outlookAccountId).renderAsJsonObject(calendarAppointment)))
                .thenApply(response -> {
                    if (response.getStatus() >= 200 && response.getStatus() < 300) {
                        logger.info("Response [SUCCESS] for create event for account: {}", outlookAccountId);
                        logger.debug("Response [DEBUG] for create event: {}", response.getBody());
                        try {
                            JsonNode node = Json.parse(response.getBody());
                            String eventId = node.get(CalendarAppointment.KEY_OUTLOOK_EVENT_ID).asText();
                            String webLink = node.get(CalendarAppointment.KEY_WEB_LINK).asText();
                            String subject = node.get(CalendarAppointment.KEY_SUBJECT).asText();
                            return new CalendarEvent(CalendarProvider.OUTLOOK, eventId, outlookAccountId, webLink, subject);
                        } catch (Exception e) {
                            logger.error("Error parsing json", e);
                            return null;
                        }
                    } else {
                        logger.error("Response [{}] for create event for account: [{}]: {}", response.getStatus(),
                                outlookAccountId, response.getBody(), new IllegalStateException());
                        return null;
                    }
                });
    }

    public CompletableFuture<List<CalendarEvent>> getBatchedCalendarEvents(String outlookAccountId, OAuthToken authToken,
                                                                           List<String> calendarEventIds,
                                                                           Map<String, Appointment> calendarEventsToAppointments,
                                                                           OnCalendarEventNotFoundListener listener) {
        List<CompletableFuture<List<CalendarEvent>>> calendarEventCompletionStageList = new ArrayList<>();

        for (int i = 0; i < calendarEventIds.size(); i += BATCH_LIMIT) {
            List<String> tempEventIds = calendarEventIds.stream().skip(i).limit(BATCH_LIMIT).collect(Collectors.toList());
            calendarEventCompletionStageList.add(
                    getCalendarEventsFromBatchedRequest(tempEventIds, authToken, outlookAccountId,
                            calendarEventsToAppointments, listener)
            );
        }

        return FutureUtility.combineStages(calendarEventCompletionStageList);
    }

    // Private Methods

    private static synchronized CompletionStage<WSResponse> performRequest(CompletionStage<WSResponse> request) {
        return CompletableFuture.completedFuture(request.toCompletableFuture().join());
    }

    private CompletableFuture<List<CalendarEvent>> getCalendarEventsFromBatchedRequest(List<String> calendarEventIds,
                                                                                       OAuthToken authToken,
                                                                                       String outlookAccountId,
                                                                                       Map<String, Appointment> calendarEventsToAppointments,
                                                                                       OnCalendarEventNotFoundListener listener) {
        if (calendarEventIds.size() > BATCH_LIMIT) {
            throw new RuntimeException("Cannot send more than 5 requests at once!");
        }

        ObjectNode baseNode = Json.newObject();
        ArrayNode arrayNode = baseNode.putArray("requests");

        calendarEventIds.forEach(eventId -> arrayNode.addObject()
                .put("id", eventId)
                .put("method", "GET")
                .put("url", "/me/events/" + eventId)
        );

        logger.debug("Retrieving {} Outlook Events via batched request", calendarEventIds.size());
        logger.debug("Node being sent: {}", baseNode);

        WSRequest request = getWsClient().url(OUTLOOK_BATCH_REQUEST_ENDPOINT)
                .addHeader(BaseController.AUTHORIZATION, "Bearer " + authToken.getAccessToken())
                .addHeader(BaseController.CONTENT_TYPE, "application/json")
                .addHeader(BaseController.ACCEPT, "application/json");

        return performRequest(request.post(baseNode))
                .thenApplyAsync(wsResponse -> {
                    if (wsResponse.getStatus() != 200) {
                        logger.error("Received [Status: {}], [Body: {}], from: {}", wsResponse.getStatus(),
                                wsResponse.getBody(), OUTLOOK_CALENDAR_GET_EVENT_ENDPOINT);
                        return null;
                    }

                    List<CalendarEvent> calendarEvents = new ArrayList<>();

                    logger.debug("Response from Outlook batched events: {}", wsResponse.getBody());

                    ArrayNode requests = (ArrayNode) Json.parse(wsResponse.getBody()).get("responses");

                    for (int i = 0; i < requests.size(); i++) {
                        ObjectNode responseNode = (ObjectNode) requests.get(i);
                        logger.debug("Node: {}", responseNode);

                        String calendarEventId = responseNode.get("id").asText();
                        int status = responseNode.get("status").asInt();
                        if (status == 200 && responseNode.get("body").getNodeType() == JsonNodeType.OBJECT) {
                            ObjectNode bodyNode = (ObjectNode) responseNode.get("body");
                            String hyperLink = bodyNode.get(CalendarAppointment.KEY_WEB_LINK).asText();
                            String subject = bodyNode.get(CalendarAppointment.KEY_SUBJECT).asText();

                            calendarEvents.add(new CalendarEvent(CalendarProvider.OUTLOOK, calendarEventId,
                                    outlookAccountId, hyperLink, subject));
                        } else {
                            if (status == 404) {
                                // The event doesn't exist anymore on Outlook, so purge it
                                listener.onCalendarEventNotFound(calendarEventId, calendarEventsToAppointments);
                            } else {
                                String errorCode = null;
                                if (responseNode.get("body").getNodeType() == JsonNodeType.OBJECT) {
                                    errorCode = responseNode.get("body").get("error").get("code").asText();
                                }
                                if ("ErrorItemNotFound".equals(errorCode)) {
                                    // The event doesn't exist anymore on Outlook so purge it
                                    listener.onCalendarEventNotFound(calendarEventId, calendarEventsToAppointments);
                                } else {
                                    logger.error("Error: while contacting Outlook: {}", wsResponse.getBody());
                                }

                            }
                        }
                    }

                    return calendarEvents;
                }, BaseController.getHttpExecutionContext().current()).toCompletableFuture()
                .exceptionally(e -> {
                    logger.error("Caught an error while processing JSON", e);
                    return null;
                });
    }

}
