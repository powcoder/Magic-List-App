https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.manager;

import java.util.List;

public class Candidate {

    private final String candidateId;
    private final String name;
    private final String email;
    private final String phone;
    private final String notes;
    private final CandidateState state;
    private final List<String> fileIdList;

    public Candidate(String candidateId, String name, String email, String phone) {
        this(candidateId, name, email, phone, null, null, null);
    }

    public Candidate(String candidateId, String notes) {
        this(candidateId, null, null, null, notes, null, null);
    }

    public Candidate(String candidateId, String name, String email, String phone, String notes,
                     CandidateState state, List<String> fileIdList) {
        this.candidateId = candidateId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
        this.state = state;
        this.fileIdList = fileIdList;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getNotes() {
        return notes;
    }

    public CandidateState getState() {
        return state;
    }

    public List<String> getFileIdList() {
        return fileIdList;
    }
}
