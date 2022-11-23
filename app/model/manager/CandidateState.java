https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.manager;

import java.io.Serializable;

public class CandidateState implements Serializable {

    private static final long serialVersionUID = -1230123123432L;

    private static final String KEY_INFORMATION_RECEIVED = "information_received";
    private static final String KEY_INFORMATION_RECEIVED_DESCRIPTION = "Info. Received";

    public static final CandidateState INFORMATION_RECEIVED =
            new CandidateState(KEY_INFORMATION_RECEIVED, KEY_INFORMATION_RECEIVED_DESCRIPTION);

    private final String statusType;
    private final String statusDescription;

    public CandidateState(String statusType, String statusDescription) {
        this.statusType = statusType;
        this.statusDescription = statusDescription;
    }

    public String getStatusType() {
        return statusType;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    @Override
    public String toString() {
        return statusDescription;
    }

}
