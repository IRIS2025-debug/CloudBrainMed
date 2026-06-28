package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;

import java.util.List;

public interface MedicalOrderService {
    List<InspectionOrderVo> getAllLabOrders();
    MedicalOrder getByOrderId(String orderId);
}
