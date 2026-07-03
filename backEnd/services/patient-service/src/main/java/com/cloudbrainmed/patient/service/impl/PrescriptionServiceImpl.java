package com.cloudbrainmed.patient.service.impl;

import com.cloudbrainmed.patient.entity.Prescription;
import com.cloudbrainmed.patient.mapper.PrescriptionMapper;
import com.cloudbrainmed.patient.service.PrescriptionService;
import com.cloudbrainmed.patient.vo.RegisterPrescriptionGroupVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.util.List;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper mapper;

    public PrescriptionServiceImpl(PrescriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Prescription> getByRegisterId(String registerId, String patientId) {
        return mapper.selectByRegisterId(registerId, patientId);
    }

    @Override
    public List<Prescription> getByPatientId(String patientId) {
        return mapper.selectByPatientId(patientId);
    }

    @Override
    public List<RegisterPrescriptionGroupVo> getGroupedByPatientId(String patientId) {
        List<Prescription> prescriptions = mapper.selectByPatientId(patientId);
        if (prescriptions == null || prescriptions.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 registerId 分组，保持顺序
        Map<String, List<Prescription>> groupMap = prescriptions.stream()
                .collect(Collectors.groupingBy(
                        Prescription::getRegisterId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RegisterPrescriptionGroupVo> result = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<String, List<Prescription>> entry : groupMap.entrySet()) {
            String registerId = entry.getKey();
            List<Prescription> list = entry.getValue();

            RegisterPrescriptionGroupVo group = new RegisterPrescriptionGroupVo();
            group.setRegisterId(registerId);

            // 取第一条记录填充公共信息
            Prescription first = list.get(0);
            group.setPatientName(first.getPatientName());
            group.setDoctorName(first.getDoctorName());

            if (first.getPrescriptionDate() != null) {
                group.setPrescriptionDate(first.getPrescriptionDate().format(dateFormatter));
            } else {
                group.setPrescriptionDate("");
            }

            group.setPayStatus(first.getPayStatus());
            group.setPrescriptions(list);

            // 计算总金额
            BigDecimal total = list.stream()
                    .map(p -> p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            group.setTotalAmount(total);
            group.setPrescriptionCount(list.size());

            result.add(group);
        }

        return result;
    }
}
