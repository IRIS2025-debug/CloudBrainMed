package com.cloudbrainmed.doctor.mapper;

import com.cloudbrainmed.doctor.dto.MedicineOptionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MedicineOptionMapper {

    @Select("""
            <script>
            SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time
            FROM medicine
            <where>
                <if test="keyword != null and keyword != ''">
                    name LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            ORDER BY create_time DESC
            </script>
            """)
    @Results({
        @Result(column = "medicine_id", property = "medicineId"),
        @Result(column = "create_time", property = "createTime")
    })
    List<MedicineOptionDto> list(@Param("keyword") String keyword);
}
