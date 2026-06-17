package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;
import com.cloudbrainmed.doctor.service.ConsultService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ConsultServiceImpl implements ConsultService {

    private final ConsultMapper consultMapper;

    public ConsultServiceImpl(ConsultMapper consultMapper) {
        this.consultMapper = consultMapper;
    }

    @Override
    public List<ConsultRecord> getList(String doctorId, String consultStatus, String date, int page, int limit) {
        int offset = (page - 1) * limit;
        return consultMapper.findList(doctorId, consultStatus, date, offset, limit);
    }

    @Override
    public ConsultRecord getDetail(String doctorId, String registerId) {
        ConsultRecord r = consultMapper.findDetail(registerId);
        if (r == null) throw new BusinessException("就诊记录不存在");
        if (!doctorId.equals(r.getDoctorId())) {
            throw new BusinessException("无权查看该接诊记录");
        }
        return r;
    }

    @Override
    public void saveDraft(String registerId, String recordDesc) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");

        String recordId = consultMapper.findRecordId(registerId);
        if (recordId == null) {
            // 新建病历
            ConsultRecord r = new ConsultRecord();
            r.setRecordId("REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            r.setPatientId(detail.getPatientId());
            r.setDoctorId(detail.getDoctorId());
            r.setRegisterId(registerId);
            r.setDoctorName(detail.getDoctorName());
            r.setPatientName(detail.getPatientName() != null ? detail.getPatientName() : detail.getName());
            r.setVisitAge(detail.getPatientAge());
            r.setDescription(recordDesc);
            r.setVisitDate(detail.getVisitDate() != null ? detail.getVisitDate() : LocalDate.now());
            consultMapper.insertRecord(r);
        } else {
            consultMapper.updateRecordDesc(registerId, recordDesc);
        }
        consultMapper.markInProgress(registerId);
    }

    @Override
    public void confirmRecord(String registerId, String recordDesc) {
        saveDraft(registerId, recordDesc);
        consultMapper.markRecordConfirmed(registerId);
    }

    @Override
    public void createExamOrder(String registerId, String checkItemList, String urgencyLevel) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) throw new BusinessException("就诊记录不存在");

        String reportId = "CHK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        consultMapper.insertCheckReport(reportId, detail.getPatientId(), registerId,
                detail.getDoctorId(), detail.getPatientName() != null ? detail.getPatientName() : detail.getName(),
                detail.getGender(), detail.getPatientAge(), "检查", checkItemList, BigDecimal.ZERO);
    }

    @Override
    public void completeConsult(String registerId) {
        consultMapper.completeConsult(registerId);
    }
}
