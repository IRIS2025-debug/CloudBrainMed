package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.service.ConsultService;
import com.cloudbrainmed.doctor.service.PrescriptionService;
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
        service.saveDraft(body.get("registerId"), body.get("recordDesc"));
        return Result.ok();
    }

    /** 4.4.2.4 确认正式病历 */
    @PostMapping("/confirm-record")
    public Result<?> confirmRecord(@RequestHeader(value = "token", required = false) String token,
                                   @RequestBody Map<String, String> body) {
        service.confirmRecord(body.get("registerId"), body.get("recordDesc"));
        return Result.ok();
    }

    /** 4.4.2.5 生成检查申请单 */
    @PostMapping("/create-exam-order")
    public Result<?> createExamOrder(@RequestHeader(value = "token", required = false) String token,
                                     @RequestBody Map<String, String> body) {
        service.createExamOrder(body.get("registerId"),
                body.get("checkItemList"), body.get("urgencyLevel"));
        return Result.ok();
    }

    /** 开具处方 */
    @PostMapping("/create-prescription")
    public Result<?> createPrescription(@RequestHeader(value = "token", required = false) String token,
                                        @RequestBody PrescriptionCreateDto dto) {
        String doctorId = extractDoctorId(token);
        // 从前端的扩展获取 doctorName，此处暂时用 doctorId 作为 fallback
        Prescription p = prescriptionService.create(dto, doctorId, doctorId);
        return Result.ok(p);
    }

    /** 按挂号ID查询已开处方 */
    @GetMapping("/prescription-list")
    public Result<?> prescriptionList(@RequestParam String registerId) {
        return Result.ok(prescriptionService.getByRegisterId(registerId));
    }

    /** 4.4.2.6 完成本次接诊 */
    @PostMapping("/complete")
    public Result<?> complete(@RequestHeader(value = "token", required = false) String token,
                              @RequestBody Map<String, String> body) {
        service.completeConsult(body.get("registerId"));
        return Result.ok();
    }

    /**
     * 从 header 或 JWT 中提取 doctorId。
     * 优先用 DoctorJwtUtil 解析 JWT 的 userId claim；
     * 兼容旧版直传 doctorId 的场景。
     */
    private String extractDoctorId(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录，请先登录");
        }
        try {
            return DoctorJwtUtil.getUserId(token);
        } catch (Exception e) {
            // 兼容直接传 doctorId 的场景
            return token;
        }
    }
}