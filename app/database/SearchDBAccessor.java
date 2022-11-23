https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.*;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.lists.ProspectSearch;
import play.Logger;
import play.db.Database;
import utilities.StringBuilderExtension;
import utilities.Validation;

import java.sql.*;

import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._PersonState.*;
import static database.TablesContract._PersonStateType.*;
import static database.TablesContract._Searches.PERSON_ID;
import static database.TablesContract._Searches.SEARCHES;
import static database.TablesContract._Searches.SEARCH_ID;
import static database.TablesContract._SharesProspect.*;

public class SearchDBAccessor extends AbstractPersonDatabase {

    private static final Logger.ALogger logger = Logger.of(SearchDBAccessor.class);

    /**
     * @param database The database used by the Buzz to persist information.
     */
    public SearchDBAccessor(Database database) {
        super(database);
    }

    public PagedList<Prospect> getProspectsFromSearch(String userId, ProspectSearch search) {
        return getDatabase().withConnection(connection -> {
            String sql = buildQueryForProspectFromSearchPredicates(search);
            PreparedStatement statement = connection.prepareStatement(sql);

            int counter = 1;
            bindParametersToSearchQuery(statement, userId, search, counter);

            ResultSet resultSet = statement.executeQuery();
            return getPersonListFromResultSet(search.getCurrentPage(), resultSet);
        });
    }

    // Package-Private Methods

    /**
     * @param search The search object containing all of the predicates on which the prospects will be filtered
     * @return A SQL query that selects on getting a {@link Prospect}. The query contains the following predicates
     * (in order): list_id, user_id, user_id, all search predicates, all checked states, and a list_id. Note, some of
     * these fields are optional if they are not sent in the <b>search</b> object.
     */
    static String buildQueryForPersonIdFromSearchPredicates(ProspectSearch search) {
        StringBuilderExtension builder = new StringBuilderExtension()
                .append("INSERT INTO ").append(SEARCHES).append("(").append(SEARCH_ID).append(", ").append(PERSON_ID + ") ")
                .append("SELECT ").append("?, ").appendTableWithQualifier(PERSON_PROSPECT, PERSON_ID).append(" ");

        builder = buildSearchPredicatesForSearchQuery(search, builder, false);

        return builder.toString();
    }

    /**
     * Binds all necessary parameters to the related search query
     */
    static void bindParametersToSearchQuery(PreparedStatement statement, String userId, ProspectSearch search, int counter) throws SQLException {
        statement.setString(counter++, userId);
        statement.setString(counter++, userId);

        for (ProspectSearch.SearchPredicate predicate : search.getSearchPredicateList()) {
            statement.setString(counter++, "%" + predicate.getValue() + "%");
        }

        for (ProspectState checkedState : search.getCheckedStates()) {
            statement.setString(counter++, checkedState.getStateType());
        }

        if (!Validation.isEmpty(search.getListId())) {
            statement.setString(counter, search.getListId());
        }
    }

    // Private Methods

    /**
     * @param search The search object containing all of the predicates on which the prospects will be filtered
     * @return A SQL query that selects on getting a {@link Prospect}. The query contains the following predicates
     * (in order): user_id, user_id, all search predicates, all checked states, and a list_id. Note, some of these
     * fields are optional.
     */
    private static String buildQueryForProspectFromSearchPredicates(ProspectSearch search) {
        String personIdTablePrefix = PERSON_PROSPECT + ".";
        StringBuilderExtension builder = new StringBuilderExtension();
        builder.append("SELECT ").append(getSelectionForPerson(personIdTablePrefix, false)).append(", ")
                .append("COUNT(*) over() ").append(TOTAL_COUNT).append(" ");

        builder = buildSearchPredicatesForSearchQuery(search, builder, true);

        String orderByColumn = getTableNameFromPredicate(search.getSortByCriteria());
        int offset = (search.getCurrentPage() - 1) * AMOUNT_PER_PAGE;
        String sortOrder = search.isAscending() ? "ASC" : "DESC";

        builder.append("ORDER BY ").append(orderByColumn).append(" ").append(sortOrder).append(" ")
                .append("LIMIT ").append(AMOUNT_PER_PAGE).append(" OFFSET ").append(offset);

        return builder.toString();
    }

    private static StringBuilderExtension buildSearchPredicatesForSearchQuery(ProspectSearch search, StringBuilderExtension builder,
                                                                              boolean isUserSearch) {
        builder.append("FROM ").append(PERSON_PROSPECT).append(" ")

                .append("JOIN ").append(PERSON_STATE).append(" ON ")
                .appendTableWithQualifier(PERSON_PROSPECT, PERSON_ID)
                .append(" = ").appendTableWithQualifier(PERSON_STATE, PERSON_ID).append(" ")

                .append("JOIN ").append(PERSON_STATE_TYPE).append(" ON ")
                .append(STATE).append(" = ").append(STATE_TYPE).append(" ")

                .append("LEFT JOIN ").append(SHARES_PROSPECT).append(" ON ")
                .appendTableWithQualifier(PERSON_PROSPECT, PERSON_ID).append(" = ")
                .appendTableWithQualifier(SHARES_PROSPECT, PERSON_ID).append(" ");

        if (!Validation.isEmpty(search.getListId())) {
            builder.append("LEFT JOIN ").append(SEARCHES).append(" ON ")
                    .append(SEARCHES).append(".").append(PERSON_ID).append(" = ")
                    .append(PERSON_PROSPECT).append(".").append(PERSON_ID).append(" ");
        }

        builder.append("WHERE ").append(ProspectDBAccessor.getPredicateForProspectByUserId()).append(" ");

        final String matchingCriteria = search.isShouldFindAnyMatchingSearchPredicates() ? " OR " : " AND ";

        for (int i = 0; i < search.getSearchPredicateList().size(); i++) {
            if (i == 0) {
                // Put the AND inside the loop since we don't want to unnecessarily append an AND
                builder.append(" AND (");
            }

            ProspectSearch.Criteria criteria = search.getSearchPredicateList().get(i).getCriteria();
            String columnName = getTableNameFromPredicate(criteria);
            builder.append("LOWER(").append(columnName).append(") LIKE LOWER(?)");

            if (i < search.getSearchPredicateList().size() - 1) {
                // Append an OR if we're not at the end
                builder.append(matchingCriteria);
            } else {
                // Close off the parenthesis at the end
                builder.append(") ");
            }
        }

        if (search.getCheckedStates().size() > 0) {
            builder.append(" AND (");

            for (int i = 0; i < search.getCheckedStates().size(); i++) {
                builder.append(STATE).append(" = ?");

                if (i < search.getCheckedStates().size() - 1) {
                    builder.append(" OR ");
                }
            }

            builder.append(") ");
        }

        if (!Validation.isEmpty(search.getListId())) {
            builder.append("AND ").append(SEARCHES).append(".").append(SEARCH_ID).append(" = ? ");
        }

        String personIdTablePrefix = PERSON_PROSPECT + ".";

        if (isUserSearch) {
            builder.append("GROUP BY ").append(getSelectionForPerson(personIdTablePrefix, true)).append(" ");
        } else {
            builder.append("GROUP BY ").appendTableWithQualifier(PERSON_PROSPECT, PERSON_ID).append(" ");
        }

        return builder;
    }

    // Package Private Methods

    static String getTableNameFromPredicate(ProspectSearch.Criteria criteria) {
        if (criteria == ProspectSearch.Criteria.COMPANY_NAME) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonProspect.PERSON_COMPANY_NAME;
        } else if (criteria == ProspectSearch.Criteria.EMAIL) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonProspect.PERSON_EMAIL;
        } else if (criteria == ProspectSearch.Criteria.JOB_TITLE) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonProspect.JOB_TITLE;
        } else if (criteria == ProspectSearch.Criteria.PERSON_NAME) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonProspect.PERSON_NAME;
        } else if (criteria == ProspectSearch.Criteria.NOTES) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonState.NOTES;
        } else if (criteria == ProspectSearch.Criteria.PHONE) {
            return TablesContract._PersonProspect.PERSON_PROSPECT + "." + TablesContract._PersonProspect.PERSON_PHONE;
        } else {
            logger.error("Invalid criteria, found: {}", criteria);
            return null;
        }
    }

    private PagedList<Prospect> getPersonListFromResultSet(int currentPage, ResultSet resultSet) throws SQLException {
        PagedList<Prospect> personList = new PagedList<>(currentPage, AMOUNT_PER_PAGE);
        String personIdTablePrefix = PERSON_PROSPECT + ".";
        while (resultSet.next()) {
            setMaxPage(personList, resultSet);
            personList.add(getPersonFromResultSet(resultSet, personIdTablePrefix, null));
        }
        return personList;
    }

}
