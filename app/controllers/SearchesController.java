https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import database.ListDBAccessor;
import database.ProspectStateDBAccessor;
import database.SearchDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.*;
import model.account.Account;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.lists.ProspectSearch;
import model.lists.SavedList;
import model.serialization.MagicListObject;
import play.Logger;
import play.mvc.Result;
import play.mvc.Security;
import utilities.Validation;
import views.html.search.*;
import views.html.failure.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class SearchesController extends BaseController {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public SearchesController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getSearchDetails() {
        Map<String, String[]> map = request().queryString();
        Account account = Account.getAccountFromSession();

        List<ProspectState> allProspectStates = new ProspectStateDBAccessor(getDatabase())
                .getAllStates(account.getCompanyName());

        ProspectSearch search = ProspectSearch.Factory.createFromRequest(map, allProspectStates);
        if(search == null) {
            logger.error("Invalid search params sent for user: {}", account.getUserId());
            String error = "There was an error performing your search. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        logger.trace("Search Object: {}", MagicListObject.prettyPrint(search));

        PagedList<Prospect> personList = new SearchDBAccessor(getDatabase())
                .getProspectsFromSearch(account.getUserId(), search);

        String listName = "My Contacts";
        if (!Validation.isEmpty(search.getListId())) {
            SavedList savedList = new ListDBAccessor(getDatabase())
                    .getUserListById(account.getUserId(), search.getListId());
            if (savedList == null) {
                logger.error("Could not retrieve list for account: {}", account.getUserId());
                String error = "There was an error retrieving your list. Please try again or submit a bug report";
                return internalServerError(FailurePage.render(error));
            }

            listName = savedList.getListName();
        }

        if (personList != null) {
            return ok(SearchResultsPage.render(personList, search, listName));
        } else {
            String reason = "There was an error retrieving your contacts. Please try again or submit a bug report so " +
                    "the issue can be resolved";
            return internalServerError(FailurePage.render(reason));
        }
    }

}