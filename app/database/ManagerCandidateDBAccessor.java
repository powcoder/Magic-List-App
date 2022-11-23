https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package database;

import model.manager.Candidate;
import model.manager.CandidateState;
import play.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static database.TablesContract._EmployeeCandidate.*;
import static database.TablesContract._EmployeeCandidateFile.*;
import static database.TablesContract._EmployeeCandidateStatusModel.*;

public class ManagerCandidateDBAccessor extends DBAccessor {

    public ManagerCandidateDBAccessor(Database database) {
        super(database);
    }

    public List<CandidateState> getAllCandidateStates(String companyName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT candidate_status_type, candidate_status_name " +
                    "FROM employee_candidate_status_model JOIN company_candidate_states " +
                    "ON employee_candidate_status_model.candidate_status_type = company_candidate_states.candidate_status " +
                    "WHERE company_name = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, companyName);
            resultSet = statement.executeQuery();

            List<CandidateState> candidateStateList = new ArrayList<>();
            while (resultSet.next()) {
                String statusType = resultSet.getString(CANDIDATE_STATUS_TYPE);
                String statusName = resultSet.getString(CANDIDATE_STATUS_NAME);
                candidateStateList.add(new CandidateState(statusType, statusName));
            }
            return candidateStateList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public List<Candidate> getAllEmployeeCandidates(String userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "SELECT employee_candidate.candidate_id AS candidate_id, candidate_name, candidate_email, " +
                    "candidate_phone, candidate_notes, candidate_status_type, candidate_status_name " +
                    "FROM employee_candidate " +
                    "LEFT JOIN employee_candidate_file " +
                    "ON employee_candidate.candidate_id = employee_candidate_file.candidate_id " +
                    "JOIN employee_candidate_status_model " +
                    "ON employee_candidate.candidate_status = employee_candidate_status_model.candidate_status_type " +
                    "WHERE manager_id = ?";
            statement = connection.prepareStatement(sql);
            statement.setString(1, userId);
            resultSet = statement.executeQuery();

            boolean shouldSkipCallToNext = false;
            List<Candidate> candidateList = new ArrayList<>();
            while (shouldSkipCallToNext || resultSet.next()) {
                String candidateId = resultSet.getString(CANDIDATE_ID);
                String name = resultSet.getString(CANDIDATE_NAME);
                String email = resultSet.getString(CANDIDATE_EMAIL);
                String phone = resultSet.getString(CANDIDATE_PHONE);
                String notes = resultSet.getString(CANDIDATE_NOTES);

                String statusType = resultSet.getString(CANDIDATE_STATUS_TYPE);
                String statusName = resultSet.getString(CANDIDATE_STATUS_NAME);
                CandidateState state = new CandidateState(statusType, statusName);
                String fileId = resultSet.getString(CANDIDATE_FILE_ID);
                List<String> fileIdList = new ArrayList<>();

                while (fileId != null && !shouldSkipCallToNext) {
                    fileIdList.add(fileId);

                    if (resultSet.next()) {
                        String currentCandidateId = resultSet.getString(CANDIDATE_ID);
                        fileId = resultSet.getString(CANDIDATE_FILE_ID);
                        if (!candidateId.equals(currentCandidateId)) {
                            shouldSkipCallToNext = true;
                            break;
                        }
                    }
                }

                candidateList.add(new Candidate(candidateId, name, email, phone, notes, state, fileIdList));
            }
            return candidateList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeConnections(connection, statement, resultSet);
        }
    }

    public boolean updateCandidate(String userId, Candidate candidate) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE employee_candidate SET candidate_name = ?, candidate_phone = ?, candidate_email = ? " +
                    "WHERE manager_id = ? AND candidate_id = ?;";
            statement = connection.prepareStatement(sql);
            setStringOrNull(statement, 1, candidate.getName());
            setStringOrNull(statement, 2, candidate.getPhone());
            setStringOrNull(statement, 3, candidate.getEmail());
            statement.setString(4, userId);
            statement.setString(5, candidate.getCandidateId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean updateCandidateNote(String userId, Candidate candidate) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE employee_candidate SET candidate_notes = ? " +
                    "WHERE manager_id = ? AND candidate_id = ?;";
            statement = connection.prepareStatement(sql);
            setStringOrNull(statement, 1, candidate.getNotes());
            statement.setString(2, userId);
            statement.setString(3, candidate.getCandidateId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

    public boolean updateCandidateStatus(String userId, String candidateId, String statusType, String companyName) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getDatabase().getConnection();
            String sql = "UPDATE employee_candidate SET candidate_status = ? " +
                    "WHERE manager_id = ? AND candidate_id = ? AND EXISTS (" +
                    "SELECT * FROM employee_candidate_status_model JOIN company_candidate_states " +
                    "ON candidate_status_type = candidate_state WHERE candidate_status_type = ? AND company_name = ?" +
                    ");";
            statement = connection.prepareStatement(sql);
            statement.setString(1, statusType);
            statement.setString(2, userId);
            statement.setString(3, candidateId);
            statement.setString(4, statusType);
            statement.setString(5, companyName);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnections(connection, statement);
        }
    }

}
