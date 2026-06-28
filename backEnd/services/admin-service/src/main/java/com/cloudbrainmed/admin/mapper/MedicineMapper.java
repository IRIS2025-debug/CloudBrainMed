package com.cloudbrainmed.admin.mapper;

import com.cloudbrainmed.admin.entity.Medicine;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MedicineMapper {

    /**
     * 查询所有药品
     */
    @Select("SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time " +
            "FROM medicine ORDER BY create_time DESC")
    List<Medicine> selectAll();

    /**
     * 根据名称模糊查询
     */
    @Select("SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time " +
            "FROM medicine WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY create_time DESC")
    List<Medicine> selectByName(@Param("keyword") String keyword);

    /**
     * 根据ID查询
     */
    @Select("SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time " +
            "FROM medicine WHERE medicine_id = #{medicineId}")
    Medicine selectById(@Param("medicineId") String medicineId);

    /**
     * 新增药品
     */
    @Insert("INSERT INTO medicine (medicine_id, name, spec, usage, indication, attention, stock, price, create_time) " +
            "VALUES (#{medicineId}, #{name}, #{spec}, #{usage}, #{indication}, #{attention}, " +
            "#{stock}, #{price}, NOW())")
    int insert(Medicine medicine);

    /**
     * 更新药品
     */
    @Update("UPDATE medicine SET name = #{name}, spec = #{spec}, usage = #{usage}, " +
            "indication = #{indication}, attention = #{attention}, stock = #{stock}, price = #{price} " +
            "WHERE medicine_id = #{medicineId}")
    int update(Medicine medicine);

    /**
     * 删除药品
     */
    @Delete("DELETE FROM medicine WHERE medicine_id = #{medicineId}")
    int delete(@Param("medicineId") String medicineId);

    /**
     * 扣除库存（原子操作）
     */
    @Update("UPDATE medicine SET stock = stock - #{quantity} " +
            "WHERE medicine_id = #{medicineId} AND stock >= #{quantity}")
    int deductStock(@Param("medicineId") String medicineId, @Param("quantity") Integer quantity);

    /**
     * 增加库存
     */
    @Update("UPDATE medicine SET stock = stock + #{quantity} " +
            "WHERE medicine_id = #{medicineId}")
    int addStock(@Param("medicineId") String medicineId, @Param("quantity") Integer quantity);
}