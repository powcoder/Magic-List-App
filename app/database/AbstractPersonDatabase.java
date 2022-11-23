https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.prospect.Prospect;
import play.Logger;
import play.db.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static database.TablesContract._PersonState.NOTES;
import static database.TablesContract._PersonProspect.*;
import static database.TablesContract._PersonProspect.PERSON_COMPANY_NAME;
import static database.TablesContract._PersonProspect.JOB_TITLE;

/**
 *
 */
abstract class AbstractPersonDatabase extends DBAccessor {

    static final int AMOUNT_PER_PAGE = 25;

    @SuppressWarnings("unused")
    private final Logger.ALogger logger = Logger.of(this.getClass());

    AbstractPersonDatabase(Database database) {
        super(database);
    }

    static String getSelectionForPerson(String personIdTablePrefix, boolean isGroupBy) {
        return getSelectionForPerson(personIdTablePrefix, null, isGroupBy);
    }

    /**
     * @param personIdTablePrefix    The table name to prepend to the column name. Must contain a period after it. IE
     *                               "person_prospect."
     * @param personStateTablePrefix The table name to prepend to the column name. Must contain a period after it. IE
     *                               "n_state."
     * @param isGroupBy              True if the projection is for a group by clause, false otherwise
     * @return A projection to be used in a SQL clause
     */
    static String getSelectionForPerson(String personIdTablePrefix, String personStateTablePrefix, boolean isGroupBy) {
        personIdTablePrefix = Optional.ofNullable(personIdTablePrefix).orElse("");
        personStateTablePrefix = Optional.ofNullable(personStateTablePrefix).orElse("");

        String personStateColumns = ProspectStateDBAccessor.getProjectionForProspectState(personStateTablePrefix, isGroupBy);

        String personIdTablePrefixNoPeriod = personIdTablePrefix.length() > 0 ?
                personIdTablePrefix.substring(0, personIdTablePrefix.length() - 1)
                : "";
        String personId;
        if (isGroupBy) {
            personId = personIdTablePrefixNoPeriod + PERSON_ID;
        } else {
            personId = personIdTablePrefix + PERSON_ID + " AS " + personIdTablePrefixNoPeriod + PERSON_ID;
        }

        return getFormattedColumns(personId, PERSON_NAME, PERSON_EMAIL, PERSON_PHONE,
                JOB_TITLE, PERSON_COMPANY_NAME, NOTES, INSERT_DATE, personStateColumns);
    }

    /**
     * Assumes that "next" was already called
     */
    static Prospect getPersonFromResultSet(ResultSet resultSet) throws SQLException {
        return getPersonFromResultSet(resultSet, null, null);
    }

    /**
     * Assumes that "next" was already called
     * @param personIdTablePrefix The table that will be used to reference the person ID column
     */
    static Prospect getPersonFromResultSet(ResultSet resultSet, String personIdTablePrefix,
                                           String stateTablePrefix) throws SQLException {
        stateTablePrefix = Optional.ofNullable(stateTablePrefix).orElse("");
        personIdTablePrefix = Optional.ofNullable(personIdTablePrefix).orElse("");

        if(personIdTablePrefix.endsWith(".")) {
            personIdTablePrefix = personIdTablePrefix.substring(0, personIdTablePrefix.length() - 1);
        }

        String id = resultSet.getString(personIdTablePrefix + PERSON_ID);
        String name = resultSet.getString(PERSON_NAME);
        String email = resultSet.getString(PERSON_EMAIL);
        String phoneNumber = resultSet.getString(PERSON_PHONE);
        String jobTitle = resultSet.getString(JOB_TITLE);
        String companyName = resultSet.getString(PERSON_COMPANY_NAME);

        String notes = resultSet.getString(NOTES);
        long dateCreated = resultSet.getDate(INSERT_DATE).getTime();

        return Prospect.Factory.createFromDatabase(id, name, email, phoneNumber, jobTitle, companyName,
                ProspectStateDBAccessor.getProspectStateFromResultSet(resultSet, stateTablePrefix), notes, dateCreated);
    }

}
