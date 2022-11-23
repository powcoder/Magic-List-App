https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import database.ManagerSharingDBAccessor;
import global.authentication.ManagerAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import com.typesafe.config.Config;
import play.Environment;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.ws.WSClient;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Corey on 6/9/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerSharingController extends BaseController {

    public static final String KEY_ORIGIN_ID = "origin_id";
    public static final String KEY_DESTINATION_ID = "destination_id";
    public static final String KEY_LIST_ID = "list_id";
    public static final String KEY_PERSON_IDS = "person_ids";

    private final ManagerSharingDBAccessor managerSharingDBAccessor;

    @Inject
    public ManagerSharingController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        managerSharingDBAccessor = new ManagerSharingDBAccessor(controllerComponents.getDatabase());
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(ManagerAuthenticator.class)
    public Result transferAllContacts() {
        JsonNode node = request().body().asJson();

        String originId = Validation.string(KEY_ORIGIN_ID, node);
        if (Validation.isEmpty(originId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_ORIGIN_ID));
        }

        String destinationId = Validation.string(KEY_DESTINATION_ID, node);
        if (Validation.isEmpty(destinationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_DESTINATION_ID));
        }

        String managerId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = managerSharingDBAccessor.transferAllProspectsAndLists(managerId, originId, destinationId);

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(ManagerAuthenticator.class)
    public Result transferList() {
        JsonNode node = request().body().asJson();

        String originId = Validation.string(KEY_ORIGIN_ID, node);
        if (Validation.isEmpty(originId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_ORIGIN_ID));
        }

        String destinationId = Validation.string(KEY_DESTINATION_ID, node);
        if (Validation.isEmpty(destinationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_DESTINATION_ID));
        }

        String listId = Validation.string(KEY_LIST_ID, node);
        if (Validation.isEmpty(listId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_LIST_ID));
        }

        String managerId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = managerSharingDBAccessor.transferList(managerId, originId, destinationId, listId);

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(ManagerAuthenticator.class)
    public Result transferProspects() {
        JsonNode node = request().body().asJson();

        String originId = Validation.string(KEY_ORIGIN_ID, node);
        if (Validation.isEmpty(originId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_ORIGIN_ID));
        }

        String destinationId = Validation.string(KEY_DESTINATION_ID, node);
        if (Validation.isEmpty(destinationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_DESTINATION_ID));
        }

        JsonNode rawArrayNode = node.get(KEY_PERSON_IDS);
        if (rawArrayNode == null || !(rawArrayNode instanceof ArrayNode)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_PERSON_IDS));
        }

        ArrayNode arrayNode = (ArrayNode) rawArrayNode;
        List<String> personIdList = StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        String managerId = Account.getAccountFromSession().getUserId();

        boolean isSuccessful = managerSharingDBAccessor.transferProspects(managerId, originId, destinationId, personIdList);

        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }
}
