package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.entity.MedicalItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicalItemMapper {

    @Select("""
        SELECT item_id, item_code, item_name, item_category, dept_id,
               default_urgency, estimated_duration_min, price, status
        FROM medical_item
        WHERE status = 1
        ORDER BY item_category, item_code
        """)
    @Results(id = "medicalItemResult", value = {
        @Result(column = "item_id", property = "itemId"),
        @Result(column = "item_code", property = "itemCode"),
        @Result(column = "item_name", property = "itemName"),
        @Result(column = "item_category", property = "itemCategory"),
        @Result(column = "dept_id", property = "deptId"),
        @Result(column = "default_urgency", property = "defaultUrgency"),
        @Result(column = "estimated_duration_min",
                property = "estimatedDurationMin")
    })
    List<MedicalItem> selectEnabled();

    @Select("""
        SELECT item_id, item_code, item_name, item_category, dept_id,
               default_urgency, estimated_duration_min, price, status
        FROM medical_item
        WHERE item_code = #{itemCode}
          AND status = 1
        """)
    @org.apache.ibatis.annotations.ResultMap("medicalItemResult")
    MedicalItem selectEnabledByCode(@Param("itemCode") String itemCode);
}
