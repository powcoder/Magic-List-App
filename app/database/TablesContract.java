https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.lists.ProspectSearch;

/**
 * A final class used to define the relationship of our server with the database. This takes the form of defining all
 * of the tables and their respective columns. Note, the underscores are present for certain table names so their class
 * name doesn't conflict with their corresponding model class name (in the model package).
 */
final class TablesContract {

    private TablesContract() {
        // No instance
    }

    static class _BugReport {
        static final String BUG_REPORT = "user_suggestion";

        static final String BUG_ID = "bug_id";
        static final String BUG_TEXT = "text";
        static final String BUG_DATE = "bug_date";
    }

    static class _CalendarAppointmentLinks {
        static final String CALENDAR_APPOINTMENT_LINKS = "calendar_appointment_links";

        static final String PROVIDER_APPOINTMENT_ID = "provider_appointment_id";
        static final String APP_APPOINTMENT_ID = "app_appointment_id";
        static final String PROVIDER = "provider";
        static final String HYPER_LINK = "hyper_link";
        static final String CALENDAR_SUBJECT = "subject";
    }

    static class _ClientVersion {
        static final String CLIENT_REVISION = "client_revision";

        static final String VERSION = "version";
        static final String CHANGES = "changes";
        static final String OPERATING_SYSTEM = "operating_system";
    }

    static class _CompanyModel {
        static final String COMPANY_MODEL = "company_model";

        static final String COMPANY_NAME = "company_name";
        static final String DIVISION_NAME = "division_name";
        static final String REGISTRATION_CODE = "registration_code";
    }


    static class _DialSheet {
        static final String DIAL_SHEET = "dial_sheet";

        static final String SHEET_ID = "sheet_id";
        static final String NUMBER_OF_DIALS = "number_of_dials";
        static final String DIALS_COUNT = "dials_count";
        static final String ACTIVITY_COUNT = "activity_count";
        static final String CONTACTS_COUNT = "contacts_count";
        static final String APPOINTMENTS_COUNT = "appointments_count";
        static final String DIAL_DATE = "dial_date";
    }

    static class _DialSheetAppointment {
        static final String DIAL_SHEET_APPOINTMENT = "dial_sheet_appointment";

        static final String APPOINTMENT_ID = "appointment_id";
        static final String IS_CONFERENCE_CALL = "is_conference_call";
        static final String IS_FULFILLED = "is_fulfilled";
        static final String SHEET_ID = _DialSheet.SHEET_ID;
        static final String PERSON_ID = _PersonProspect.PERSON_ID;
        static final String APPOINTMENT_DATE = "appointment_date";
        static final String APPOINTMENT_NOTES = "appointment_notes";
        static final String APPOINTMENT_TYPE = "appointment_type";
        static final String APPOINTMENT_OUTCOME = "appointment_outcome";
        static final String LAST_CONTACTED = "last_contacted";
    }

    static class _DialSheetContact {
        static final String DIAL_SHEET_CONTACT = "dial_sheet_contact";

        static final String SHEET_ID = _DialSheet.SHEET_ID;
        static final String PERSON_ID = _PersonProspect.PERSON_ID;
        static final String CONTACT_STATUS = "contact_status";
        static final String CONTACT_TIME = "contact_time";
        static final String STATUS_COUNT = "status_count";
    }

    static class _EmployeeCandidate {
        static final String EMPLOYEE_CANDIDATE = "employee_candidate";

        static final String CANDIDATE_ID = "candidate_id";
        static final String CANDIDATE_NAME = "candidate_name";
        static final String CANDIDATE_EMAIL = "candidate_email";
        static final String CANDIDATE_PHONE = "candidate_phone";
        static final String CANDIDATE_NOTES = "candidate_notes";
        static final String CANDIDATE_STATUS = "candidate_status";
    }

    static class _EmployeeCandidateFile {
        static final String EMPLOYEE_CANDIDATE_FILE = "employee_candidate_file";

        static final String CANDIDATE_FILE_ID = "candidate_file_id";
    }

    static class _EmployeeCandidateStatusModel {
        static final String EMPLOYEE_CANDIDATE_STATUS_MODEL = "employee_candidate_status_model";

        static final String CANDIDATE_STATUS_TYPE = "candidate_status_type";
        static final String CANDIDATE_STATUS_NAME = "candidate_status_name";
    }

    static class _ImporterLimit {
        static final String IMPORTER_LIMIT = "importer_limit";

        static final String IMPORT_COUNT = "import_count";
        static final String IMPORT_DATE = "import_date";
    }

    static class _ImporterLimitModel {
        static final String IMPORTER_LIMIT_MODEL = "importer_limit_model";

        static final String DAILY_MAX = "daily_max";
        static final String MONTHLY_MAX = "monthly_max";
    }

    static class _Manages {
        static final String MANAGES = "manages";

        static final String MANAGER_ID = "manager_id";
        static final String EMPLOYEE_ID = "employee_id";
        static final String TEAM_JOIN_DATE = "team_join_date";
    }

    static class _ManagerRequestsEmployee {
        static final String MANAGER_REQUESTS_EMPLOYEE = "manager_requests_employee";

        static final String MANAGER_ID = _Manages.MANAGER_ID;
        static final String EMPLOYEE_ID = _Manages.EMPLOYEE_ID;
        static final String REQUEST_TOKEN = "request_token";
    }

    static class _Notification {
        static final String NOTIFICATION = "notification";

        static final String NOTIFICATION_ID = "notification_id";
        static final String IS_ARCHIVED = "is_archived";
        static final String MESSAGE = "message";
        static final String USER_ID = _User.USER_ID;
        static final String NOTIFICATION_DATE = "notification_date";
        static final String DATE_CREATED = "date_created";
        static final String NOTIFICATION_STATUS = "notification_status";
        static final String LAST_CONTACTED = "last_contacted";
    }

    static class _OAuthAccount {
        static final String OAUTH_ACCOUNT = "oauth_account";

        static final String OAUTH_ACCOUNT_ID = "oauth_account_id";
        static final String ACCESS_TOKEN = "access_token";
        static final String REFRESH_TOKEN = "refresh_token";
        static final String ACCESS_EXPIRATION_TIME = "access_expiration_time";
        static final String OAUTH_EMAIL = "oauth_email";
        static final String OAUTH_PROVIDER = "provider";
    }

    static class _OutlookCalendarTemplate {
        static final String OUTLOOK_CALENDAR_TEMPLATE = "outlook_calendar_template";

        static final String TEMPLATE_ID = "template_id";
        static final String TEMPLATE_NAME = "template_name";
        static final String DATE_CREATED = "date_created";
        static final String BODY_TEXT = "body_text";
        static final String CATEGORIES = "categories";
        static final String DEFAULT_ATTENDEES = "default_attendees";
        static final String DURATION_IN_MINUTES = "duration_in_minutes";
        static final String IMPORTANCE = "importance";
        static final String IS_REMINDER_ON = "is_reminder_on";
        static final String EVENT_LOCATION = "event_location";
        static final String REMINDER_MINUTES_BEFORE_START = "reminder_minutes_before_start";
        static final String IS_RESPONSE_REQUESTED = "is_response_requested";
        static final String SENSITIVITY = "sensitivity";
        static final String SHOW_AS_STATUS = "show_as_status";
        static final String SUBJECT = "subject";
    }

    static class _PersonProspect {
        static final String PERSON_PROSPECT = "person_prospect";

        static final String PERSON_ID = "person_id";
        static final String OWNER_ID = "owner_id";
        static final String PERSON_NAME = "person_name";
        static final String PERSON_COMPANY_NAME = "person_company_name";
        static final String JOB_TITLE = "job_title";
        static final String PERSON_EMAIL = "person_email";
        static final String PERSON_PHONE = "person_phone";
        static final String INSERT_DATE = "insert_date";
    }

    static class _PersonState {
        static final String PERSON_STATE = "person_state";

        static final String STATE = "state";
        static final String NOTES = "notes";
        static final String IS_PAID = "is_paid";
    }

    static class _PersonStateType {
        static final String PERSON_STATE_TYPE = "person_state_type";

        static final String STATE_TYPE = "state_type";
        static final String STATE_NAME = "state_name";
        static final String IS_DIAL_SHEET_STATE = "is_dial_sheet_state";
        static final String IS_OBJECTION = "is_objection";
        static final String IS_INDETERMINATE = "is_indeterminate";
        static final String IS_INTRODUCTION = "is_introduction";
        static final String IS_INVENTORY = "is_inventory";
        static final String IS_MISSED_APPOINTMENT = "is_missed_appointment";
        static final String IS_PARENT = "is_parent";
        static final String IS_CHILD = "is_child";
        static final String STATE_CLASS = "state_class";
        static final String _INDEX = "_index";
    }

    static class _Searches {
        static final String SEARCHES = "searches";

        static final String SEARCH_ID = _SearchResult.SEARCH_ID;
        static final String PERSON_ID = _PersonProspect.PERSON_ID;
    }

    static class _SearchResult {
        static final String SEARCH_RESULT = "search_result";

        static final String OWNER_ID = "owner_id";
        static final String SEARCH_ID = "search_id";
        static final String SEARCH_NAME = "search_name";
        static final String SEARCH_DATE = "search_date";
        static final String SEARCH_COMMENT = "search_comment";
    }

    static class _ServerVersionRevisions {
        static final String VERSION_NUMBER = "version_number";
        static final String VERSION_CHANGES = "version_changes";
        static final String VERSION_RELEASE_DATE = "version_release_date";
    }

    static class _UserViewsServerVersionRevisions {
        static final String IS_DISMISSED = "is_dismissed";
    }

    static class _SharesSearchResult {
        static final String SHARES_SEARCH_RESULT = "shares_search_result";

        static final String RECEIVER_ID = "receiver_id";
        static final String SHARER_ID = "sharer_id";
        static final String DATE_SHARED = "date_shared";
    }

    static class _SharesProspect {
        static final String SHARES_PROSPECT = "shares_prospect";

        static final String RECEIVER_ID = "receiver_id";
        static final String SHARER_ID = "sharer_id";
        static final String DATE_SHARED = "date_shared";
    }

    static class _StripePlan {
        static final String STRIPE_PLAN = "stripe_plan";

        static final String STRIPE_PLAN_ID = "stripe_plan_id";
        /**
         * Amount before any changes are mad (IE dividing by 100)
         */
        static final String PRICE_AMOUNT = "price_amount";
        static final String IS_SUBSCRIPTION = "is_subscription";
        /**
         * Number of times per year the person gets charged. IE a value of 12 means he/she gets charged monthly
         */
        static final String FREQUENCY = "frequency";
    }

    static class _StripeUserInfo {
        static final String STRIPE_USER_INFO = "stripe_user_info";

        static final String USER_ID = _User.USER_ID;
        static final String STRIPE_PLAN_ID = _StripePlan.STRIPE_PLAN_ID;
        static final String SUBSCRIPTION_ID = "stripe_subscription_id";
        static final String CUSTOMER_ID = "stripe_customer_id";
        static final String COUPON_ID = "coupon_id";
        static final String SUBSCRIPTION_STATUS = "subscription_status";
    }

    static class _User {
        static final String USER = "_user";

        static final String USER_ID = "user_id";
        static final String NAME = "name";
        static final String TOKEN = "token";
        static final String IMPORTER_TOKEN = "importer_token";
        static final String EMAIL = "email";
        static final String JOIN_DATE = "join_date";
        static final String PASSWORD = "password";
        static final String FORGOT_PASSWORD_LINK = "forgot_password_link";
        static final String IS_ADMIN = "is_admin";
        static final String IS_SUPER_ADMIN = "is_super_admin";
        static final String IS_BETA_TESTER = "is_beta_tester";
        static final String IS_MANAGER = "is_manager";
        static final String IS_EMAIL_VERIFIED = "is_email_verified";
        static final String VERIFY_EMAIL_LINK = "verify_email_link";
    }

    static class _UserMessageNotification {
        static final String USER_MESSAGE_NOTIFICATION = "user_message_notification";

        static final String SENDER_ID = "sender_id";
        static final String MESSAGE = "message";
    }

    static class _UserNotification {
        static final String USER_NOTIFICATION = "user_notification";

        static final String NOTIFICATION_ID = "notification_id";
        static final String NOTIFICATION_TYPE = "notification_type";
        static final String NOTIFICATION_DATE = "notification_date";
        static final String EXPIRATION_DATE = "expiration_date";
        static final String IS_FULFILLED = "is_fulfilled";
        static final String IS_SEEN = "is_seen";
    }

    static class _UserSettings {
        static final String SETTINGS_VALUE = "settings_value";

    }

    static class _UserSettingsModel {
        static final String MODEL_SETTINGS_TYPE = "model_settings_type";
        static final String SETTINGS_DEFAULT_VALUE = "settings_default_value";
    }

    static class _UserSuggestion {
        static final String USER_SUGGESTION = "user_suggestion";

        static final String SUGGESTION_ID = "suggestion_id";
        static final String SUGGESTION_TEXT = "text";
        static final String SUGGESTION_DATE = "suggestion_date";
    }

    static class _UserQuote {
        static final String USER_QUOTE = "user_quote";

        static final String QUOTE_ID = "quote_id";
        static final String QUOTE_TEXT = "text";
        static final String QUOTE_AUTHOR = "author";
        static final String QUOTE_DATE = "quote_date";
    }

}
