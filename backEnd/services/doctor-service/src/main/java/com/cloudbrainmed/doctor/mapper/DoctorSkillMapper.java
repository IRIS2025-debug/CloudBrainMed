package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.DoctorSkill;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DoctorSkillMapper {

    @Select("""
        SELECT * FROM doctor_skill
        WHERE doctor_id = #{doctorId} AND status = 1
        """)
    @Results({
        @Result(column = "skill_id", property = "skillId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "priority", property = "priority"),
        @Result(column = "status", property = "status")
    })
    List<DoctorSkill> selectByDoctorId(@Param("doctorId") String doctorId);

    @Select("""
        SELECT ds.* FROM doctor_skill ds
        WHERE ds.item_code = #{itemCode} AND ds.status = 1
        """)
    @Results({
        @Result(column = "skill_id", property = "skillId"),
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "priority", property = "priority"),
        @Result(column = "status", property = "status")
    })
    List<DoctorSkill> selectByItemCode(@Param("itemCode") String itemCode);

    @Insert("""
        INSERT INTO doctor_skill (skill_id, doctor_id, item_code, priority, status)
        VALUES (#{skillId}, #{doctorId}, #{itemCode}, #{priority}, #{status})
        """)
    int insert(DoctorSkill skill);

    @Update("""
        UPDATE doctor_skill SET status = #{status}
        WHERE skill_id = #{skillId} AND doctor_id = #{doctorId}
        """)
    int updateStatus(@Param("skillId") String skillId, @Param("doctorId") String doctorId, @Param("status") Integer status);

    @Select("""
        SELECT COUNT(*) FROM doctor_skill
        WHERE doctor_id = #{doctorId} AND item_code = #{itemCode} AND status = 1
        """)
    int countByDoctorAndItem(@Param("doctorId") String doctorId, @Param("itemCode") String itemCode);

    @Select("""
        <script>
        SELECT DISTINCT d.doctor_id, d.name, d.doctor_type, d.department_id
        FROM doctor d
        JOIN doctor_skill ds ON d.doctor_id = ds.doctor_id
        WHERE ds.item_code = #{itemCode} AND ds.status = 1 AND d.status = 1 AND d.is_deleted = 0
        <if test="doctorType != null">
          AND d.doctor_type = #{doctorType}
        </if>
        </script>
        """)
    @Results({
        @Result(column = "doctor_id", property = "doctorId"),
        @Result(column = "name", property = "name"),
        @Result(column = "doctor_type", property = "doctorType"),
        @Result(column = "department_id", property = "departmentId")
    })
    List<DoctorSkillMatch> findMatchingDoctorsByItemCode(@Param("itemCode") String itemCode, @Param("doctorType") Integer doctorType);

    @Data
    class DoctorSkillMatch {
        private String doctorId;
        private String name;
        private Integer doctorType;
        private String departmentId;
    }
}