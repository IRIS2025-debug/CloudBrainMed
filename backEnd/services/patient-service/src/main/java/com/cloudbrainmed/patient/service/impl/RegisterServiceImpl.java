package com.cloudbrainmed.patient.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.patient.dto.RegisterSubmitDto;
import com.cloudbrainmed.patient.entity.*;
import com.cloudbrainmed.patient.mapper.*;
import com.cloudbrainmed.patient.service.RegisterService;
import com.cloudbrainmed.patient.vo.DoctorDetailVo;
import com.cloudbrainmed.patient.vo.ScheduleVo;
import com.cloudbrainmed.patient.vo.VisitDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final DeptMapper deptMapper;
    private final DoctorMapper doctorMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final PatientMapper patientMapper;
    private final RegistrationMapper registrationMapper;
    private final RegisterReportMapper registerReportMapper;
    private final MedicalOrderMapper medicalOrderMapper;
    private final MedicalOrderItemMapper medicalOrderItemMapper;
    private final PrescriptionMapper prescriptionMapper;


    @Override
    public List<Dept> getAllDepts() {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getStatus, 1);
        wrapper.orderByAsc(Dept::getCreateTime);
        return deptMapper.selectList(wrapper);
    }

    @Override
    public List<Doctor> getDoctorsByDept(String deptId) {
        return doctorMapper.selectByDeptId(deptId);
    }

    @Override
    public DoctorDetailVo getDoctorDetail(String doctorId) {
        Doctor doctor = doctorMapper.selectByDoctorId(doctorId);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }

        Dept dept = deptMapper.selectById(doctor.getDepartmentId());

        List<ScheduleVo> schedules = getDoctorSchedules(doctorId);

        DoctorDetailVo vo = new DoctorDetailVo();
        vo.setDoctorId(doctor.getDoctorId());
        vo.setName(doctor.getName());
        vo.setPosition(doctor.getPosition());
        vo.setGoodAt(doctor.getGoodAt());
        vo.setIntroduction(doctor.getIntroduction());
        vo.setAvatar(doctor.getAvatar());
        vo.setDeptName(dept != null ? dept.getDeptName() : "");
        vo.setSchedules(schedules);
        return vo;
    }

    @Override
    public List<ScheduleVo> getDoctorSchedules(String doctorId) {
        List<DoctorSchedule> schedules = doctorScheduleMapper.selectByDoctorId(doctorId);
        return schedules.stream().map(s -> {
            ScheduleVo vo = new ScheduleVo();
            vo.setScheduleId(s.getScheduleId());
            vo.setDoctorId(s.getDoctorId());
            vo.setDoctorName(s.getDoctorName());
            vo.setWorkDate(s.getWorkDate());
            vo.setStartTime(s.getStartTime());
            vo.setEndTime(s.getEndTime());
            vo.setMaxNum(s.getMaxNum());
            vo.setRemainNum(s.getRemainNum());
            vo.setStatus(s.getStatus());
            vo.setPrice(s.getPrice());
            vo.setRoom(s.getRoom());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Registration submitRegister(RegisterSubmitDto dto) {
        // 1. 校验患者是否存在
        Patient patient = patientMapper.selectById(dto.getPatientId());
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }

        // 2. 校验患者信息是否完整
        validatePatientInfo(patient);

        // 3. 校验医生是否存在
        Doctor doctor = doctorMapper.selectByDoctorId(dto.getDoctorId());
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }

        // 4. 获取科室信息
        Dept dept = deptMapper.selectById(doctor.getDepartmentId());
        if (dept == null) {
            throw new BusinessException("科室不存在");
        }

        // 5. 校验排班是否存在且有余号
        DoctorSchedule schedule = doctorScheduleMapper.selectByScheduleId(dto.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        if (schedule.getRemainNum() <= 0) {
            throw new BusinessException("号源已满");
        }

        // 6. 扣减剩余号源
        int updated = doctorScheduleMapper.decrementRemainNum(dto.getScheduleId());
        if (updated == 0) {
            throw new BusinessException("扣减号源失败，请重试");
        }

        // 7. 创建挂号记录
        Registration registration = new Registration();
        registration.setRegisterId(generateRegisterId());
        registration.setPatientId(dto.getPatientId());
        registration.setDoctorId(dto.getDoctorId());
        registration.setName(patient.getName());
        registration.setGender(patient.getGender());
        registration.setBirthday(patient.getBirthday());
        registration.setChiefComplaint(dto.getChiefComplaint());
        registration.setDepartment(dept.getDeptName());
        registration.setConsultRoom(schedule.getRoom());
        registration.setVisitDate(dto.getVisitDate());
        registration.setConsultTime(dto.getConsultTime());
        registration.setPrice(schedule.getPrice());
        registration.setPayStatus("WAITING");
        registration.setCreateTime(OffsetDateTime.now(ZoneOffset.ofHours(8)));

        registrationMapper.insert(registration);

        return registration;
    }

    @Override
    public List<Registration> getRegisterHistory(String patientId) {
        return registrationMapper.selectByPatientId(patientId);
    }

    @Override
    public Registration getRegisterDetail(String registerId) {
        return registrationMapper.selectByRegisterId(registerId);
    }

    @Override
    public VisitDetail getVisitDetail(String registerId) {
        VisitDetail detail = new VisitDetail();

        // 1. 获取挂号信息
        Registration registration = registrationMapper.selectByRegisterId(registerId);
        detail.setRegister(registration);

        // 2. 获取病历报告
        RegisterReport report = registerReportMapper.selectByRegisterId(registerId);
        detail.setReport(report);

        // 3. 获取检查检验订单
        List<MedicalOrder> orders = medicalOrderMapper.selectByRegisterId(registerId);
        detail.setOrders(orders);

        // 4. 获取检查检验明细（通过 registerId 直接查询）
        List<MedicalOrderItem> orderItems = medicalOrderItemMapper.selectByRegisterId(registerId);
        detail.setOrderItems(orderItems);

        // 5. 获取处方项
        if (registration != null) {
            List<Prescription> prescriptions = prescriptionMapper.selectByRegisterId(registerId, registration.getPatientId());
            detail.setPrescriptions(prescriptions);
        }

        return detail;
    }

    @Override
    public Map<String, Object> checkPatientInfo(String patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new BusinessException("患者不存在");
        }

        Map<String, Object> result = new HashMap<>();
        StringBuilder missingFields = new StringBuilder();
        boolean isComplete = true;

        if (patient.getName() == null || patient.getName().trim().isEmpty()) {
            missingFields.append("姓名、");
            isComplete = false;
        }
        if (patient.getGender() == null) {
            missingFields.append("性别、");
            isComplete = false;
        }
        if (patient.getBirthday() == null) {
            missingFields.append("生日、");
            isComplete = false;
        }
        if (patient.getIdCard() == null || patient.getIdCard().trim().isEmpty()) {
            missingFields.append("身份证号、");
            isComplete = false;
        }
        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            missingFields.append("手机号、");
            isComplete = false;
        }

        result.put("isComplete", isComplete);
        if (!isComplete) {
            String fields = missingFields.substring(0, missingFields.length() - 1);
            result.put("missingFields", fields);
        } else {
            result.put("missingFields", "");
        }

        return result;
    }

    /**
     * 查询挂号的支付状态
     */
    @Override
    public String getPayStatus(String registerId) {
        if (registerId == null) {
            return "UNKNOWN";
        }
        Registration registration = registrationMapper.selectByRegisterId(registerId);
        if (registration == null) {
            return "UNKNOWN";
        }
        return registration.getPayStatus() != null ? registration.getPayStatus() : "WAITING";
    }


    private String generateRegisterId() {
        String latestId = registrationMapper.getLastRegisterId();
        int nextNum = 1;
        if (latestId != null && latestId.startsWith("reg")) {
            try {
                String numStr = latestId.substring(3);
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }
        return String.format("reg%03d", nextNum);
    }

    private void validatePatientInfo(Patient patient) {
        StringBuilder missingFields = new StringBuilder();
        if (patient.getName() == null || patient.getName().trim().isEmpty()) {
            missingFields.append("姓名、");
        }
        if (patient.getGender() == null) {
            missingFields.append("性别、");
        }
        if (patient.getBirthday() == null) {
            missingFields.append("生日、");
        }
        if (patient.getIdCard() == null || patient.getIdCard().trim().isEmpty()) {
            missingFields.append("身份证号、");
        }
        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) {
            missingFields.append("手机号、");
        }
        if (missingFields.length() > 0) {
            String fields = missingFields.substring(0, missingFields.length() - 1);
            throw new BusinessException("患者信息不完整，请先完善以下信息：" + fields);
        }
    }
}