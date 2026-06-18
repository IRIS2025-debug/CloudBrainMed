package com.cloudbrainmed.patient.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.patient.dto.RegisterSubmitDto;
import com.cloudbrainmed.patient.entity.Dept;
import com.cloudbrainmed.patient.entity.Doctor;
import com.cloudbrainmed.patient.entity.Registration;
import com.cloudbrainmed.patient.service.RegisterService;
import com.cloudbrainmed.patient.vo.DoctorDetailVo;
import com.cloudbrainmed.patient.vo.ScheduleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patient-service/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @GetMapping("/depts")
    public Result<List<Dept>> getAllDepts() {
        return Result.success(registerService.getAllDepts());
    }

    @GetMapping("/doctors/{deptId}")
    public Result<List<Doctor>> getDoctorsByDept(@PathVariable String deptId) {
        return Result.success(registerService.getDoctorsByDept(deptId));
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<DoctorDetailVo> getDoctorDetail(@PathVariable String doctorId) {
        return Result.success(registerService.getDoctorDetail(doctorId));
    }

    @GetMapping("/schedules/{doctorId}")
    public Result<List<ScheduleVo>> getDoctorSchedules(@PathVariable String doctorId) {
        return Result.success(registerService.getDoctorSchedules(doctorId));
    }

    @PostMapping("/submit")
    public Result<Registration> submitRegister(@RequestBody RegisterSubmitDto dto) {
        return Result.success(registerService.submitRegister(dto));
    }

    @GetMapping("/history/{patientId}")
    public Result<List<Registration>> getRegisterHistory(@PathVariable String patientId) {
        return Result.success(registerService.getRegisterHistory(patientId));
    }

    @GetMapping("/detail/{registerId}")
    public Result<Registration> getRegisterDetail(@PathVariable String registerId) {
        return Result.success(registerService.getRegisterDetail(registerId));
    }

    /**
     * 检查患者信息是否完整
     */
    @GetMapping("/check-patient/{patientId}")
    public Result<Map<String, Object>> checkPatientInfo(@PathVariable String patientId) {
        return Result.success(registerService.checkPatientInfo(patientId));
    }

}