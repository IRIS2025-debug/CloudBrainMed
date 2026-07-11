package com.cloudbrainmed.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataBoardMapper {

    @Select("SELECT COUNT(*) FROM doctor_schedule " +
            "WHERE work_date = CURRENT_DATE AND status = 1 AND schedule_status = 'PUBLISHED'")
    int countTodaySchedules();
}
