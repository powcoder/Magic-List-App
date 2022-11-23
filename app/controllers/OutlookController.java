https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import clients.OutlookAuthClient;
import database.CalendarDBAccessor;
import database.OAuthDBAccessor;
import database.OutlookDBAccessor;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.account.Account;
import model.calendar.BaseCalendarTemplate;
import model.dialsheet.DialSheetAppointment;
import model.oauth.OAuthAccount;
import model.oauth.OAuthToken;
import model.outlook.CalendarAppointment;
import model.outlook.CalendarTemplate;
import model.outlook.OutlookCalendarFactory;
import model.prospect.Prospect;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;
import utilities.DebugUtility;
import utilities.FutureUtility;
import utilities.ResultUtility;
import utilities.Validation;
import views.html.failure.FailurePage;
import views.html.outlook.*;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static utilities.ResultUtility.getNodeForMissingField;

public class OutlookController extends BaseController {

    private final OutlookAuthClient outlookAuthClient;


    @Inject
    public OutlookController(ControllerComponents controllerComponents) {
        super(controllerComponents);
        outlookAuthClient = OutlookAuthClient.getInstance(getWsClient(), getOutlookClientId(), getOutlookClientSecret());
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getCreateTemplatePage() {
        Account account = Account.getAccountFromSession();
        List<OAuthAccount> accountList = new OAuthDBAccessor(getDatabase())
                .getAccounts(account.getUserId());
        if (accountList != null) {
            String accountId = accountList.stream()
                    .map(OAuthAccount::getAccountId)
                    .findFirst()
                    .orElse(null);
            return ok(CreateOutlookTemplatePage.render(accountId));
        } else {
            return internalServerError(FailurePage.render("Could not retrieve account information"));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result createNewTemplate() {
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        DebugUtility.printFormContent(form);

        String outlookAccountId = Validation.string(CalendarAppointment.KEY_OUTLOOK_USER_ID, form);
        if (Validation.isEmpty(outlookAccountId)) {
            return badRequest(getNodeForMissingField(CalendarAppointment.KEY_OUTLOOK_USER_ID));
        }

        CalendarTemplate template;
        try {
            template = OutlookCalendarFactory.createTemplateFromForm(form);
        } catch (Exception e) {
            return getRedirectFailure(e.getMessage());
        }

        boolean isSuccessful = new OutlookDBAccessor(getDatabase())
                .createOutlookTemplate(template);

        if (isSuccessful) {
            return redirect(controllers.routes.OutlookController.getUserTemplatesPage().url());
        } else {
            String reason = "There was an error creating your template. Please try again or submit a bug report";
            return getRedirectFailure(reason);
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getEditTemplatePage(String templateId) {
        Account account = Account.getAccountFromSession();

        CalendarTemplate template = new OutlookDBAccessor(getDatabase())
                .getUserCalendarTemplateById(account.getUserId(), templateId);

        if (template != null) {
            return ok(EditOutlookTemplatePage.render(template.getOauthAccountId(), template));
        } else {
            return internalServerError(FailurePage.render("Could not get account information"));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result editTemplate(String templateId) {
        Map<String, String[]> form = request().body().asFormUrlEncoded();

        DebugUtility.printFormContent(form);

        String outlookAccountId = Validation.string(CalendarAppointment.KEY_OUTLOOK_USER_ID, form);
        if (Validation.isEmpty(outlookAccountId)) {
            return badRequest(getNodeForMissingField(CalendarAppointment.KEY_OUTLOOK_USER_ID));
        }

        CalendarTemplate template;
        try {
            template = OutlookCalendarFactory.createTemplateFromForm(templateId, form);
        } catch (Exception e) {
            return getRedirectFailure(e.getMessage());
        }

        boolean isSuccessful = new OutlookDBAccessor(getDatabase())
                .editOutlookTemplate(outlookAccountId, template);

        if (isSuccessful) {
            flash(KEY_SUCCESS, "Your template has been edited");
            return redirect(controllers.routes.OutlookController.getUserTemplatesPage().url());
        } else {
            String reason = "There was an error creating your template. Please try again or submit a bug report";
            return getRedirectFailure(reason);
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUserTemplatesPage() {
        Account account = Account.getAccountFromSession();

        List<BaseCalendarTemplate> templates = new OutlookDBAccessor(getDatabase())
                .getUserCalendarTemplate(account.getUserId());

        List<OAuthAccount> accountList = new OAuthDBAccessor(getDatabase())
                .getAccounts(account.getUserId());

        if (accountList != null && accountList.isEmpty()) {
            return ok(UserCalendarTemplatesPage.render(null, null));
        } else if (templates != null) {
            String flashText = flash(KEY_SUCCESS);
            return ok(UserCalendarTemplatesPage.render(templates, flashText));
        } else {
            String reason = "There was an error retrieving your templates. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getPreviewCalendarTemplate(String templateId) {
        if (Validation.isEmpty(templateId)) {
            return badRequest(FailurePage.render("You were sent to this page by a bad link"));
        }

        Account account = Account.getAccountFromSession();
        CalendarTemplate template = new OutlookDBAccessor(getDatabase())
                .getUserCalendarTemplateById(account.getUserId(), templateId);
        if (template != null) {
            template.getDefaultEmails().clear();
            template.replaceVariablesWithAppointmentInfo(DialSheetAppointment.getDummy());
            return ok(PreviewCalendarTemplatePage.render(template.getOauthAccountId(), template));
        } else {
            String reason = "There was an error retrieving your template. Please try again or submit a bug report";
            return internalServerError(FailurePage.render(reason));
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public CompletionStage<Result> previewCalendarTemplate() {
        Account account = Account.getAccountFromSession();

        CalendarTemplate template;
        try {
            template = OutlookCalendarFactory.createTemplateFromForm(request().body().asFormUrlEncoded());
        } catch (Exception e) {
            return CompletableFuture.completedFuture(getRedirectFailure(e.getMessage()));
        }

        OAuthDBAccessor oAuthDBAccessor = new OAuthDBAccessor(getDatabase());

        OAuthToken authToken = oAuthDBAccessor.getAuthTokenForAccount(account.getUserId(), template.getOauthAccountId());
        if (authToken == null) {
            String reason = "There was an error communicating with Outlook. Please try again or submit a bug report.";
            Logger.error(reason, new IllegalStateException());
            return CompletableFuture.completedFuture(internalServerError(FailurePage.render(reason)));
        }

        if (authToken.isExpired()) {
            authToken = FutureUtility.getFromFutureQuietly(
                    outlookAuthClient.refreshAccessToken(template.getOauthAccountId(), authToken).toCompletableFuture()
            );
            if (authToken != null) {
                oAuthDBAccessor.saveAuthTokenForAccount(account.getUserId(), template.getOauthAccountId(), authToken);
            } else {
                String reason = "There was an error communicating with Outlook. Please try again or submit a bug report.";
                return CompletableFuture.completedFuture(internalServerError(FailurePage.render(reason)));
            }
        }
        CalendarAppointment calendarAppointment =
                OutlookCalendarFactory.createFromTemplate(template, account, DialSheetAppointment.getDummy());

        Logger.debug("Calendar Appointment: {}", calendarAppointment.toString(template.getOauthAccountId()));

        return outlookAuthClient.createOutlookEvent(authToken, template.getOauthAccountId(), calendarAppointment)
                .thenApply((calendarEventId) -> {
                    if (calendarEventId != null) {
                        return redirect(routes.OutlookController.getUserTemplatesPage());
                    } else {
                        String reason = "There was an error communicating with Outlook. Please try again or submit a bug report";
                        return getRedirectFailure(reason);
                    }
                });
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getUserTemplates() {
        Account account = Account.getAccountFromSession();

        List<BaseCalendarTemplate> templates = new OutlookDBAccessor(getDatabase())
                .getUserCalendarTemplate(account.getUserId());

        List<OAuthAccount> accountList = new OAuthDBAccessor(getDatabase())
                .getAccounts(account.getUserId());

        if (accountList != null && accountList.isEmpty()) {
            return status(409, ResultUtility.getNodeForBooleanResponse("You have no templates"));
        } else if (templates != null && !templates.isEmpty()) {
            return ok(new BaseCalendarTemplate.Converter().renderAsJsonArray(templates));
        } else if (templates != null) {
            return status(400, ResultUtility.getNodeForBooleanResponse("You have no templates"));
        } else {
            String reason = "There was an error retrieving your templates. Please try again or submit a bug report";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result deleteCalendarTemplate(String templateId) {
        Account account = Account.getAccountFromSession();

        boolean isSuccessful = new CalendarDBAccessor(getDatabase())
                .deleteTemplate(account.getUserId(), templateId);
        if (isSuccessful) {
            return ok(ResultUtility.getNodeForBooleanResponse(true));
        } else {
            String reason = "There was an error deleting this template";
            return internalServerError(ResultUtility.getNodeForBooleanResponse(reason));
        }
    }

}
