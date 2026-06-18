package com.cloudbrainmed.ai.mapper;

import com.cloudbrainmed.ai.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface DepartmentMapper {

    /**
     * 根据科室ID查询科室信息
     */
    @Select("SELECT dept_id, dept_name, room_id, max_capacity, free_capacity, status, create_time " +
            "FROM department WHERE dept_id = #{deptId}")
    Department selectByDeptId(@Param("deptId") String deptId);

    /**
     * 查询所有启用的科室名称
     */
    @Select("SELECT dept_name FROM department WHERE status = 1")
    List<String> selectAllDeptNames();

    /**
     * 查询所有启用的科室（完整信息）
     */
    @Select("SELECT dept_id, dept_name, room_id, max_capacity, free_capacity, status, create_time " +
            "FROM department WHERE status = 1")
    List<Department> selectAllDepartments();

    /**
     * 获取科室ID到名称的映射
     */
    @Select("SELECT dept_id, dept_name FROM department WHERE status = 1")
    List<Map<String, String>> selectDeptIdNameMap();

    /**
     * 根据房间ID查询科室
     */
    @Select("SELECT dept_id, dept_name, room_id, max_capacity, free_capacity, status, create_time " +
            "FROM department WHERE room_id = #{roomId} AND status = 1")
    Department selectByRoomId(@Param("roomId") String roomId);

    /**
     * 查询有空位的科室（用于分诊）
     */
    @Select("SELECT dept_id, dept_name, room_id, max_capacity, free_capacity, status, create_time " +
            "FROM department WHERE status = 1 AND free_capacity > 0 " +
            "ORDER BY free_capacity DESC")
    List<Department> selectDepartmentsWithAvailableSlots();

    /**
     * 默认方法：获取科室ID到名称的Map
     */
    default Map<String, String> getDeptIdToNameMap() {
        List<Map<String, String>> list = selectDeptIdNameMap();
        Map<String, String> map = new java.util.HashMap<>();
        for (Map<String, String> item : list) {
            map.put(item.get("dept_id"), item.get("dept_name"));
        }
        return map;
    }
}