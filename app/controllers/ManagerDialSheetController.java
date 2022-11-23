https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import database.DialSheetDBAccessor;
import database.ManagerDialSheetDBAccessor;
import database.ManagerMiscDBAccessor;
import global.authentication.ManagerAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.dialsheet.*;
import model.manager.Employee;
import play.mvc.Result;
import play.mvc.Security;
import utilities.Validation;
import views.html.dialsheets.DialSheetDetailsPage;
import views.html.failure.FailurePage;
import views.html.manager.*;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Corey on 6/11/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerDialSheetController extends BaseController {

    private final ManagerDialSheetDBAccessor managerDialSheetDBAccessor;
    private final ManagerMiscDBAccessor managerMiscDBAccessor;

    @Inject
    public ManagerDialSheetController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        managerDialSheetDBAccessor = new ManagerDialSheetDBAccessor(getDatabase());
        managerMiscDBAccessor = new ManagerMiscDBAccessor(getDatabase());
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getDialSheetHistoryPage() {
        String managerId = Account.getAccountFromSession().getUserId();

        // TODO fix me
        List<ManagerDialSheet> managerDialSheets = managerDialSheetDBAccessor.getTeamDialSheets(managerId, new Date());
        if (managerDialSheets != null) {
            return ok(ManagerDialSheetSummaryPage.render(managerDialSheets));
        } else {
            String reason = "There was an error retrieving your team\'s activity sheets. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getDialSheetDatePage(String rawDate) {
        Date date;
        try {
            date = new SimpleDateFormat("MM-dd-yyyy")
                    .parse(rawDate);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(FailurePage.render("The date entered to access this page is invalid"));
        }

        String managerId = Account.getAccountFromSession().getUserId();

        List<Employee> employeeList = managerMiscDBAccessor.getManagerEmployees(managerId, true);
        if (employeeList != null && employeeList.isEmpty()) {
            return ok(ManagerDialSheetDatePage.render(null, date.getTime(), false));
        } else if (employeeList == null) {
            String reason = "There was an error retrieving your team. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }

        List<EmployeeDialSheet> employeeDialSheetList = managerDialSheetDBAccessor.getTeamDialSheetsForDate(managerId, date);

        if (employeeDialSheetList != null) {
            return ok(ManagerDialSheetDatePage.render(employeeDialSheetList, date.getTime(), true));
        } else {
            String reason = "There was an error retrieving your team\'s activity sheets. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getDialSheetDetailsPage(String employeeId, String sheetId) {
        if (Validation.isEmpty(sheetId)) {
            String reason = "You were sent to this page by a bad link. If you believe this is an error, please submit a bug report";
            return badRequest(FailurePage.render(reason));
        }

        String managerId = Account.getAccountFromSession().getUserId();

        Optional<Employee> employee = managerMiscDBAccessor.getEmployeeById(managerId, employeeId);

        if (!employee.isPresent()) {
            String reason = "This user is not on your team";
            return badRequest(FailurePage.render(reason));
        }

        DialSheetDBAccessor dialSheetDBAccessor = new DialSheetDBAccessor(getDatabase());

        DialSheet dialSheet = dialSheetDBAccessor.getDialSheetById(employeeId, sheetId);

        if (dialSheet == null) {
            String error = "There was an error retrieving your dial sheet. Please try again or submit a bug report.";
            return internalServerError(FailurePage.render(error));
        }

        DialSheetDetails dialSheetDetails = dialSheetDBAccessor.getDailyDialSheetDetails(employeeId, new Date(dialSheet.getDate()));

        if (dialSheetDetails != null) {
            DialSheetPaginationSummary previousDialSheet = dialSheetDBAccessor.getPreviousDialSheet(employeeId, sheetId);
            DialSheetPaginationSummary nextDialSheet = dialSheetDBAccessor.getNextDialSheet(employeeId, sheetId);
            return ok(DialSheetDetailsPage.render(dialSheetDetails, previousDialSheet, nextDialSheet, true,
                    employee.get(), DialSheetType.DAY));
        } else {
            String reason = "There was an error retrieving this team member\'s activity sheet. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

}
