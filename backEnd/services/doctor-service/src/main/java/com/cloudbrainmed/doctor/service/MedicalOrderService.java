package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;

import java.util.List;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;

public interface MedicalOrderService {
    List<InspectionOrderVo> getAllLabOrders();
    MedicalOrder getByOrderId(String orderId);
    //MedicalOrderConfirmResponse confirm(
            //MedicalOrderConfirmRequest request, String doctorId);
}
