package com.cloudbrainmed.ai.controller;

import com.cloudbrainmed.ai.dto.DoctorMedicineChatRequest;
import com.cloudbrainmed.ai.service.DoctorMedicineQueryService;
import com.cloudbrainmed.ai.support.DoctorAuthHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生端AI药品查询接口。
 *
 * <p>无状态药品说明书 / 禁忌 / 相互作用查询，仅限医生角色调用，与患者端
 * 用药助手（/ai-service/ai/medicine）和接诊处方草稿完全独立。</p>
 */
@RestController
@RequestMapping("/ai-service/medicine")
public class DoctorMedicineQueryController {

    private final DoctorMedicineQueryService doctorMedicineQueryService;

    public DoctorMedicineQueryController(
            DoctorMedicineQueryService doctorMedicineQueryService) {
        this.doctorMedicineQueryService = doctorMedicineQueryService;
    }

    @PostMapping(value = "/chat", produces = "text/plain;charset=UTF-8")
    public String chat(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody DoctorMedicineChatRequest request) {
        DoctorAuthHelper.requireDoctorId(token, "AI药品查询");
        return doctorMedicineQueryService.query(request.getQuestion());
    }
}
