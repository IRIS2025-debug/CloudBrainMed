package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.InspectionOrder;
import com.cloudbrainmed.doctor.mapper.InspectionOrderMapper;
import com.cloudbrainmed.doctor.service.InspectionOrderService;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InspectionOrderServiceImpl implements InspectionOrderService {

    private final InspectionOrderMapper mapper;

    public InspectionOrderServiceImpl(InspectionOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<InspectionOrderVo> getAllLabOrders() {
        return mapper.selectAllLabOrders();
    }

    @Override
    public InspectionOrder getByOrderId(String orderId) {
        return mapper.selectByOrderId(orderId);
    }
}
