package org.oreo.smore.domain.studyroom;

public enum StudyRoomCategory {
    EMPLOYMENT("취업"),
    CERTIFICATION("자격증"),
    LANGUAGE("어학"),
    SELF_STUDY("자율"),
    MEETING("회의"),
    SCHOOL_STUDY("학교공부");

    private String value;

    StudyRoomCategory(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
