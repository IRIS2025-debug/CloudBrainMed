package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final MedicalOrderMapper mapper;

    @Override
    public List<InspectionOrderVo> getAllLabOrders() {
        return mapper.selectAllLabOrders();
    }

    @Override
    public MedicalOrder getByOrderId(String orderId) {
        return mapper.selectByOrderId(orderId);
    }
}
