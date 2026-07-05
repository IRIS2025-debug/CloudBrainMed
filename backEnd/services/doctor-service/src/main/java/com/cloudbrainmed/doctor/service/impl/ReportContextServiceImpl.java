package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.api.dto.ReportContextDto;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.service.ReportContextService;
import org.springframework.stereotype.Service;

@Service
public class ReportContextServiceImpl implements ReportContextService {

    private final ConsultMapper consultMapper;

    public ReportContextServiceImpl(ConsultMapper consultMapper) {
        this.consultMapper = consultMapper;
    }

    @Override
    public ReportContextDto getContext(String registerId, String doctorId) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) {
            throw new BusinessException("就诊记录不存在");
        }
        if (doctorId == null || !doctorId.equals(detail.getDoctorId())) {
            throw new BusinessException("无权访问该患者的接诊信息");
        }
        if ("COMPLETED".equals(detail.getConsultStatus())) {
            throw new BusinessException("接诊已完成，不能再次发起AI分析");
        }

        ReportContextDto context = new ReportContextDto();
        context.setRegisterId(detail.getRegisterId());
        context.setPatientId(detail.getPatientId());
        context.setPatientAge(detail.getPatientAge());
        context.setPatientGender(toGenderText(detail.getGender()));
        context.setChiefComplaint(detail.getChiefComplaint());
        context.setCurrentRecordDesc(detail.getDescription());
        context.setMedicalHistory(consultMapper.findMedicalHistory(
                detail.getPatientId(), detail.getRegisterId()));
        context.setPreviousReports(consultMapper.findPreviousReports(
                detail.getPatientId(), detail.getRegisterId()));
        return context;
    }

    private String toGenderText(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        return gender == 2 ? "女" : "未知";
    }
}