package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.doctor.mapper.DoctorSkillMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper;
import com.cloudbrainmed.doctor.mapper.MedicalOrderMapper.QueuedTaskItem;
import com.cloudbrainmed.doctor.service.AgingService;
import com.cloudbrainmed.doctor.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class QueueServiceImpl implements QueueService {

    private final MedicalOrderMapper medicalOrderMapper;
    private final DoctorSkillMapper doctorSkillMapper;
    private final AgingService agingService;

    public QueueServiceImpl(
            MedicalOrderMapper medicalOrderMapper,
            DoctorSkillMapper doctorSkillMapper,
            AgingService agingService) {
        this.medicalOrderMapper = medicalOrderMapper;
        this.doctorSkillMapper = doctorSkillMapper;
        this.agingService = agingService;
    }

    @Override
    public List<QueuedTaskItem> getQueuedTasks(int limit) {
        List<QueuedTaskItem> rawTasks = medicalOrderMapper.findQueuedTasks(limit);
        return agingService.sortWithAging(rawTasks);
    }

    @Override
    public long getQueueCount() {
        return medicalOrderMapper.countQueuedTasks();
    }

    @Override
    public String findAvailableDoctor(QueuedTaskItem task, Set<String> busyDoctors) {
        Integer doctorType = "EXAM".equals(task.getItemCategory()) ? 2 :
                "LAB".equals(task.getItemCategory()) ? 3 : null;
        List<DoctorSkillMapper.DoctorSkillMatch> matches =
                doctorSkillMapper.findMatchingDoctorsByItemCode(task.getItemCode(), doctorType);
        if (matches == null || matches.isEmpty()) {
            log.warn("No doctor with skill for itemCode={} itemCategory={}",
                    task.getItemCode(), task.getItemCategory());
            return null;
        }
        for (DoctorSkillMapper.DoctorSkillMatch match : matches) {
            if (busyDoctors.contains(match.getDoctorId())) {
                continue;
            }
            if (medicalOrderMapper.countInProgressByDoctor(match.getDoctorId()) > 0) {
                busyDoctors.add(match.getDoctorId());
                continue;
            }
            return match.getDoctorId();
        }
        return null;
    }

    @Override
    public String itemCategoryForDoctorType(Integer doctorType) {
        if (Integer.valueOf(2).equals(doctorType)) {
            return "EXAM";
        }
        if (Integer.valueOf(3).equals(doctorType)) {
            return "LAB";
        }
        return null;
    }
}
