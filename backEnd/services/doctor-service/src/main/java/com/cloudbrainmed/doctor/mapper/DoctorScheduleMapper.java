package com.cloudbrainmed.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.doctor.entity.DoctorSchedule;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
    /**
     * 查询医生某周的排班
     */
    @Select("SELECT schedule_id, plan_id, doctor_id, doctor_name, dept_id, " +
            "work_date, start_time, end_time, max_num, remain_num, " +
            "status, price, room, source_type, schedule_status, " +
            "create_time, update_time " +
            "FROM doctor_schedule " +
            "WHERE doctor_id = #{doctorId} " +
            "AND work_date BETWEEN #{weekStart} AND #{weekEnd} " +
            "AND status = 1 " +
            "AND schedule_status = 'PUBLISHED' " +
            "ORDER BY work_date ASC, start_time ASC")
    @Results(id = "doctorScheduleResult", value = {
            @Result(column = "schedule_id", property = "scheduleId"),
            @Result(column = "plan_id", property = "planId"),
            @Result(column = "doctor_id", property = "doctorId"),
            @Result(column = "doctor_name", property = "doctorName"),
            @Result(column = "dept_id", property = "deptId"),
            @Result(column = "work_date", property = "workDate"),
            @Result(column = "start_time", property = "startTime"),
            @Result(column = "end_time", property = "endTime"),
            @Result(column = "max_num", property = "maxNum"),
            @Result(column = "remain_num", property = "remainNum"),
            @Result(column = "status", property = "status"),
            @Result(column = "price", property = "price"),
            @Result(column = "room", property = "room"),
            @Result(column = "source_type", property = "sourceType"),
            @Result(column = "schedule_status", property = "scheduleStatus"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<DoctorSchedule> selectByDoctorIdAndWeek(@Param("doctorId") String doctorId,
                                                 @Param("weekStart") LocalDate weekStart,
                                                 @Param("weekEnd") LocalDate weekEnd);

    /**
     * 查询医生某天的排班
     */
    @Select("SELECT schedule_id, plan_id, doctor_id, doctor_name, dept_id, " +
            "work_date, start_time, end_time, max_num, remain_num, " +
            "status, price, room, source_type, schedule_status, " +
            "create_time, update_time " +
            "FROM doctor_schedule " +
            "WHERE doctor_id = #{doctorId} " +
            "AND work_date = #{workDate} " +
            "AND status = 1 " +
            "AND schedule_status = 'PUBLISHED' " +
            "ORDER BY start_time ASC")
    @ResultMap("doctorScheduleResult")
    List<DoctorSchedule> selectByDoctorIdAndDate(@Param("doctorId") String doctorId,
                                                 @Param("workDate") LocalDate workDate);

    /**
     * 批量查询指定医生当天有效排班
     */
    @Select({
            "<script>",
            "SELECT schedule_id, plan_id, doctor_id, doctor_name, dept_id, ",
            "work_date, start_time, end_time, max_num, remain_num, ",
            "status, price, room, source_type, schedule_status, ",
            "create_time, update_time ",
            "FROM doctor_schedule ",
            "WHERE doctor_id IN ",
            "<foreach collection='doctorIds' item='doctorId' open='(' separator=',' close=')'>",
            "#{doctorId}",
            "</foreach>",
            "AND work_date = #{workDate} ",
            "AND status = 1 ",
            "AND schedule_status = 'PUBLISHED' ",
            "ORDER BY doctor_id ASC, start_time ASC",
            "</script>"
    })
    @ResultMap("doctorScheduleResult")
    List<DoctorSchedule> selectPublishedByDoctorIdsAndDate(@Param("doctorIds") List<String> doctorIds,
                                                           @Param("workDate") LocalDate workDate);
}
