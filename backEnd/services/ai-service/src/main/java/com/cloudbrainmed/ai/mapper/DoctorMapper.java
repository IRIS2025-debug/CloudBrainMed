package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DoctorMapper {

    /**
     * 查询所有启用且未删除的医生
     */
    @Select("SELECT doctor_id, avatar, name, gender, phone, email, position, " +
            "good_at, introduction, password, department_id, status, create_time, is_deleted " +
            "FROM doctor " +
            "WHERE status = 1 AND (is_deleted = 0 OR is_deleted IS NULL)")
    List<Doctor> selectActiveDoctors();

    /**
     * 根据ID查询医生
     */
    @Select("SELECT doctor_id, avatar, name, gender, phone, email, position, " +
            "good_at, introduction, password, department_id, status, create_time, is_deleted " +
            "FROM doctor WHERE doctor_id = #{doctorId}")
    Doctor selectByDoctorId(@Param("doctorId") String doctorId);

    /**
     * 根据科室ID查询医生列表
     */
    @Select("SELECT doctor_id, avatar, name, gender, phone, email, position, " +
            "good_at, introduction, password, department_id, status, create_time, is_deleted " +
            "FROM doctor " +
            "WHERE department_id = #{departmentId} AND status = 1 AND (is_deleted = 0 OR is_deleted IS NULL)")
    List<Doctor> selectByDepartmentId(@Param("departmentId") String departmentId);
}