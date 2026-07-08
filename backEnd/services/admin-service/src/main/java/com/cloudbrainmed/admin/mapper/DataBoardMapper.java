package com.cloudbrainmed.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataBoardMapper {

    @Select("SELECT COUNT(*) FROM doctor WHERE COALESCE(is_deleted, 0) = 0")
    long countDoctors();

    @Select("SELECT COUNT(*) FROM patient WHERE COALESCE(is_deleted, 0) = 0")
    long countPatients();

    @Select("SELECT COUNT(*) FROM registration WHERE visit_date = CURRENT_DATE")
    long countTodayRegistrations();

    @Select("SELECT COUNT(*) FROM medical_order " +
            "WHERE status IN ('WAITING_ASSIGN', 'QUEUED')")
    long countPendingMedicalOrders();

    @Select("SELECT COUNT(*) FROM medicine WHERE stock <= 10")
    long countLowStockMedicines();
}
