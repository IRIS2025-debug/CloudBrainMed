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
    @Select("SELECT dept_id, dept_name, parent_id, status, create_time " +
            "FROM department WHERE dept_id = #{deptId}")
    Department selectByDeptId(@Param("deptId") String deptId);

    /**
     * 查询所有启用的科室名称
     */
    @Select("SELECT dept_name FROM department WHERE status = 1")
    List<String> selectAllDeptNames();

    /**
     * 查询所有启用的科室
     */
    @Select("SELECT dept_id, dept_name, parent_id, status, create_time " +
            "FROM department WHERE status = 1")
    List<Department> selectAllDepartments();

    /**
     * 获取科室ID到名称的映射
     */
    @Select("SELECT dept_id, dept_name FROM department WHERE status = 1")
    List<Map<String, String>> selectDeptIdNameMap();

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