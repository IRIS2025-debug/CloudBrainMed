package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.ConsultRecord;

import java.util.List;
import java.util.Map;

public interface ConsultService {
    List<ConsultRecord> getList(String doctorId, String consultStatus, String date, boolean reportReturnedOnly, int page, int limit);

    /**
     * 接诊医生首页概览：仅统计传入 doctorId 当天数据。
     * 返回 todayTotal / pendingCount / inProgressCount / completedTodayCount / recentConsults。
     */
    Map<String, Object> getOverview(String doctorId);
    ConsultRecord getDetail(String doctorId, String registerId);
    void saveDraft(String doctorId, String registerId, String recordDesc);
    void confirmRecord(String doctorId, String registerId, String recordDesc);
    void createExamOrder(String doctorId, String registerId, String checkItemList, String urgencyLevel);
    void completeConsult(String doctorId, String registerId);
}
