package com.cloudbrainmed.admin.dto;

import lombok.Data;

@Data
public class ScheduleConflictResult {

    public static final String NONE = "NONE";
    public static final String DOCTOR_TIME = "DOCTOR_TIME";
    public static final String ROOM_TIME = "ROOM_TIME";
    public static final String BATCH_DOCTOR_TIME = "BATCH_DOCTOR_TIME";
    public static final String BATCH_ROOM_TIME = "BATCH_ROOM_TIME";

    private boolean conflict;
    private String conflictType = NONE;
    private String conflictReason;
    private String conflictScheduleId;
    private String conflictDoctorId;
    private String conflictDoctorName;
    private String room;

    public static ScheduleConflictResult none() {
        ScheduleConflictResult result = new ScheduleConflictResult();
        result.setConflict(false);
        result.setConflictType(NONE);
        return result;
    }

    public static ScheduleConflictResult of(
            String conflictType,
            String conflictReason,
            String conflictScheduleId,
            String conflictDoctorId,
            String conflictDoctorName,
            String room) {
        ScheduleConflictResult result = new ScheduleConflictResult();
        result.setConflict(true);
        result.setConflictType(conflictType);
        result.setConflictReason(conflictReason);
        result.setConflictScheduleId(conflictScheduleId);
        result.setConflictDoctorId(conflictDoctorId);
        result.setConflictDoctorName(conflictDoctorName);
        result.setRoom(room);
        return result;
    }
}
