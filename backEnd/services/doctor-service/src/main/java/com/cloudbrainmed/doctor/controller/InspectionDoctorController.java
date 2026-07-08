package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalOrderAssignRequest;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inspection-doctor")
@RequiredArgsConstructor
public class InspectionDoctorController {

    private final MedicalOrderService medicalOrderService;

    @GetMapping({"/orders", "/lab-orders"})
    public Result<?> getAllLabOrders(@RequestHeader(value = "token", required = false) String token) {
        DoctorContext doctor = requireInspectionDoctor(token);
        List<InspectionOrderVo> orders = medicalOrderService.getAllLabOrders();
        return Result.ok(orders.stream()
                .filter(order -> matchesDoctorType(order, doctor.doctorType()))
                .peek(order -> annotateActions(order, doctor.doctorId()))
                .toList());
    }

    @GetMapping("/order-item/{orderItemId}")
    public Result<?> getByOrderItemId(@RequestHeader(value = "token", required = false) String token,
                                      @PathVariable String orderItemId) {
        DoctorContext doctor = requireInspectionDoctor(token);
        requireVisibleOrderItem(orderItemId, doctor.doctorType());
        return Result.ok(medicalOrderService.getByOrderItemId(orderItemId));
    }

    @PostMapping("/order-item/{orderItemId}/assign")
    public Result<?> assignOrderItem(@RequestHeader(value = "token", required = false) String token,
                                     @PathVariable String orderItemId,
                                     @RequestBody(required = false) MedicalOrderAssignRequest request) {
        DoctorContext doctor = requireInspectionDoctor(token);
        requireVisibleOrderItem(orderItemId, doctor.doctorType());
        String assignedRoom = request == null ? null : request.getAssignedRoom();
        return Result.ok(medicalOrderService.assignOrderItem(orderItemId, assignedRoom));
    }

    private DoctorContext requireInspectionDoctor(String token) {
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
        return new DoctorContext(DoctorJwtUtil.getUserId(token), doctorType);
    }

    private boolean matchesDoctorType(InspectionOrderVo order, Integer doctorType) {
        if (order == null) {
            return false;
        }
        if (Integer.valueOf(2).equals(doctorType)) {
            return "EXAM".equals(order.getItemCategory());
        }
        if (Integer.valueOf(3).equals(doctorType)) {
            return "LAB".equals(order.getItemCategory());
        }
        return false;
    }

    private void annotateActions(InspectionOrderVo order, String doctorId) {
        boolean assignedToCurrentDoctor = doctorId.equals(order.getAssignedDoctorId());
        order.setCanStart("QUEUED".equals(order.getStatus()));
        order.setCanWriteReport("IN_PROCESS".equals(order.getStatus())
                && assignedToCurrentDoctor);
        order.setCanViewReport("COMPLETED".equals(order.getStatus()));
    }

    private void requireVisibleOrderItem(String orderItemId, Integer doctorType) {
        boolean visible = medicalOrderService.getAllLabOrders().stream()
                .anyMatch(order -> orderItemId.equals(order.getOrderItemId())
                        && matchesDoctorType(order, doctorType));
        if (!visible) {
            throw new BusinessException("无权访问该检查/检验申请");
        }
    }

    private record DoctorContext(String doctorId, Integer doctorType) {
    }
}
