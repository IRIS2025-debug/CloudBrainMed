package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.dto.PrescriptionCreateDto;
import com.cloudbrainmed.doctor.entity.Prescription;
import com.cloudbrainmed.doctor.mapper.PrescriptionMapper;
import com.cloudbrainmed.doctor.service.PrescriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper mapper;

    public PrescriptionServiceImpl(PrescriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Prescription create(PrescriptionCreateDto dto, String doctorId, String doctorName) {
        Prescription p = new Prescription();
        p.setPrescriptionId("PRE" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase());
        p.setRegisterId(dto.getRegisterId());
        p.setDoctorId(doctorId);
        p.setDoctorName(doctorName);
        p.setMedicineName(dto.getMedicineName());
        p.setSpec(dto.getSpec());
        p.setUsage(dto.getUsage());
        p.setNum(dto.getNum());
        p.setPrice(dto.getPrice());
        p.setPrescriptionDate(LocalDate.now());
        mapper.insert(p);
        return p;
    }

    @Override
    public List<Prescription> getByRegisterId(String registerId) {
        return mapper.selectByRegisterId(registerId);
    }

    @Override
    public Prescription getById(String prescriptionId) {
        return mapper.selectById(prescriptionId);
    }
}
