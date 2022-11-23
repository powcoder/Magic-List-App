https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.manager;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.account.Account;
import model.JsonConverter;
import play.libs.Json;

import java.util.Calendar;

/**
 * Created by Corey on 6/8/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class Employee {

    private static final String KEY_EMPLOYEE_ID = "employee_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TEAM_JOIN_DATE = "team_join_date";
    private static final String KEY_MANAGER_ID = "manager_id";
    private static final String KEY_IS_CURRENT_EMPLOYEE = "is_current_employee";

    private final String employeeId;
    private final String name;
    private final String email;
    private final long teamJoinDate;
    private final long magicListJoinDate;
    private final String managerId;

    public Employee(String employeeId, String name, String email, long teamJoinDate, long magicListJoinDate,
                    String managerId) {
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.teamJoinDate = teamJoinDate;
        this.magicListJoinDate = magicListJoinDate;
        this.managerId = managerId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public long getTeamJoinDate() {
        return teamJoinDate;
    }

    public boolean isMagicListMemberYet() {
        return magicListJoinDate <= Calendar.getInstance().getTimeInMillis();
    }

    public String getManagerId() {
        return managerId;
    }

    public static class Converter implements JsonConverter<Employee> {

        private final String currentEmployeeId = Account.getAccountFromSession().getUserId();

        @Override
        public ObjectNode renderAsJsonObject(Employee employee) {
            return Json.newObject()
                    .put(KEY_EMPLOYEE_ID, escape(employee.employeeId))
                    .put(KEY_NAME, escape(employee.name))
                    .put(KEY_EMAIL, escape(employee.email))
                    .put(KEY_TEAM_JOIN_DATE, employee.teamJoinDate)
                    .put(KEY_MANAGER_ID, escape(employee.managerId))
                    .put(KEY_IS_CURRENT_EMPLOYEE, employee.employeeId.equalsIgnoreCase(currentEmployeeId));
        }

        @Override
        public Employee deserializeFromJson(ObjectNode objectNode) {
            throw new RuntimeException("Invalid constructor");
        }

    }

}
