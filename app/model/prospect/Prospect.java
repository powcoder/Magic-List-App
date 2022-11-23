https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.prospect;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;
import model.serialization.MagicListObject;
import play.libs.Json;
import utilities.RandomStringGenerator;
import utilities.StringUtility;
import utilities.Validation;

import java.io.Serializable;

public class Prospect extends MagicListObject implements Serializable {

    private static final long serialVersionUID = -5276805334850936092L;

    public static final String KEY_ID = "person_id";
    public static final String KEY_PERSON_NAME = "person_name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_JOB_TITLE = "job_title";
    public static final String KEY_COMPANY_NAME = "company_name";
    public static final String KEY_STATE = "state";

    @SerializedName(KEY_ID)
    private final String id;
    @SerializedName(value = KEY_PERSON_NAME)
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final String jobTitle;
    private final String companyName;

    private final ProspectState state;
    private final String notes;
    private final Long dateCreated;

    public static class Factory {

        public static Prospect createDummy(String id) {
            return new Prospect(id, "John Smith", "john.smith@example.com", "9083072323",
                    "CEO", "Caplan Innovations");
        }

        public static Prospect createFromId(String id) {
            return new Prospect(id);
        }

        /**
         * @return A prospect after sanitizing the input from the user
         */
        public static Prospect createFromRawInput(String id, String name, String email, String phoneNumber,
                                                  String jobTitle, String companyName, String notes) {
            if (!Validation.isEmpty(email)) {
                if (email.trim().matches(StringUtility.EMAIL_REGEX)) {
                    email = email.trim();
                } else {
                    email = null;
                }
            } else {
                email = null;
            }

            if (Validation.isEmpty(phoneNumber)) {
                phoneNumber = null;
            }

            if (Validation.isEmpty(jobTitle)) {
                jobTitle = null;
            }

            if (Validation.isEmpty(companyName)) {
                companyName = null;
            }

            if (Validation.isEmpty(notes)) {
                notes = null;
            }
            return new Prospect(id, name, email, phoneNumber, jobTitle, companyName, notes);
        }

        public static Prospect createFromDatabase(String id, String name, String email, String phoneNumber,
                                                  String jobTitle, String companyName, ProspectState state,
                                                  String notes, Long dateCreated) {
            return new Prospect(id, name, email, phoneNumber, jobTitle, companyName, state, notes, dateCreated);
        }

    }

    private Prospect(String id) {
        this(id, null, null, null, null, null,
                ProspectState.NOT_CONTACTED, null, -1L);
    }

    private Prospect(String id, String name, String email, String phoneNumber, String jobTitle, String companyName,
                     String notes) {
        this(id, name, email, phoneNumber, jobTitle, companyName, ProspectState.NOT_CONTACTED, notes, -1L);
    }

    private Prospect(String id, String name, String email, String phoneNumber, String jobTitle, String companyName) {
        this(id, name, email, phoneNumber, jobTitle, companyName, ProspectState.NOT_CONTACTED, null,
                -1L);
    }

    protected Prospect(String id, String name, String email, String phoneNumber, String jobTitle, String companyName,
                     ProspectState state, String notes, Long dateCreated) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.state = state;
        this.notes = notes;
        this.dateCreated = dateCreated;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public ProspectState getState() {
        return state;
    }

    public String getNotes() {
        return notes;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    @Override
    public String toString() {
        return MagicListObject.prettyPrint(this);
    }

}
