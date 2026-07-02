package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.mapper.MedicineOptionMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctor-service/medicine")
public class DoctorMedicineController {

    private final MedicineOptionMapper medicineOptionMapper;

    public DoctorMedicineController(MedicineOptionMapper medicineOptionMapper) {
        this.medicineOptionMapper = medicineOptionMapper;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestHeader(value = "token", required = false) String token,
                          @RequestParam(required = false) String keyword) {
        extractDoctorId(token);
        return Result.ok(medicineOptionMapper.list(keyword));
    }

    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        Integer roleType;
        String doctorId;
        try {
            roleType = DoctorJwtUtil.getRoleType(token);
            doctorId = DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("医生登录凭证无效");
        }
        if (!Integer.valueOf(2).equals(roleType)) {
            throw new RuntimeException("仅医生可以查看药品列表");
        }
        return doctorId;
    }
}
