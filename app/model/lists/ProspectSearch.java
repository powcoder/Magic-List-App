https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.lists;

import com.fasterxml.jackson.databind.JsonNode;
import model.prospect.ProspectState;
import model.serialization.MagicListObject;
import play.Logger;
import utilities.Validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static controllers.BaseController.KEY_ASCENDING;
import static controllers.BaseController.KEY_PAGE;
import static controllers.BaseController.KEY_SORT_BY;

public class ProspectSearch {

    public static class Factory {

        /**
         * @param object            The {@link Map} (of types String, String; String, String[]; String, List[String];
         *                          OR {@link JsonNode} from which the key's value should be extracted.
         * @param allProspectStates All of the prospect states for the given account's company name
         * @return A {@link ProspectSearch} or null if a server error occurred.
         */
        public static <T> ProspectSearch createFromRequest(T object, List<ProspectState> allProspectStates) {
            String name = Validation.string(SearchPredicate.KEY_NAME, object);
            String phone = Validation.string(SearchPredicate.KEY_PHONE, object);
            String email = Validation.string(SearchPredicate.KEY_EMAIL, object);
            String companyName = Validation.string(SearchPredicate.KEY_COMPANY_NAME, object);
            String jobTitle = Validation.string(SearchPredicate.KEY_JOB_TITLE, object);
            String notes = Validation.string(SearchPredicate.KEY_NOTES, object);

            SearchPredicate namePredicate = new SearchPredicate(name, ProspectSearch.Criteria.PERSON_NAME);
            SearchPredicate phonePredicate = new SearchPredicate(phone, ProspectSearch.Criteria.PHONE);
            SearchPredicate emailPredicate = new SearchPredicate(email, ProspectSearch.Criteria.EMAIL);
            SearchPredicate companyNamePredicate = new SearchPredicate(companyName, ProspectSearch.Criteria.COMPANY_NAME);
            SearchPredicate jobTitlePredicate = new SearchPredicate(jobTitle, ProspectSearch.Criteria.JOB_TITLE);
            SearchPredicate notesPredicate = new SearchPredicate(notes, ProspectSearch.Criteria.NOTES);

            List<String> selectedStates = Validation.array(SearchPredicate.KEY_SELECTED_STATES, object);
            if (selectedStates == null) {
                logger.debug("Selected states was null... proceeding to initialize empty array...");
            }

            List<ProspectState> checkedStates;
            if (selectedStates != null) {
                checkedStates = allProspectStates.stream()
                        .filter(state -> selectedStates.stream()
                                .anyMatch(selectedState -> selectedState.equals(state.getStateType())))
                        .collect(Collectors.toList());
            } else {
                checkedStates = new ArrayList<>();
            }

            if (checkedStates.isEmpty()) {
                checkedStates.addAll(allProspectStates);
            }

            ProspectSearch.Criteria sortByCriteria = Criteria.parse(Validation.string(KEY_SORT_BY, object));

            String listId = Validation.string(SearchPredicate.KEY_LIST_ID, object);
            if (Validation.isEmpty(listId)) {
                listId = null;
            }

            boolean shouldMatchAnyCriteria = Validation.bool(SearchPredicate.KEY_SHOULD_MATCH_ANY, object, false);
            boolean isAscending = Validation.bool(KEY_ASCENDING, object, true);
            int page = Validation.page(KEY_PAGE, object);

            return new ProspectSearch(namePredicate, phonePredicate, jobTitlePredicate, companyNamePredicate, emailPredicate,
                    notesPredicate, listId, allProspectStates, checkedStates, sortByCriteria, shouldMatchAnyCriteria,
                    isAscending, page);
        }

    }

    private static final Logger.ALogger logger = Logger.of(ProspectSearch.class);

    private final SearchPredicate namePredicate;
    private final SearchPredicate phonePredicate;
    private final SearchPredicate jobTitlePredicate;
    private final SearchPredicate companyNamePredicate;
    private final SearchPredicate emailPredicate;
    private final SearchPredicate notesPredicate;
    private final String listId;
    private List<ProspectState> allStates;
    private List<ProspectState> checkedStates;
    private Criteria sortByCriteria;
    private boolean shouldFindAnyMatchingSearchPredicates;
    private boolean isAscending = true;
    private final int currentPage;

    private final List<SearchPredicate> searchPredicateList;

    private ProspectSearch(SearchPredicate namePredicate, SearchPredicate phonePredicate,
                           SearchPredicate jobTitlePredicate, SearchPredicate companyNamePredicate,
                           SearchPredicate emailPredicate, SearchPredicate notesPredicate, String listId,
                           List<ProspectState> allStates, List<ProspectState> checkedStates, Criteria sortByCriteria,
                           boolean shouldFindAnyMatchingSearchPredicates, boolean isAscending, int currentPage) {
        this.namePredicate = namePredicate;
        this.phonePredicate = phonePredicate;
        this.jobTitlePredicate = jobTitlePredicate;
        this.companyNamePredicate = companyNamePredicate;
        this.emailPredicate = emailPredicate;
        this.notesPredicate = notesPredicate;
        this.listId = listId;
        this.allStates = allStates;
        this.checkedStates = checkedStates;
        this.sortByCriteria = sortByCriteria;
        this.isAscending = isAscending;
        this.shouldFindAnyMatchingSearchPredicates = shouldFindAnyMatchingSearchPredicates;
        this.currentPage = currentPage;
        this.searchPredicateList = new ArrayList<>();

        if (!Validation.isEmpty(namePredicate.getValue())) {
            searchPredicateList.add(namePredicate);
        }
        if (!Validation.isEmpty(phonePredicate.getValue())) {
            searchPredicateList.add(phonePredicate);
        }
        if (!Validation.isEmpty(companyNamePredicate.getValue())) {
            searchPredicateList.add(companyNamePredicate);
        }
        if (!Validation.isEmpty(jobTitlePredicate.getValue())) {
            searchPredicateList.add(jobTitlePredicate);
        }
        if (!Validation.isEmpty(emailPredicate.getValue())) {
            searchPredicateList.add(emailPredicate);
        }
        if (!Validation.isEmpty(notesPredicate.getValue())) {
            searchPredicateList.add(notesPredicate);
        }
    }

    public SearchPredicate getNamePredicate() {
        return namePredicate;
    }

    public SearchPredicate getPhonePredicate() {
        return phonePredicate;
    }

    public SearchPredicate getJobTitlePredicate() {
        return jobTitlePredicate;
    }

    public SearchPredicate getCompanyNamePredicate() {
        return companyNamePredicate;
    }

    public SearchPredicate getEmailPredicate() {
        return emailPredicate;
    }

    public SearchPredicate getNotesPredicate() {
        return notesPredicate;
    }

    public String getListId() {
        return listId;
    }

    public List<ProspectState> getCheckedStates() {
        return checkedStates;
    }

    public Criteria getSortByCriteria() {
        return sortByCriteria;
    }

    public boolean isShouldFindAnyMatchingSearchPredicates() {
        return shouldFindAnyMatchingSearchPredicates;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean isAscending() {
        return isAscending;
    }

    public List<SearchPredicate> getSearchPredicateList() {
        return searchPredicateList;
    }

    public List<StatusFilter> getContactStatusFilters() {
        List<StatusFilter> statusFilters = new ArrayList<>();

        for (ProspectState parent : allStates) {
            boolean isChecked = false;
            for (ProspectState checkedState : checkedStates) {
                if (parent == checkedState) {
                    isChecked = true;
                    break;
                }
            }
            statusFilters.add(new StatusFilter(parent, isChecked));
        }

        logger.trace("Status Filters: {}", MagicListObject.prettyPrint(statusFilters));

        return statusFilters;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(namePredicate.criteria.getRawText()).append(namePredicate.getValue())
                .append(phonePredicate.criteria.getRawText()).append(phonePredicate.getValue())
                .append(jobTitlePredicate.criteria.getRawText()).append(jobTitlePredicate.getValue())
                .append(companyNamePredicate.criteria.getRawText()).append(companyNamePredicate.getValue())
                .append(emailPredicate.criteria.getRawText()).append(emailPredicate.getValue())
                .append(notesPredicate.criteria.getRawText()).append(notesPredicate.getValue())
                .append(listId);
        for (ProspectState checkedState : checkedStates) {
            builder.append(checkedState.getStateType());
        }
        builder.append(isAscending);
        builder.append(currentPage);
        return builder.toString();
    }

    public static class StatusFilter extends MagicListObject {

        private final ProspectState state;
        private final boolean isChecked;
        private final List<StatusFilter> children = new ArrayList<>();

        StatusFilter(ProspectState state, boolean isChecked) {
            this.state = state;
            this.isChecked = isChecked;
        }

        public ProspectState getState() {
            return state;
        }

        public boolean isChecked() {
            return isChecked;
        }

        public List<StatusFilter> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return "{" + state.toString() + "; " + isChecked + "}";
        }

        static void convertStatusFiltersToParentLists(List<StatusFilter> statusFilters) {
            for (int i = 0; i < statusFilters.size(); i++) {
                StatusFilter parent = statusFilters.get(i);
                if (parent.getState().isParent()) {
                    i += 1; // move i one forward
                    while (i < statusFilters.size() && statusFilters.get(i).getState().isChild()) {
                        parent.children.add(statusFilters.remove(i));
                    }
                    i -= 1; // move i one backward in anticipation of the for loop moving it forward
                }
            }
        }
    }

    public static class SearchPredicate {

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_NAME = "person_name";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_PHONE = "person_phone";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_EMAIL = "person_email";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_COMPANY_NAME = "person_company_name";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_JOB_TITLE = "person_job_title";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL OR)
         */
        public static final String KEY_NOTES = "person_notes";

        /**
         * Takes a non-null and non-empty string value to filter by it (SQL AND)
         */
        public static final String KEY_SELECTED_STATES = "selected_states";

        /**
         * The ID of the user's search to sort by
         */
        public static final String KEY_LIST_ID = SavedList.KEY_LIST_ID;

        /**
         * Takes a boolean value which causes searches to match ANY criteria in the search criteria or false for ALL
         */
        public static final String KEY_SHOULD_MATCH_ANY = "should_match_any_criteria";

        private final String value;
        private final Criteria criteria;

        public SearchPredicate(String value, Criteria criteria) {
            this.value = value;
            this.criteria = criteria;
        }

        public String getValue() {
            return value;
        }

        public Criteria getCriteria() {
            return criteria;
        }
    }

    public enum Criteria {

        PERSON_NAME, EMAIL, COMPANY_NAME, JOB_TITLE, NOTES, PHONE;

        /**
         * @param rawPredicate The raw predicate from the request.
         * @return The matching order by criteria, or {@link #PERSON_NAME} if none match or the rawPredicate is null
         */
        public static Criteria parse(String rawPredicate) {
            return Arrays.stream(Criteria.values())
                    .filter(criteria -> criteria.getRawText().equalsIgnoreCase(rawPredicate))
                    .findFirst()
                    .orElse(Criteria.PERSON_NAME);
        }

        public String getRawText() {
            return this.toString().toLowerCase();
        }

        public String getUiText() {
            String s = this.toString();
            return s.substring(0, 1) + s.substring(1, s.length()).toLowerCase();
        }

    }

}
