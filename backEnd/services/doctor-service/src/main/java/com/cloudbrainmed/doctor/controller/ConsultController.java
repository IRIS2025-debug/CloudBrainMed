package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.entity.Prescription;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/doctor-service/consult")
public class ConsultController {

    private final ConsultService service;
    private final PrescriptionService prescriptionService;

    public ConsultController(ConsultService service, PrescriptionService prescriptionService) {
        this.service = service;
        this.prescriptionService = prescriptionService;
    }

    /** 4.4.2.1 查询接诊患者列表 */
    @GetMapping("/list")
    public Result<?> list(@RequestHeader(value = "token", required = false) String token,
                          @RequestParam(required = false) String consultStatus,
                          @RequestParam(required = false) String date,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int limit) {
        String doctorId = extractDoctorId(token);
        return Result.ok(service.getList(doctorId, consultStatus, date, page, limit));
    }

    /** 4.4.2.2 获取接诊详情 */
    @GetMapping("/detail")
    public Result<?> detail(@RequestHeader(value = "token", required = false) String token,
                            @RequestParam String registerId) {
        String doctorId = extractDoctorId(token);
        return Result.ok(service.getDetail(doctorId, registerId));
    }

    /** 4.4.2.3 暂存病历草稿 */
    @PostMapping("/save-draft")
    public Result<?> saveDraft(@RequestHeader(value = "token", required = false) String token,
                               @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        service.saveDraft(doctorId, body.get("registerId"), body.get("recordDesc"));
        return Result.ok();
    }

    /** 4.4.2.4 确认正式病历 */
    @PostMapping("/confirm-record")
    public Result<?> confirmRecord(@RequestHeader(value = "token", required = false) String token,
                                   @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        service.confirmRecord(doctorId, body.get("registerId"), body.get("recordDesc"));
        return Result.ok();
    }

    /** 4.4.2.5 生成检查申请单 */
    @PostMapping("/create-exam-order")
    public Result<?> createExamOrder(@RequestHeader(value = "token", required = false) String token,
                                     @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        service.createExamOrder(doctorId, body.get("registerId"),
                body.get("checkItemList"), body.get("urgencyLevel"));
        return Result.ok();
    }

    /** 开具处方 */
    @PostMapping("/create-prescription")
    public Result<?> createPrescription(@RequestHeader(value = "token", required = false) String token,
                                        @RequestBody PrescriptionCreateDto dto) {
        String doctorId = extractDoctorId(token);
        // 从前端的扩展获取 doctorName，此处暂时用 doctorId 作为 fallback
        ConsultRecord record = service.getDetail(doctorId, dto.getRegisterId());
        if ("COMPLETED".equals(record.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能继续开具处方");
        }
        Prescription p = prescriptionService.create(dto, doctorId, doctorId);
        return Result.ok(p);
    }

    /** 按挂号ID查询已开处方 */
    @GetMapping("/prescription-list")
    public Result<?> prescriptionList(@RequestHeader(value = "token", required = false) String token,
                                      @RequestParam String registerId) {
        String doctorId = extractDoctorId(token);
        service.getDetail(doctorId, registerId);
        return Result.ok(prescriptionService.getByRegisterId(registerId));
    }

    /** 4.4.2.6 完成本次接诊 */
    @PostMapping("/complete")
    public Result<?> complete(@RequestHeader(value = "token", required = false) String token,
                              @RequestBody Map<String, String> body) {
        String doctorId = extractDoctorId(token);
        service.completeConsult(doctorId, body.get("registerId"));
        return Result.ok();
    }

    /**
     * 从 JWT 中解析并校验接诊医生身份，返回 doctorId。
     * 要求 roleType==2 且 doctorType==1（接诊医生）。
     */
    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        Integer roleType;
        Integer doctorType;
        String doctorId;
        try {
            roleType = DoctorJwtUtil.getRoleType(token);
            doctorType = DoctorJwtUtil.getDoctorType(token);
            doctorId = DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new BusinessException("医生登录凭证无效");
        }
        if (!Integer.valueOf(2).equals(roleType)) {
            throw new BusinessException("仅医生可访问接诊功能");
        }
        if (!Integer.valueOf(1).equals(doctorType)) {
            throw new BusinessException("仅接诊医生可访问接诊功能");
        }
        return doctorId;
    }
}
