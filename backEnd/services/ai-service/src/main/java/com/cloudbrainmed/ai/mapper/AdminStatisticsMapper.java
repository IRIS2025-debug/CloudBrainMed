package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.dto.DateValueStat;
import com.cloudbrainmed.ai.dto.NameValueStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminStatisticsMapper {

    @Select("SELECT COUNT(*) FROM patient WHERE COALESCE(is_deleted, 0) = 0")
    Long countPatients();

    @Select("SELECT CASE gender WHEN 1 THEN '男' WHEN 2 THEN '女' ELSE '未知' END AS name, COUNT(*)::numeric AS value " +
            "FROM patient WHERE COALESCE(is_deleted, 0) = 0 GROUP BY gender ORDER BY value DESC")
    List<NameValueStat> countPatientsByGender();

    @Select("SELECT age_range AS name, COUNT(*)::numeric AS value FROM (" +
            "SELECT CASE " +
            "WHEN birthday IS NULL THEN '未知' " +
            "WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, birthday)) < 18 THEN '0-17岁' " +
            "WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, birthday)) BETWEEN 18 AND 30 THEN '18-30岁' " +
            "WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, birthday)) BETWEEN 31 AND 45 THEN '31-45岁' " +
            "WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, birthday)) BETWEEN 46 AND 60 THEN '46-60岁' " +
            "ELSE '60岁以上' END AS age_range FROM patient WHERE COALESCE(is_deleted, 0) = 0" +
            ") t GROUP BY age_range ORDER BY MIN(CASE age_range WHEN '0-17岁' THEN 1 WHEN '18-30岁' THEN 2 WHEN '31-45岁' THEN 3 WHEN '46-60岁' THEN 4 WHEN '60岁以上' THEN 5 ELSE 6 END)")
    List<NameValueStat> countPatientsByAgeRange();

    @Select("SELECT COUNT(*) FROM registration WHERE visit_date BETWEEN #{start} AND #{end}")
    Long countRegistrations(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT visit_date AS statDate, COUNT(*)::numeric AS value FROM registration " +
            "WHERE visit_date BETWEEN #{start} AND #{end} GROUP BY visit_date ORDER BY visit_date")
    List<DateValueStat> countDailyRegistrations(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(department, '未知科室') AS name, COUNT(*)::numeric AS value FROM registration " +
            "WHERE visit_date BETWEEN #{start} AND #{end} GROUP BY department ORDER BY value DESC LIMIT 10")
    List<NameValueStat> countRegistrationsByDepartment(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(d.name, r.doctor_id, '未知医生') AS name, COUNT(*)::numeric AS value FROM registration r " +
            "LEFT JOIN doctor d ON d.doctor_id = r.doctor_id " +
            "WHERE r.visit_date BETWEEN #{start} AND #{end} GROUP BY COALESCE(d.name, r.doctor_id, '未知医生') ORDER BY value DESC LIMIT 10")
    List<NameValueStat> countRegistrationsByDoctor(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COUNT(*) FROM medical_order WHERE create_time::date BETWEEN #{start} AND #{end}")
    Long countMedicalOrders(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT CASE WHEN COUNT(*) = 0 THEN 0 ELSE ROUND(SUM(CASE WHEN status IN ('COMPLETED','REPORTED','DONE') THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) END FROM medical_order_item WHERE create_time::date BETWEEN #{start} AND #{end}")
    BigDecimal medicalItemCompletionRate(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(item_category, 'UNKNOWN') AS name, COUNT(*)::numeric AS value FROM medical_order_item " +
            "WHERE create_time::date BETWEEN #{start} AND #{end} GROUP BY item_category ORDER BY value DESC")
    List<NameValueStat> countMedicalItemsByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COUNT(*) FROM medical_report WHERE create_time::date BETWEEN #{start} AND #{end} AND COALESCE(abnormal_flag, '') NOT IN ('', 'NORMAL', '0', 'false', 'FALSE')")
    Long countAbnormalReports(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM pay WHERE pay_status = 'PAID' AND pay_time::date BETWEEN #{start} AND #{end}")
    BigDecimal sumPaidAmount(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT pay_time::date AS statDate, COALESCE(SUM(total_amount), 0) AS value FROM pay " +
            "WHERE pay_status = 'PAID' AND pay_time::date BETWEEN #{start} AND #{end} GROUP BY pay_time::date ORDER BY pay_time::date")
    List<DateValueStat> sumDailyPaidAmount(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(pay_status, 'UNKNOWN') AS name, COUNT(*)::numeric AS value FROM pay " +
            "WHERE pay_time::date BETWEEN #{start} AND #{end} GROUP BY pay_status ORDER BY value DESC")
    List<NameValueStat> countPayStatus(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
