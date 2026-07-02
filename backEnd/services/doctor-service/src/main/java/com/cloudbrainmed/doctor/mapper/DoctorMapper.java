package com.cloudbrainmed.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudbrainmed.doctor.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
    @Update("UPDATE doctor SET phone = #{phone} WHERE doctor_id = #{doctorId}")
    int updatePhone(@Param("doctorId") String doctorId, @Param("phone") String phone);
}
