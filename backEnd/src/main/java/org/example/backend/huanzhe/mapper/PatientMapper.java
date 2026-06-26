package org.example.backend.huanzhe.mapper;

import org.apache.ibatis.annotations.*;
import org.example.backend.huanzhe.entity.Patient;

@Mapper
public interface PatientMapper {

    @Select("SELECT patient_id, name, gender, phone, id_card, address, avatar, birthday, create_time, update_time FROM patient WHERE patient_id=#{patientId} AND is_deleted=0")
    Patient findById(@Param("patientId") String patientId);

    @Update("UPDATE patient SET name=#{name}, gender=#{gender}, address=#{address}, birthday=#{birthday}, update_time=NOW() WHERE patient_id=#{patientId}")
    int updateInfo(Patient patient);

    @Update("UPDATE patient SET avatar=#{avatar}, update_time=NOW() WHERE patient_id=#{patientId}")
    int updateAvatar(@Param("patientId") String patientId, @Param("avatar") String avatar);

    @Select("SELECT patient_id FROM patient WHERE patient_id=#{patientId} AND phone=#{oldPhone} AND is_deleted=0")
    String checkPhone(@Param("patientId") String patientId, @Param("oldPhone") String oldPhone);

    @Select("SELECT patient_id FROM patient WHERE phone=#{newPhone} AND is_deleted=0")
    String findByPhone(String newPhone);

    @Update("UPDATE patient SET phone=#{newPhone}, update_time=NOW() WHERE patient_id=#{patientId}")
    int updatePhone(@Param("patientId") String patientId, @Param("newPhone") String newPhone);

    @Select("SELECT password FROM patient WHERE patient_id=#{patientId} AND is_deleted=0")
    String findPasswordById(@Param("patientId") String patientId);

    @Update("UPDATE patient SET password=#{newPassword}, update_time=NOW() WHERE patient_id=#{patientId}")
    int updatePassword(@Param("patientId") String patientId, @Param("newPassword") String newPassword);
}
