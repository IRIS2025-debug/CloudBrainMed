package com.cloudbrainmed.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.admin.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<DoctorSchedule> {

    @Select("SELECT * FROM doctor_schedule WHERE doctor_id = #{doctorId} " +
            "AND work_date BETWEEN #{startDate} AND #{endDate} " +
            "AND schedule_status = 'PUBLISHED' ORDER BY work_date, start_time")
    List<DoctorSchedule> selectByDoctorIdAndDateRange(@Param("doctorId") String doctorId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM doctor_schedule WHERE work_date BETWEEN #{startDate} AND #{endDate} " +
            "AND schedule_status = 'PUBLISHED' ORDER BY work_date, start_time")
    List<DoctorSchedule> selectByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM doctor_schedule WHERE dept_id = #{deptId} " +
            "AND work_date BETWEEN #{startDate} AND #{endDate} " +
            "AND schedule_status = 'PUBLISHED' ORDER BY work_date, start_time")
    List<DoctorSchedule> selectByDeptIdAndDateRange(@Param("deptId") String deptId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Update("UPDATE doctor_schedule SET status = #{status}, update_time = NOW() " +
            "WHERE schedule_id = #{scheduleId}")
    int updateStatus(@Param("scheduleId") String scheduleId, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM doctor_schedule WHERE doctor_id = #{doctorId} " +
            "AND work_date = #{workDate} " +
            "AND ((start_time < #{endTime} AND end_time > #{startTime})) " +
            "AND schedule_id != #{scheduleId} " +
            "AND schedule_status = 'PUBLISHED'")
    int checkConflict(@Param("doctorId") String doctorId,
                      @Param("workDate") LocalDate workDate,
                      @Param("startTime") LocalTime startTime,
                      @Param("endTime") LocalTime endTime,
                      @Param("scheduleId") String scheduleId);

    @Update("UPDATE doctor_schedule SET status = 1, update_time = NOW() " +
            "WHERE schedule_id = #{scheduleId}")
    int enableSchedule(@Param("scheduleId") String scheduleId);
}