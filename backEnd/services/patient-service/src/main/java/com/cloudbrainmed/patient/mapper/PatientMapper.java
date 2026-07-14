package com.cloudbrainmed.patient.mapper;

import com.cloudbrainmed.patient.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 患者Mapper接口
 */
@Mapper
public interface PatientMapper {

    /**
     * 插入患者
     */
    @Insert("INSERT INTO patient (patient_id, name, gender, birthday, phone, id_card, " +
            "address, password, create_time, update_time) " +
            "VALUES (#{patientId}, #{name}, #{gender}, #{birthday}, #{phone}, #{idCard}, " +
            "#{address}, #{password}, NOW(), NOW())")
    int insert(Patient patient);

    /**
     * 更新患者信息
     */
    @Update("UPDATE patient SET name = #{name}, gender = #{gender}, birthday = #{birthday}, " +
            "address = #{address}, id_card = #{idCard}, " +
            "phone = #{phone}, update_time = NOW() WHERE patient_id = #{patientId}")
    int update(Patient patient);

    @Update("UPDATE patient SET name = #{name}, gender = #{gender}, birthday = #{birthday}, " +
            "address = #{address}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updateBasicInfo(Patient patient);

    /**
     * 根据ID查询患者
     */
    @Select("SELECT * FROM patient WHERE patient_id = #{patientId} AND is_deleted = 0")
    Patient selectById(@Param("patientId") String patientId);

    /**
     * 根据手机号查询患者
     */
    @Select("SELECT * FROM patient WHERE phone = #{phone} AND is_deleted = 0")
    Patient selectByPhone(@Param("phone") String phone);

    /**
     * 根据身份证号查询患者
     */
    @Select("SELECT * FROM patient WHERE id_card = #{idCard} AND is_deleted = 0")
    Patient selectByIdCard(@Param("idCard") String idCard);


    /**
     * 更新密码
     */
    @Update("UPDATE patient SET password = #{password}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updatePassword(@Param("patientId") String patientId, @Param("password") String password);

    @Update("UPDATE patient SET avatar = #{avatar}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updateAvatar(@Param("patientId") String patientId, @Param("avatar") String avatar);

    @Update("UPDATE patient SET phone = #{phone}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updatePhone(@Param("patientId") String patientId, @Param("phone") String phone);

    @Update("UPDATE patient SET id_card = #{idCard}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updateIdCard(@Param("patientId") String patientId, @Param("idCard") String idCard);

    /**
     * 更新患者地址
     */
    @Update("UPDATE patient SET address = #{address}, update_time = NOW() WHERE patient_id = #{patientId}")
    int updateAddress(@Param("patientId") String patientId, @Param("address") String address);

}
