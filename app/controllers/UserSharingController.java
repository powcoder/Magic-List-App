https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import database.ManagerMiscDBAccessor;
import database.ManagerSharingDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by Corey on 6/10/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class UserSharingController extends BaseController {

    private final ManagerMiscDBAccessor managerMiscDBAccessor;
    private final ManagerSharingDBAccessor managerSharingDBAccessor;

    @Inject
    public UserSharingController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        managerMiscDBAccessor = new ManagerMiscDBAccessor(getDatabase());
        managerSharingDBAccessor = new ManagerSharingDBAccessor(getDatabase());
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result transferProspects() {
        JsonNode node = request().body().asJson();

        String destinationId = Validation.string(ManagerSharingController.KEY_DESTINATION_ID, node);
        if (Validation.isEmpty(destinationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(ManagerSharingController.KEY_DESTINATION_ID));
        }

        JsonNode rawArrayNode = node.get(ManagerSharingController.KEY_PERSON_IDS);
        if (!(rawArrayNode instanceof ArrayNode)) { // This covers if rawArrayNode == null too
            return badRequest(ResultUtility.getNodeForMissingField(ManagerSharingController.KEY_PERSON_IDS));
        }

        ArrayNode arrayNode = (ArrayNode) rawArrayNode;
        List<String> personIdList = StreamSupport.stream(arrayNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        int maxProspectIds = 1000;
        if (personIdList.size() > maxProspectIds) {
            String failureReason = "Too many prospects to transfer. Keep it less than " + maxProspectIds;
            return badRequest(ResultUtility.getNodeForBooleanResponse(failureReason));
        }

        String originId = Account.getAccountFromSession().getUserId();

        Optional<List<String>> managerId = managerMiscDBAccessor.getManagerIdFromUserId(originId);

        if (managerId.isPresent() && !managerId.get().isEmpty()) {
            boolean isSuccessful = managerSharingDBAccessor
                    .transferProspects(managerId.get().get(0), originId, destinationId, personIdList);
            return getStandardResultForOperation(isSuccessful);
        } else {
            return getResultForNoManager(managerId.isPresent());
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result transferList() {
        JsonNode node = request().body().asJson();

        String destinationId = Validation.string(ManagerSharingController.KEY_DESTINATION_ID, node);
        if (Validation.isEmpty(destinationId)) {
            return badRequest(ResultUtility.getNodeForMissingField(ManagerSharingController.KEY_DESTINATION_ID));
        }

        String listId = Validation.string(ManagerSharingController.KEY_LIST_ID, node);
        if (Validation.isEmpty(listId)) {
            return badRequest(ResultUtility.getNodeForMissingField(ManagerSharingController.KEY_LIST_ID));
        }

        String originId = Account.getAccountFromSession().getUserId();

        Optional<List<String>> managerId = managerMiscDBAccessor.getManagerIdFromUserId(originId);

        if (managerId.isPresent() && !managerId.get().isEmpty()) {
            boolean isSuccessful = managerSharingDBAccessor
                    .transferList(managerId.get().get(0), originId, destinationId, listId);
            return getStandardResultForOperation(isSuccessful);
        } else {
            return getResultForNoManager(managerId.isPresent());
        }
    }

    private Result getStandardResultForOperation(boolean isSuccessful) {
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }
    }

    private Result getResultForNoManager(boolean isManagerIdPresent) {
        if (isManagerIdPresent) {
            String reason = "You are not on a team currently and have no manager.";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "A server error occurred. Please try again or submit a bug report";
            return internalServerError(reason);
        }
    }

}
