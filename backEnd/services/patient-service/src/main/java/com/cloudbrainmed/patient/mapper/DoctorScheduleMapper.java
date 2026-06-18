package com.cloudbrainmed.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.patient.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
    @Select("SELECT * FROM doctor_schedule WHERE doctor_id = #{doctorId} AND status = 1 ORDER BY work_date, start_time")
    List<DoctorSchedule> selectByDoctorId(@Param("doctorId") String doctorId);

    @Select("SELECT * FROM doctor_schedule WHERE schedule_id = #{scheduleId} AND status = 1")
    DoctorSchedule selectByScheduleId(@Param("scheduleId") String scheduleId);

    @Update("UPDATE doctor_schedule SET remain_num = remain_num - 1 WHERE schedule_id = #{scheduleId} AND remain_num > 0")
    int decrementRemainNum(@Param("scheduleId") String scheduleId);
}