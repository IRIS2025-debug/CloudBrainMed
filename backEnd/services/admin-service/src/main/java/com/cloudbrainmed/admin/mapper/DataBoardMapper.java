package com.cloudbrainmed.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataBoardMapper {

    @Select("SELECT COUNT(*) FROM doctor WHERE is_deleted = 0")
    int countDoctors();

    @Select("SELECT COUNT(*) FROM department WHERE status = 1")
    int countDepartments();

    @Select("SELECT COUNT(*) FROM doctor_schedule " +
            "WHERE work_date = CURRENT_DATE AND status = 1 AND schedule_status = 'PUBLISHED'")
    int countTodaySchedules();

    @Select("SELECT COUNT(*) FROM medicine")
    int countMedicines();
}
