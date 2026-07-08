package com.cloudbrainmed.doctor.service.impl;

import com.cloudbrainmed.common.exception.BusinessException;
import com.cloudbrainmed.doctor.entity.ConsultRecord;
import com.cloudbrainmed.doctor.mapper.ConsultMapper;

import java.time.LocalDate;

final class ConsultAccessGuard {

    private record EntryState(
            boolean allowed,
            boolean readOnly,
            String actionText,
            String blockedReason) {
    }

    private ConsultAccessGuard() {
    }

    static void applyListEntryState(ConsultRecord detail) {
        EntryState state = resolveEntryState(detail);
        detail.setEntryAllowed(state.allowed());
        detail.setReadOnly(state.readOnly());
        detail.setEntryActionText(state.actionText());
        detail.setEntryBlockedReason(state.blockedReason());
    }

    static ConsultRecord requireDetailAccess(
            ConsultMapper consultMapper, String registerId, String doctorId) {
        ConsultRecord detail = requireOwnedRecord(
                consultMapper, registerId, doctorId);
        if (isToday(detail)) {
            detail.setReadOnly(false);
            return detail;
        }
        if (isCompleted(detail) && !isFutureVisit(detail)) {
            detail.setReadOnly(true);
            return detail;
        }
        throw new BusinessException(nonSameDayMessage(detail));
    }

    static ConsultRecord requireActionAccess(
            ConsultMapper consultMapper, String registerId, String doctorId) {
        ConsultRecord detail = requireOwnedRecord(
                consultMapper, registerId, doctorId);
        if (isToday(detail)) {
            return detail;
        }
        throw new BusinessException(nonSameDayMessage(detail));
    }

    private static ConsultRecord requireOwnedRecord(
            ConsultMapper consultMapper, String registerId, String doctorId) {
        ConsultRecord detail = consultMapper.findDetail(registerId);
        if (detail == null) {
            throw new BusinessException("就诊记录不存在");
        }
        if (!doctorId.equals(detail.getDoctorId())) {
            throw new BusinessException("无权操作该接诊记录");
        }
        return detail;
    }

    private static boolean isToday(ConsultRecord detail) {
        if (detail.getVisitDate() == null) {
            return false;
        }
        return LocalDate.now().equals(detail.getVisitDate());
    }

    private static boolean isFutureVisit(ConsultRecord detail) {
        return detail.getVisitDate() != null
                && detail.getVisitDate().isAfter(LocalDate.now());
    }

    private static boolean isCompleted(ConsultRecord detail) {
        return "COMPLETED".equals(detail.getConsultStatus());
    }

    private static EntryState resolveEntryState(ConsultRecord detail) {
        if (isCompleted(detail)) {
            return new EntryState(
                    true,
                    true,
                    resolveActionText(detail),
                    null);
        }
        if (isToday(detail)) {
            return new EntryState(
                    true,
                    false,
                    resolveActionText(detail),
                    null);
        }
        return new EntryState(
                false,
                false,
                resolveActionText(detail),
                nonSameDayMessage(detail));
    }

    private static String resolveActionText(ConsultRecord detail) {
        return "COMPLETED".equals(detail.getConsultStatus()) ? "查看" : "接诊";
    }

    private static String nonSameDayMessage(ConsultRecord detail) {
        if (detail.getVisitDate() == null) {
            return "非就诊当天不可接诊：挂号缺少就诊日期";
        }
        if (detail.getVisitDate().isBefore(LocalDate.now())) {
            return "非就诊当天不可接诊：该挂号已过期";
        }
        return "非就诊当天不可接诊：未到就诊日期";
    }
}
