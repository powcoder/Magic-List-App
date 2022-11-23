https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.account;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Corey on 6/17/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class ManagerRequestNotification extends AccountNotification {

    public static final String KEY_EMPLOYEE_ID = "employee_id";
    public static final String KEY_MANAGER_ID = "manager_id";
    public static final String KEY_MANAGER_NAME = "manager_name";
    public static final String KEY_MANAGER_EMAIL = "manager_email";

    private final String employeeId;
    private final String managerId;
    private final String managerName;
    private final String managerEmail;

    public static ManagerRequestNotification getDefaultInstance(String notificationId, String employeeId,
                                                                String managerId) {
        return new ManagerRequestNotification(notificationId, NOTIFICATION_TYPE_EMPLOYEE_REQUEST, employeeId,
                -1, -1, false, false, employeeId, managerId, null,
                null);
    }

    public ManagerRequestNotification(String notificationId, String notificationType, String userId,
                                      long notificationDate, long expirationDate, boolean isFulfilled, boolean isSeen,
                                      String employeeId, String managerId, String managerName, String managerEmail) {
        super(notificationId, notificationType, userId, notificationDate, expirationDate, isFulfilled, isSeen);
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.managerName = managerName;
        this.managerEmail = managerEmail;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getManagerId() {
        return managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public static class Converter extends AccountNotification.Converter<ManagerRequestNotification> {

        @Override
        public ObjectNode renderAsJsonObject(ManagerRequestNotification object) {
            return super.renderAsJsonObject(object)
                    .put(KEY_EMPLOYEE_ID, escape(object.employeeId))
                    .put(KEY_MANAGER_ID, escape(object.managerId))
                    .put(KEY_MANAGER_NAME, escape(object.managerName))
                    .put(KEY_MANAGER_EMAIL, escape(object.managerEmail));
        }
    }


}
