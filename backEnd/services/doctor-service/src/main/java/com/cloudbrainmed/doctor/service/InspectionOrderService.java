package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.InspectionOrder;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import java.util.List;

public interface InspectionOrderService {
    List<InspectionOrderVo> getAllLabOrders();
    InspectionOrder getByOrderId(String orderId);
}
