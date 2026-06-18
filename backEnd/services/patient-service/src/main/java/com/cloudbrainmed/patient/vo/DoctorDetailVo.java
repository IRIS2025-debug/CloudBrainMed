// com.cloudbrainmed.patient.vo.DoctorDetailVo.java
package com.cloudbrainmed.patient.vo;

import lombok.Data;
import java.util.List;

@Data
public class DoctorDetailVo {
    private String doctorId;
    private String name;
    private String position;
    private String goodAt;
    private String introduction;
    private String avatar;
    private String deptName;
    private List<ScheduleVo> schedules;
}