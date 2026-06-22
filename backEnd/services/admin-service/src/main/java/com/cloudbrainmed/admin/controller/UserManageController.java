package com.cloudbrainmed.admin.controller;

import com.cloudbrainmed.admin.service.DoctorManageService;
import com.cloudbrainmed.admin.vo.DoctorManageVo;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-service/doctor-manage")
public class UserManageController {

    @Resource
    private DoctorManageService doctorManageService;

    /**
     * 获取医生列表
     */
    @GetMapping("/list")
    public Result<List<DoctorManageVo>> list() {
        return Result.ok(doctorManageService.listAll());
    }

    /**
     * 根据ID获取医生详情
     */
    @GetMapping("/detail/{doctorId}")
    public Result<DoctorManageVo> detail(@PathVariable String doctorId) {
        return Result.ok(doctorManageService.getById(doctorId));
    }

    /**
     * 新增医生
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Object> body) {
        doctorManageService.addDoctor(
                (String) body.get("name"),
                body.get("gender") != null ? Integer.valueOf(body.get("gender").toString()) : null,
                (String) body.get("phone"),
                (String) body.get("email"),
                (String) body.get("position"),
                (String) body.get("goodAt"),
                (String) body.get("introduction"),
                (String) body.get("departmentId")
        );
        return Result.ok();
    }

    /**
     * 修改医生
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> body) {
        doctorManageService.updateDoctor(
                (String) body.get("doctorId"),
                (String) body.get("name"),
                body.get("gender") != null ? Integer.valueOf(body.get("gender").toString()) : null,
                (String) body.get("phone"),
                (String) body.get("email"),
                (String) body.get("position"),
                (String) body.get("goodAt"),
                (String) body.get("introduction"),
                (String) body.get("departmentId"),
                body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null
        );
        return Result.ok();
    }

    /**
     * 删除医生（软删除）
     */
    @DeleteMapping("/delete/{doctorId}")
    public Result<?> delete(@PathVariable String doctorId) {
        doctorManageService.deleteDoctor(doctorId);
        return Result.ok();
    }
}