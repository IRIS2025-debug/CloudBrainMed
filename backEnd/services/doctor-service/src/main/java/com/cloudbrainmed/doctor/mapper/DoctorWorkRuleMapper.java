package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.DoctorWorkRule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DoctorWorkRuleMapper {

    @Select("""
        SELECT *
        FROM doctor_work_rule
        WHERE doctor_id = #{doctorId}
          AND status = 1
        ORDER BY day_of_week, start_time
        """)
    @Results(id = "workRuleResult", value = {
        @Result(column = "rule_id", property = "ruleId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "dept_id", property = "deptId"),
        @Result(column = "day_of_week", property = "dayOfWeek"),
        @Result(column = "start_time", property = "startTime"),
        @Result(column = "end_time", property = "endTime"),
        @Result(column = "max_patients", property = "maxPatients"),
        @Result(column = "preferred_level", property = "preferredLevel"),
        @Result(column = "valid_from", property = "validFrom"),
        @Result(column = "valid_to", property = "validTo"),
        @Result(column = "create_time", property = "createTime"),
        @Result(column = "update_time", property = "updateTime")
    })
    List<DoctorWorkRule> selectByDoctorId(
            @Param("doctorId") String doctorId);

    @Select("""
        SELECT *
        FROM doctor_work_rule
        WHERE rule_id = #{ruleId}
          AND doctor_id = #{doctorId}
          AND status = 1
        """)
    @org.apache.ibatis.annotations.ResultMap("workRuleResult")
    DoctorWorkRule selectOwned(
            @Param("ruleId") String ruleId,
            @Param("doctorId") String doctorId);

    @Select("""
        SELECT COUNT(*)
        FROM doctor_work_rule
        WHERE doctor_id = #{doctorId}
          AND day_of_week = #{dayOfWeek}
          AND status = 1
          AND rule_id <> COALESCE(#{ruleId}, '')
          AND valid_from <= #{validTo}
          AND valid_to >= #{validFrom}
          AND start_time < #{endTime}
          AND end_time > #{startTime}
        """)
    int countOverlap(DoctorWorkRule rule);

    @Insert("""
        INSERT INTO doctor_work_rule (
            rule_id, doctor_id, dept_id, day_of_week, start_time,
            end_time, max_patients, preferred_level, valid_from,
            valid_to, status, create_time
        ) VALUES (
            #{ruleId}, #{doctorId}, #{deptId}, #{dayOfWeek}, #{startTime},
            #{endTime}, #{maxPatients}, #{preferredLevel}, #{validFrom},
            #{validTo}, 1, #{createTime}
        )
        """)
    int insert(DoctorWorkRule rule);

    @Update("""
        UPDATE doctor_work_rule
        SET day_of_week = #{dayOfWeek},
            start_time = #{startTime},
            end_time = #{endTime},
            max_patients = #{maxPatients},
            preferred_level = #{preferredLevel},
            valid_from = #{validFrom},
            valid_to = #{validTo},
            update_time = #{updateTime}
        WHERE rule_id = #{ruleId}
          AND doctor_id = #{doctorId}
          AND status = 1
        """)
    int update(DoctorWorkRule rule);

    @Update("""
        UPDATE doctor_work_rule
        SET status = 0,
            update_time = CURRENT_TIMESTAMP
        WHERE rule_id = #{ruleId}
          AND doctor_id = #{doctorId}
          AND status = 1
        """)
    int disable(
            @Param("ruleId") String ruleId,
            @Param("doctorId") String doctorId);
}
