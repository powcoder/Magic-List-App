https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.gson.reflect.TypeToken;
import database.ListDBAccessor;
import database.ProspectStateDBAccessor;
import excel.ExcelDocumentParser;
import global.authentication.SubscriptionAuthenticator;
import model.*;
import model.account.Account;
import model.database.SavedListDatabaseResult;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.lists.ProspectSearch;
import model.lists.SavedList;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utilities.RandomStringGenerator;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.*;
import views.html.lists.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static model.database.SavedListDatabaseResult.ERROR_TOO_MANY_IMPORTS;
import static model.database.SavedListDatabaseResult.SUCCESS;

@Singleton
public class ListsController extends BaseController {

    @SuppressWarnings("WeakerAccess")
    public static final String KEY_PERSON_LIST = "person_list";
    public static final String KEY_LIST_NAME = "list_name";
    @SuppressWarnings("WeakerAccess")
    public static final String KEY_LIST_IDS = "list_ids";

    private final ListDBAccessor listDBAccessor;
    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public ListsController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        listDBAccessor = new ListDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result insertListFromWeb() {
        JsonNode node = request().body().asJson();
        Account account = Account.getAccountFromSession();
        return insertPersonList(node, account);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result createListFromPredicates() {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        String listName = Validation.string(SavedList.KEY_LIST_NAME, node);
        if (Validation.isEmpty(listName)) {
            return badRequest(ResultUtility.getNodeForMissingField(SavedList.KEY_LIST_NAME));
        }

        List<ProspectState> allProspectStates = new ProspectStateDBAccessor(getDatabase())
                .getAllStates(account.getCompanyName());
        if (allProspectStates == null) {
            logger.error("Could not retrieve prospect states: [account: {}]", account.getUserId());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        }

        ProspectSearch search = ProspectSearch.Factory.createFromRequest(node, allProspectStates);
        logger.debug("Search: {}", MagicListObject.prettyPrint(search));

        String searchId = RandomStringGenerator.getInstance().getNextRandomListId();
        long date = Calendar.getInstance().getTimeInMillis();
        String comment = Validation.string(SavedList.KEY_COMMENT, node);
        SavedList savedList = new SavedList(searchId, listName, date, comment);

        SavedListDatabaseResult result = listDBAccessor.insertListFromPredicates(account, savedList, search);
        if (result == SUCCESS) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "There was an error uploading your new list from predicates. Please try again or submit bug report.";
            logger.error(reason, new IllegalStateException());
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result setListComment(String searchId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (searchId == null) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Invalid search ID"));
        }

        String comment = Validation.string(SavedList.KEY_COMMENT, node);
        Optional<Boolean> isSuccessful = new ListDBAccessor(getDatabase())
                .setListComments(account.getUserId(), searchId, comment);
        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "Invalid list ID entered. Could not update list.";
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "Could not update the list\'s comment. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result setListName(String searchId) {
        Account account = Account.getAccountFromSession();
        JsonNode node = request().body().asJson();

        if (searchId == null) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Invalid search ID"));
        }

        String searchName = Validation.string(SavedList.KEY_LIST_NAME, node);
        if (searchName == null) {
            return badRequest(ResultUtility.getNodeForInvalidField(SavedList.KEY_LIST_NAME));
        }

        boolean isSuccessful = listDBAccessor.setListName(account.getUserId(), searchId, searchName);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "Could not update the search\'s name";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    @BodyParser.Of(BodyParser.Json.class)
    public Result deleteList(String searchId) {
        Account account = Account.getAccountFromSession();

        if (searchId == null) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("Invalid search ID"));
        }

        Optional<Boolean> isSuccessful = listDBAccessor.deleteList(account.getUserId(), searchId);
        if (isSuccessful.isPresent() && isSuccessful.get()) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (isSuccessful.isPresent()) {
            String reason = "You cannot delete a list that was shared to you";
            return status(409, ResultUtility.getNodeForBooleanResponse(reason));
        } else {
            String reason = "Could not delete list";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getLists() {
        Map<String, String[]> map = request().queryString();
        Account account = Account.getAccountFromSession();

        int currentPage = Validation.page(KEY_PAGE, map);

        SavedList.Sorter sortBy = SavedList.Sorter.parse(Validation.string(KEY_SORT_BY, map));

        boolean isAscending = Validation.bool(KEY_ASCENDING, map, true);

        PagedList<SavedList> savedLists = listDBAccessor.getUserLists(account.getUserId(), currentPage, sortBy, isAscending);

        if (savedLists != null) {
            return ok(ListsPage.render(savedLists, sortBy, isAscending));
        } else {
            String reason = "Could not load your lists. Please try again or  submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUploadNewListPage() {
        String errorText = flash(KEY_ERROR);
        return ok(UploadNewListPage.render(errorText));
    }

    @BodyParser.Of(BodyParser.MultipartFormData.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> previewNewList() {
        Http.MultipartFormData.FilePart<Object> filePart = request().body().asMultipartFormData().getFile("file");
        File file = (File) filePart.getFile();

        if (Validation.isEmpty(filePart.getFilename())) {
            flash(KEY_ERROR, "The uploaded file\'s name is invalid");
            return wrapInFuture(redirect(routes.ListsController.getUploadNewListPage()));
        }

        Account account = Account.getAccountFromSession();

        // We don't want to overflow the server with too many files to parse, so only allow a couple at a time
        return CompletableFuture.supplyAsync(() -> {
            try (ExcelDocumentParser documentParser = new ExcelDocumentParser(file)) {
                if (!documentParser.isFileValid()) {
                    flash(KEY_ERROR, "The uploaded file is not a valid excel file");
                    return redirect(routes.ListsController.getUploadNewListPage());
                }

                String filename = filePart.getFilename();
                if (filename.lastIndexOf(".") != -1) {
                    filename = filename.substring(0, filename.lastIndexOf("."));
                }

                List<Prospect> personList = documentParser.getProspectListFromExcel();

                if (personList.size() > 10000) {
                    flash(KEY_ERROR, "You may not upload more than 10,000 contacts at a time. Please split up the " +
                            "excel file into multiple files and try again.");
                    return redirect(routes.ListsController.getUploadNewListPage());
                } else {
                    return ok(PreviewNewListPage.render(filename, personList, documentParser.getInvalidEmailLinesMap()));
                }
            } catch (Exception e) {
                logger.error("Error: ", e);
                flash(KEY_ERROR, e.getMessage());
                return redirect(routes.ListsController.getUploadNewListPage());
            } finally {
                if (!file.delete()) {
                    logger.error("Could not delete file for user: {}", account.getUserId());
                    logger.error("Error: ", new IllegalStateException());
                }
            }
        }, getHttpExecutionContext().current()).exceptionally(throwable -> {
            logger.error("Error: ", throwable);

            String message;
            if (throwable instanceof OutOfMemoryError) {
                message = "This file is too large. Please try again by breaking down the file into multiple \"sub files\".";
            } else {
                message = throwable.getMessage();
            }
            flash(KEY_ERROR, message);
            return redirect(routes.ListsController.getUploadNewListPage());
        });
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result mergeLists() {
        JsonNode node = request().body().asJson();

        logger.debug("Form Content: {}", node);

        if (!node.has(KEY_LIST_IDS) || !(node.get(KEY_LIST_IDS) instanceof ArrayNode)) {
            logger.debug("List IDs: {}", node.get(KEY_LIST_IDS));
            return badRequest(ResultUtility.getNodeForMissingField(KEY_LIST_IDS));
        }

        ArrayNode listIdsNode = (ArrayNode) node.get(KEY_LIST_IDS);

        List<String> listIdList = StreamSupport.stream(listIdsNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());

        if (listIdList.size() <= 1) {
            return badRequest(ResultUtility.getNodeForBooleanResponse("You must merge at least 2 lists"));
        }

        String listName = Validation.string(KEY_LIST_NAME, node);
        if (Validation.isEmpty(listName)) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_LIST_NAME));
        }

        String listId = RandomStringGenerator.getInstance().getNextRandomListId();

        boolean isSuccessful = listDBAccessor.mergeLists(listId, listName, Account.getAccountFromSession().getUserId(), listIdList);
        if (isSuccessful) {
            SavedList savedList = new SavedList(listId, listName, Calendar.getInstance().getTimeInMillis(), null);
            return sendJsonOk(MagicListObject.serializeToJson(savedList));
        } else {
            String reason = "There was an error inserting your merged lists.";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPreviewNewListPage() {
        return redirect(routes.ListsController.getUploadNewListPage());
    }

//    Private Methods

    private Result insertPersonList(JsonNode node, Account account) {
        String listName = Validation.string(KEY_LIST_NAME, node);
        if (Validation.isEmpty(listName)) {
            logger.info("Invalid list name for user: {}", account.getUserId());
            return badRequest(ResultUtility.getNodeForMissingField(KEY_LIST_NAME));
        }

        ArrayNode personArray;
        try {
            JsonNode personNode = node.get(KEY_PERSON_LIST);
            if (personNode == null) {
                logger.info("Invalid person list for user: {}", account.getUserId());
                return badRequest(ResultUtility.getNodeForMissingField(KEY_PERSON_LIST));
            }
            if (personNode.getNodeType() != JsonNodeType.ARRAY) {
                logger.info("Invalid person list node type for user: {}", account.getUserId());
                return badRequest(ResultUtility.getNodeForInvalidField(KEY_PERSON_LIST));
            }
            personArray = (ArrayNode) personNode;
            if (personArray.size() == 0 || personArray.get(0).getNodeType() != JsonNodeType.OBJECT) {
                logger.info("Invalid person list array size or object type for user: {}", account.getUserId());
                String reason = "Person array should be of type Person Object";
                return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
            }
        } catch (Exception e) {
            logger.error("The person array could not be cast", e);
            return internalServerError(ResultUtility.getNodeForBooleanResponse("JSON parsing error"));
        }

        List<Prospect> personList = MagicListObject.deserializeListFromJson(personArray, Prospect[].class);

        if (personList.stream().anyMatch(prospect -> Validation.isEmpty(prospect.getName()))) {
            logger.info("Invalid person name in list for user: {}", account.getUserId());
            return badRequest(ResultUtility.getNodeForBooleanResponse("All of the people must have names associated with them"));
        }

        personList = personList.stream().map(p -> Prospect.Factory.createFromRawInput(p.getId(), p.getName(),
                p.getEmail(), p.getPhoneNumber(), p.getJobTitle(), p.getCompanyName(), p.getNotes()))
                .collect(Collectors.toList());

        String listId = RandomStringGenerator.getInstance().getNextRandomListId();
        long date = Calendar.getInstance().getTimeInMillis();
        SavedList savedList = new SavedList(listId, listName, date, null);
        SavedListDatabaseResult result;
        result = listDBAccessor.insertListFromWebUpload(account, savedList, personList);

        if (result == SUCCESS) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else if (result == ERROR_TOO_MANY_IMPORTS) {
            String error = "You have surpassed your limit for this month";
            logger.info("User has surpassed import limit: {}", account.getUserId());
            return badRequest(ResultUtility.getNodeForBooleanResponse(error));
        } else {
            String reason = "A server error occurred while inserting your list. Please try again or submit a bug report.";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
