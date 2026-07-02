package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalOrderAssignRequest;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inspection-doctor")
@RequiredArgsConstructor
public class InspectionDoctorController {

    private final MedicalOrderService medicalOrderService;

    @GetMapping({"/orders", "/lab-orders"})
    public Result<?> getAllLabOrders(@RequestHeader(value = "token", required = false) String token) {
        requireInspectionDoctor(token);
        return Result.ok(medicalOrderService.getAllLabOrders());
    }

    @GetMapping("/order/{orderId}")
    public Result<?> getByOrderId(@RequestHeader(value = "token", required = false) String token,
                                  @PathVariable String orderId) {
        requireInspectionDoctor(token);
        return Result.ok(medicalOrderService.getByOrderId(orderId));
    }

    @PostMapping("/order/{orderId}/assign")
    public Result<?> assignOrder(@RequestHeader(value = "token", required = false) String token,
                                 @PathVariable String orderId,
                                 @RequestBody(required = false) MedicalOrderAssignRequest request) {
        requireInspectionDoctor(token);
        String assignedRoom = request == null ? null : request.getAssignedRoom();
        return Result.ok(medicalOrderService.assignOrder(orderId, assignedRoom));
    }

    /**
     * 校验调用者为检查/检验医生（roleType==2 且 doctorType 为 2 或 3）。
     */
    private void requireInspectionDoctor(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        Integer roleType;
        Integer doctorType;
        try {
            roleType = DoctorJwtUtil.getRoleType(token);
            doctorType = DoctorJwtUtil.getDoctorType(token);
        } catch (Exception e) {
            throw new BusinessException("医生登录凭证无效");
        }
        if (!Integer.valueOf(2).equals(roleType)) {
            throw new BusinessException("仅医生可访问检验功能");
        }
        if (!Integer.valueOf(2).equals(doctorType)
                && !Integer.valueOf(3).equals(doctorType)) {
            throw new BusinessException("仅检查/检验医生可访问检查/检验功能");
        }
    }
}
