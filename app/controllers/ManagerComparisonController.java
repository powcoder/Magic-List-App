https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import akka.japi.Pair;
import com.fasterxml.jackson.databind.node.ArrayNode;
import database.ManagerComparisonDBAccessor;
import model.ControllerComponents;
import model.graph.GraphSqlGetter;
import database.ManagerMiscDBAccessor;
import global.authentication.ManagerAuthenticator;
import model.account.Account;
import model.graph.GraphOutputDateFormat;
import model.graph.GraphStatistic;
import model.manager.Employee;
import com.typesafe.config.Config;
import play.Environment;
import play.Logger;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ListUtility;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.*;
import views.html.manager.ManagerComparisonPage;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Corey on 6/12/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerComparisonController extends BaseController {

    private static final String KEY_FILTER_WEEKENDS = "filter_weekends";
    private static final String KEY_START_DATE = "start_date";
    private static final String KEY_END_DATE = "end_date";

    private final Logger.ALogger logger = Logger.of(this.getClass());
    private final ManagerComparisonDBAccessor managerComparisonDBAccessor;

    @Inject
    public ManagerComparisonController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        managerComparisonDBAccessor = new ManagerComparisonDBAccessor(controllerComponents.getDatabase());
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getEmployeeComparisonPage() {
        String managerId = Account.getAccountFromSession().getUserId();

        List<Employee> employeeList = new ManagerMiscDBAccessor(getDatabase())
                .getManagerEmployees(managerId, true);
        if (employeeList != null) {
            return ok(ManagerComparisonPage.render(employeeList));
        } else {
            String reason = "An error occurred retrieving your team. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(ManagerAuthenticator.class)
    public Result getCompareEmployeesOverRange(String type) {
        Map<String, String[]> queryString = request().queryString();
        String managerId = Account.getAccountFromSession().getUserId();

        GraphOutputDateFormat format = GraphOutputDateFormat.parse(type);
        if (format == null) {
            String reason = "Invalid date format entered, found: " + type;
            return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
        }

        String rawStartDate = Validation.string(KEY_START_DATE, queryString);
        String rawEndDate = Validation.string(KEY_END_DATE, queryString);

        Pair<Date, Date> startEndDatePair = null;
        if (!Validation.isEmpty(rawStartDate) && !Validation.isEmpty(rawEndDate)) {
            Date startDate;
            Date endDate;
            DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
            try {
                startDate = dateFormat.parse(rawStartDate);
            } catch (ParseException e) {
                logger.debug("Invalid start date found for request: {}", rawStartDate);
                return badRequest(ResultUtility.getNodeForBooleanResponse("Invalid start date entered"));
            }
            try {
                endDate = dateFormat.parse(rawEndDate);
            } catch (ParseException e) {
                logger.debug("Invalid end date found for request: {}", rawEndDate);
                return badRequest(ResultUtility.getNodeForBooleanResponse("Invalid end date entered"));
            }

            logger.debug("Start date: " + startDate);
            logger.debug("End date: " + endDate);

            if (startDate.getTime() > endDate.getTime()) {
                String reason = "The start time cannot be greater than the end time";
                return badRequest(ResultUtility.getNodeForBooleanResponse(reason));
            }
            startEndDatePair = new Pair<>(startDate, endDate);
        }

        boolean filterWeekends = Validation.bool(KEY_FILTER_WEEKENDS, queryString);

        // Collect the functions from the comparison DB accessor that generate charts, in this order
        List<EmployeeGraphGetter> graphGetterList = ListUtility.asList(
                managerComparisonDBAccessor::getContactsToDials,
                managerComparisonDBAccessor::getAppointmentsToContacts,
                managerComparisonDBAccessor::getAppointments,
                managerComparisonDBAccessor::getMissedDiscoveries,
                managerComparisonDBAccessor::getObjections,
                managerComparisonDBAccessor::getLimbosConvertedToAppointments,
                managerComparisonDBAccessor::getAppointmentsConvertedToOpenInventory,
                managerComparisonDBAccessor::getAppointmentsByCompany,
                managerComparisonDBAccessor::getAppointmentsByAreaCode,
                managerComparisonDBAccessor::getUploadedContacts
        );

        F.Either<Result, Result> either = getGraphsForEmployeeComparison(graphGetterList, managerId, format, startEndDatePair, filterWeekends);

        return either.left.orElseGet(either.right::get);
    }

    private F.Either<Result, Result> getGraphsForEmployeeComparison(List<EmployeeGraphGetter> graphGetterList,
                                                                    String managerId, GraphOutputDateFormat format,
                                                                    Pair<Date, Date> startEndDatePair,
                                                                    boolean filterWeekends) {
        ArrayNode nodes = Json.newArray();
        for (EmployeeGraphGetter<?> graphGetter : graphGetterList) {
            GraphSqlGetter sqlGetter = new GraphSqlGetter(format, startEndDatePair, filterWeekends);
            GraphStatistic graphStatistic = graphGetter.getManagerGraph(managerId, sqlGetter);
            if (graphStatistic == null) {
                return F.Either.Left(internalServerError(ResultUtility.getNodeForBooleanResponse(false)));
            } else {
                nodes.add(graphStatistic.renderAsJsonObject());
            }
        }

        return F.Either.Right(ok(nodes));
    }

    private interface EmployeeGraphGetter<T extends GraphStatistic> {

        T getManagerGraph(String managerId, GraphSqlGetter sqlGetter);
    }

}
