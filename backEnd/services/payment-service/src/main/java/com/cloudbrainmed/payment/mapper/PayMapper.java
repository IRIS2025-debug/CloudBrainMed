package com.cloudbrainmed.payment.mapper;

import com.cloudbrainmed.payment.entity.Pay;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayMapper {

    /**
     * 插入支付记录
     */
    @Insert("INSERT INTO pay (pay_id, order_type, business_id, patient_id, patient_name, " +
            "total_amount, pay_status, pay_time) " +
            "VALUES (#{payId}, #{orderType}, #{businessId}, #{patientId}, #{patientName}, " +
            "#{totalAmount}, #{payStatus}, #{payTime})")
    int insert(Pay pay);

    /**
     * 支付成功更新
     */
    @Update("UPDATE pay SET pay_status = #{payStatus}, pay_time = #{payTime} " +
            "WHERE pay_id = #{payId} AND pay_status = 'WAITING'")
    int updatePaySuccess(@Param("payId") String payId,
                         @Param("payStatus") String payStatus,
                         @Param("payTime") LocalDateTime payTime);

    /**
     * 更新支付状态
     */
    @Update("UPDATE pay SET pay_status = #{payStatus} WHERE pay_id = #{payId}")
    int updatePayStatus(@Param("payId") String payId,
                        @Param("payStatus") String payStatus);

    /**
     * 根据支付ID查询
     */
    @Select("SELECT * FROM pay WHERE pay_id = #{payId}")
    Pay selectByPayId(@Param("payId") String payId);

    /**
     * 根据业务ID和订单类型查询
     */
    @Select("SELECT * FROM pay WHERE business_id = #{businessId} AND order_type = #{orderType} " +
            "ORDER BY pay_time DESC LIMIT 1")
    Pay selectByBusinessId(@Param("businessId") String businessId,
                           @Param("orderType") String orderType);

    /**
     * 根据患者ID查询历史
     */
    @Select("SELECT * FROM pay WHERE patient_id = #{patientId} ORDER BY pay_time DESC")
    List<Pay> selectByPatientId(@Param("patientId") String patientId);

    /**
     * 分页查询
     */
    @Select("SELECT * FROM pay WHERE 1=1 " +
            "<if test='patientId != null and patientId != \"\"'>AND patient_id = #{patientId}</if>" +
            "<if test='payStatus != null and payStatus != \"\"'>AND pay_status = #{payStatus}</if>" +
            "<if test='orderType != null and orderType != \"\"'>AND order_type = #{orderType}</if>" +
            "ORDER BY pay_time DESC LIMIT #{offset}, #{pageSize}")
    List<Pay> selectPage(@Param("patientId") String patientId,
                         @Param("payStatus") String payStatus,
                         @Param("orderType") String orderType,
                         @Param("offset") int offset,
                         @Param("pageSize") int pageSize);

    /**
     * 统计数量
     */
    @Select("SELECT COUNT(*) FROM pay WHERE 1=1 " +
            "<if test='patientId != null and patientId != \"\"'>AND patient_id = #{patientId}</if>" +
            "<if test='payStatus != null and payStatus != \"\"'>AND pay_status = #{payStatus}</if>" +
            "<if test='orderType != null and orderType != \"\"'>AND order_type = #{orderType}</if>")
    Long countByPatientId(@Param("patientId") String patientId,
                          @Param("payStatus") String payStatus,
                          @Param("orderType") String orderType);

    /**
     * 查询当前最大支付ID
     */
    @Select("SELECT MAX(pay_id) FROM pay")
    String selectMaxPayId();
}