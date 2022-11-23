https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.prospect.ProspectState;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static database.TablesContract._PersonStateType.*;

/**
 *
 */
public class ProspectStateDBAccessor extends DBAccessor {

    public ProspectStateDBAccessor(Database database) {
        super(database);
    }

    public List<ProspectState> getAllStates(String companyName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT * " +
                    "FROM person_state_type " +
                    "JOIN company_person_states ON state_type = person_state " +
                    "WHERE company_name = ? " +
                    "ORDER BY _index";
            statement = connection.prepareStatement(sql);
            statement.setString(1, companyName);
            resultSet = statement.executeQuery();

            return getStateListFromResultSet(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<ProspectState> getAllPagesDialSheetObjectionStates(String companyName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT *  " +
                    "FROM person_state_type " +
                    "JOIN company_person_states ON state_type = person_state " +
                    "WHERE company_name = ? AND is_all_pages_dial_sheet = TRUE " +
                    "ORDER BY _index";
            statement = connection.prepareStatement(sql);
            statement.setString(1, companyName);
            resultSet = statement.executeQuery();
            return getStateListFromResultSet(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public ProspectState getProspectStateFromKey(String prospectStateType, String companyName) {
        return getDatabase().withConnection(connection -> {
            String sql = "SELECT * " +
                    "FROM person_state_type " +
                    "JOIN company_person_states ON state_type = person_state " +
                    "WHERE company_name = ? AND state_type = ?" +
                    "ORDER BY _index";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, companyName);
            statement.setString(2, prospectStateType);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return getProspectStateFromResultSet(resultSet, "");
            } else {
                return null;
            }
        });
    }

    // Package-Private Methods

    /**
     * @param tableNamePrefix The table name to prepend to the column name. Must contain a period after it. IE "n_state."
     * @param isGroupBy       True if the projection is for a group by clause, false otherwise
     * @return A projection to be used in a SQL clause
     */
    static String getProjectionForProspectState(String tableNamePrefix, boolean isGroupBy) {
        tableNamePrefix = Optional.ofNullable(tableNamePrefix)
                .orElse("");

        String stateType;
        String stateName;
        String isDialSheetState;
        String isIndeterminate;
        String isObjection;
        String isIntroduction;
        String isInventory;
        String isMissedAppointment;
        String isParent;
        String isChild;
        String stateClass;
        String index;

        if (isGroupBy) {
            tableNamePrefix = tableNamePrefix.length() > 0 ? tableNamePrefix.substring(0, tableNamePrefix.length() - 1) : "";
            stateType = tableNamePrefix + STATE_TYPE;
            stateName = tableNamePrefix + STATE_NAME;
            isDialSheetState = tableNamePrefix + IS_DIAL_SHEET_STATE;
            isIndeterminate = tableNamePrefix + IS_INDETERMINATE;
            isObjection = tableNamePrefix + IS_OBJECTION;
            isIntroduction = tableNamePrefix + IS_INTRODUCTION;
            isInventory = tableNamePrefix + IS_INVENTORY;
            isMissedAppointment = tableNamePrefix + IS_MISSED_APPOINTMENT;
            isParent = tableNamePrefix + IS_PARENT;
            isChild = tableNamePrefix + IS_CHILD;
            stateClass = tableNamePrefix + STATE_CLASS;
            index = tableNamePrefix + _INDEX;
        } else {
            String tableNamePrefixNoPeriod = tableNamePrefix.length() > 0 ? tableNamePrefix.substring(0, tableNamePrefix.length() - 1) : "";

            stateType = tableNamePrefix + STATE_TYPE + " AS " + tableNamePrefixNoPeriod + STATE_TYPE;
            stateName = tableNamePrefix + STATE_NAME + " AS " + tableNamePrefixNoPeriod + STATE_NAME;
            isDialSheetState = tableNamePrefix + IS_DIAL_SHEET_STATE + " AS " + tableNamePrefixNoPeriod + IS_DIAL_SHEET_STATE;
            isIndeterminate = tableNamePrefix + IS_INDETERMINATE + " AS " + tableNamePrefixNoPeriod + IS_INDETERMINATE;
            isObjection = tableNamePrefix + IS_OBJECTION + " AS " + tableNamePrefixNoPeriod + IS_OBJECTION;
            isIntroduction = tableNamePrefix + IS_INTRODUCTION + " AS " + tableNamePrefixNoPeriod + IS_INTRODUCTION;
            isInventory = tableNamePrefix + IS_INVENTORY + " AS " + tableNamePrefixNoPeriod + IS_INVENTORY;
            isMissedAppointment = tableNamePrefix + IS_MISSED_APPOINTMENT + " AS " + tableNamePrefixNoPeriod + IS_MISSED_APPOINTMENT;
            isParent = tableNamePrefix + IS_PARENT + " AS " + tableNamePrefixNoPeriod + IS_PARENT;
            isChild = tableNamePrefix + IS_CHILD + " AS " + tableNamePrefixNoPeriod + IS_CHILD;
            stateClass = tableNamePrefix + STATE_CLASS + " AS " + tableNamePrefixNoPeriod + STATE_CLASS;
            index = tableNamePrefix + _INDEX + " AS " + tableNamePrefixNoPeriod + _INDEX;
        }

        return getFormattedColumns(stateType, stateName, isDialSheetState, isIndeterminate, isObjection, isIntroduction,
                isInventory, isMissedAppointment, isParent, isChild, stateClass, index);
    }

    static ProspectState getProspectStateFromResultSet(ResultSet resultSet, String tableNamePrefix) throws SQLException {
        tableNamePrefix = Optional.ofNullable(tableNamePrefix).orElse("");

        if (tableNamePrefix.length() > 0 && tableNamePrefix.endsWith(".")) {
            tableNamePrefix = tableNamePrefix.substring(0, tableNamePrefix.length() - 1);
        }

        String rawState = resultSet.getString(tableNamePrefix + STATE_TYPE);
        String stateDescription = resultSet.getString(tableNamePrefix + STATE_NAME);
        boolean isDialSheetState = resultSet.getBoolean(tableNamePrefix + IS_DIAL_SHEET_STATE);
        boolean isIndeterminate = resultSet.getBoolean(tableNamePrefix + IS_INDETERMINATE);
        boolean isObjection = resultSet.getBoolean(tableNamePrefix + IS_OBJECTION);
        boolean isIntroduction = resultSet.getBoolean(tableNamePrefix + IS_INTRODUCTION);
        boolean isInventory = resultSet.getBoolean(tableNamePrefix + IS_INVENTORY);
        boolean isMissedAppointment = resultSet.getBoolean(tableNamePrefix + IS_MISSED_APPOINTMENT);
        boolean isParent = resultSet.getBoolean(tableNamePrefix + IS_PARENT);
        boolean isChild = resultSet.getBoolean(tableNamePrefix + IS_CHILD);
        String stateClass = resultSet.getString(tableNamePrefix + STATE_CLASS);
        int index = resultSet.getInt(tableNamePrefix + _INDEX);
        return new ProspectState(rawState, stateDescription, isDialSheetState, isIndeterminate, isObjection,
                isIntroduction, isInventory, isMissedAppointment, isParent, isChild, stateClass, index);
    }

    // Private Methods

    private static List<ProspectState> getStateListFromResultSet(ResultSet resultSet) throws SQLException {
        List<ProspectState> stateList = new ArrayList<>();
        while (resultSet.next()) {
            stateList.add(getProspectStateFromResultSet(resultSet, null));
        }
        return stateList;
    }

}
