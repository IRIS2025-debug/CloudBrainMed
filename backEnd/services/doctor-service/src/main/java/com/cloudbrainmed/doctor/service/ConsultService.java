package com.cloudbrainmed.doctor.service;

import com.cloudbrainmed.doctor.entity.ConsultRecord;

import java.util.List;

public interface ConsultService {
    List<ConsultRecord> getList(String doctorId, String consultStatus, String date, int page, int limit);
    ConsultRecord getDetail(String doctorId, String registerId);
    void saveDraft(String registerId, String recordDesc);
    void confirmRecord(String registerId, String recordDesc);
    void createExamOrder(String registerId, String checkItemList, String urgencyLevel);
    void completeConsult(String registerId, String doctorId);
}