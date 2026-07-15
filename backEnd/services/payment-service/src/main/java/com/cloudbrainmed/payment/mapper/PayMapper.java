package com.cloudbrainmed.payment.mapper;

import com.cloudbrainmed.payment.entity.Pay;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PayMapper {

    @Update("ALTER TABLE prescription " +
            "ADD COLUMN IF NOT EXISTS stock_deducted BOOLEAN NOT NULL DEFAULT FALSE")
    int ensurePrescriptionStockDeductedColumn();

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
     * 按当前状态条件更新支付状态，避免并发覆盖状态迁移。
     */
    @Update("UPDATE pay SET pay_status = #{payStatus} " +
            "WHERE pay_id = #{payId} AND pay_status = #{expectedStatus}")
    int updatePayStatusFrom(@Param("payId") String payId,
                            @Param("payStatus") String payStatus,
                            @Param("expectedStatus") String expectedStatus);

    default int updateBusinessPayStatus(String orderType, String businessId, String payStatus) {
        if ("MEDICAL".equals(orderType)) {
            return updateMedicalOrderPayStatus(businessId, payStatus);
        }
        if ("PRESCRIPTION".equals(orderType)) {
            return updatePrescriptionPayStatus(businessId, payStatus);
        }
        if ("REGISTER".equals(orderType)) {
            return updateRegistrationPayStatus(businessId, payStatus);
        }
        return 0;
    }

    @Update("UPDATE medical_order SET pay_status = #{payStatus} WHERE order_id = #{businessId}")
    int updateMedicalOrderPayStatus(@Param("businessId") String businessId,
                                    @Param("payStatus") String payStatus);

    @Update("UPDATE prescription SET pay_status = #{payStatus} WHERE prescription_id = #{businessId}")
    int updatePrescriptionPayStatus(@Param("businessId") String businessId,
                                    @Param("payStatus") String payStatus);

    /**
     * 按处方数量原子扣减库存，并记录该处方已经扣过库存。
     */
    @Update("""
        WITH claimed AS (
            UPDATE prescription AS p
            SET stock_deducted = TRUE
            FROM medicine AS m
            WHERE p.prescription_id = #{prescriptionId}
              AND p.medicine_id = m.medicine_id
              AND p.medicine_id IS NOT NULL
              AND p.num > 0
              AND p.stock_deducted = FALSE
              AND m.stock IS NOT NULL
              AND m.stock >= p.num
            RETURNING p.prescription_id, p.medicine_id, p.num
        )
        UPDATE medicine AS m
        SET stock = m.stock - c.num
        FROM claimed AS c
        WHERE m.medicine_id = c.medicine_id
          AND m.stock IS NOT NULL
          AND m.stock >= c.num
        """)
    int deductPrescriptionStock(@Param("prescriptionId") String prescriptionId);

    /**
     * 仅对实际扣过库存的处方原子回补库存。
     */
    @Update("""
        WITH claimed AS (
            UPDATE prescription AS p
            SET stock_deducted = FALSE
            WHERE p.prescription_id = #{prescriptionId}
              AND p.medicine_id IS NOT NULL
              AND p.num > 0
              AND p.stock_deducted = TRUE
            RETURNING p.prescription_id, p.medicine_id, p.num
        )
        UPDATE medicine AS m
        SET stock = m.stock + c.num
        FROM claimed AS c
        WHERE m.medicine_id = c.medicine_id
        """)
    int restorePrescriptionStock(@Param("prescriptionId") String prescriptionId);

    @Select("SELECT stock_deducted FROM prescription WHERE prescription_id = #{prescriptionId}")
    Boolean isPrescriptionStockDeducted(@Param("prescriptionId") String prescriptionId);

    @Update("UPDATE registration SET pay_status = #{payStatus} WHERE register_id = #{businessId}")
    int updateRegistrationPayStatus(@Param("businessId") String businessId,
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
    @Select("""
        <script>
        SELECT * FROM pay WHERE 1=1
        <if test='patientId != null and patientId != ""'>AND patient_id = #{patientId}</if>
        <if test='payStatus != null and payStatus != ""'>AND pay_status = #{payStatus}</if>
        <if test='orderType != null and orderType != ""'>AND order_type = #{orderType}</if>
        ORDER BY pay_time DESC LIMIT #{pageSize} OFFSET #{offset}
        </script>
        """)
    List<Pay> selectPage(@Param("patientId") String patientId,
                         @Param("payStatus") String payStatus,
                         @Param("orderType") String orderType,
                         @Param("offset") int offset,
                         @Param("pageSize") int pageSize);

    /**
     * 统计数量
     */
    @Select("""
        <script>
        SELECT COUNT(*) FROM pay WHERE 1=1
        <if test='patientId != null and patientId != ""'>AND patient_id = #{patientId}</if>
        <if test='payStatus != null and payStatus != ""'>AND pay_status = #{payStatus}</if>
        <if test='orderType != null and orderType != ""'>AND order_type = #{orderType}</if>
        </script>
        """)
    Long countByPatientId(@Param("patientId") String patientId,
                          @Param("payStatus") String payStatus,
                          @Param("orderType") String orderType);

    /**
     * 查询当前最大支付ID
     */
    @Select("SELECT MAX(pay_id) FROM pay")
    String selectMaxPayId();
}
