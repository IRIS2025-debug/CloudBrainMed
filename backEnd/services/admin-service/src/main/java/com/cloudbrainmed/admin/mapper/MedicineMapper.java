package com.cloudbrainmed.admin.mapper;

import com.cloudbrainmed.admin.entity.Medicine;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MedicineMapper {

    /**
     * 查询所有药品 - 按名称排序
     */
    @Select("SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time " +
            "FROM medicine ORDER BY name COLLATE \"zh_CN\" ASC")
    List<Medicine> selectAll();

    /**
     * 根据名称模糊查询 - 按名称首字母排序
     */
    @Select("SELECT medicine_id, name, spec, usage, indication, attention, stock, price, create_time " +
            "FROM medicine WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY name COLLATE \"zh_CN\" ASC")
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

    /**
     * 批量更新药品 - 使用 CASE WHEN (PostgreSQL)
     */
    @Update("<script>" +
            "UPDATE medicine SET " +
            "  name = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.name}" +
            "    </foreach>" +
            "  END, " +
            "  spec = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.spec}" +
            "    </foreach>" +
            "  END, " +
            "  usage = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.usage}" +
            "    </foreach>" +
            "  END, " +
            "  indication = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.indication}" +
            "    </foreach>" +
            "  END, " +
            "  attention = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.attention}" +
            "    </foreach>" +
            "  END, " +
            "  stock = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.stock}" +
            "    </foreach>" +
            "  END, " +
            "  price = CASE medicine_id " +
            "    <foreach collection='list' item='item'>" +
            "      WHEN #{item.medicineId} THEN #{item.price}" +
            "    </foreach>" +
            "  END " +
            "WHERE medicine_id IN " +
            "  <foreach collection='list' item='item' open='(' separator=',' close=')'>" +
            "    #{item.medicineId}" +
            "  </foreach>" +
            "</script>")
    int batchUpdate(@Param("list") List<Medicine> list);

    /**
     * 批量更新药品 - 只更新非空字段 (PostgreSQL)
     * 使用动态SQL，只更新传入的非空字段
     */
    @Update("<script>" +
            "UPDATE medicine SET " +
            "  <trim suffixOverrides=','>" +
            "    <if test='list[0].name != null'>" +
            "      name = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.name}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].spec != null'>" +
            "      spec = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.spec}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].usage != null'>" +
            "      usage = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.usage}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].indication != null'>" +
            "      indication = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.indication}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].attention != null'>" +
            "      attention = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.attention}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].stock != null'>" +
            "      stock = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.stock}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "    <if test='list[0].price != null'>" +
            "      price = CASE medicine_id " +
            "        <foreach collection='list' item='item'>" +
            "          WHEN #{item.medicineId} THEN #{item.price}" +
            "        </foreach>" +
            "      END, " +
            "    </if>" +
            "  </trim>" +
            "WHERE medicine_id IN " +
            "  <foreach collection='list' item='item' open='(' separator=',' close=')'>" +
            "    #{item.medicineId}" +
            "  </foreach>" +
            "</script>")
    int batchUpdateSelective(@Param("list") List<Medicine> list);
}