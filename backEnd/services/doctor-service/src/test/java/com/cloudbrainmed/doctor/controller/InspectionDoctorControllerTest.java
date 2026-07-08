package com.cloudbrainmed.doctor.controller;

import com.cloudbrainmed.common.result.Result;
import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmRequest;
import com.cloudbrainmed.doctor.dto.MedicalOrderConfirmResponse;
import com.cloudbrainmed.doctor.entity.MedicalOrder;
import com.cloudbrainmed.doctor.service.MedicalOrderService;
import com.cloudbrainmed.doctor.vo.InspectionOrderVo;
import com.cloudbrainmed.doctor.vo.MedicalReportVo;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InspectionDoctorControllerTest {

    private final StubMedicalOrderService medicalOrderService =
            new StubMedicalOrderService();
    private final InspectionDoctorController controller =
            new InspectionDoctorController(medicalOrderService);

    @Test
    void ordersAllowsExaminationDoctor() {
        String token = DoctorJwtUtil.createToken("D002", "11111111111", 2, 2);
        medicalOrderService.orders = List.of(order("EXAM"), order("LAB"));

        Result<?> result = assertDoesNotThrow(() -> controller.getAllLabOrders(token));

        assertThat(medicalOrderService.getAllLabOrdersCalled).isEqualTo(1);
        assertThat(castOrders(result))
                .extracting(InspectionOrderVo::getItemCategory)
                .containsExactly("EXAM");
    }

    @Test
    void ordersAllowsLabDoctor() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);
        medicalOrderService.orders = List.of(order("EXAM"), order("LAB"));

        Result<?> result = assertDoesNotThrow(() -> controller.getAllLabOrders(token));

        assertThat(medicalOrderService.getAllLabOrdersCalled).isEqualTo(1);
        assertThat(castOrders(result))
                .extracting(InspectionOrderVo::getItemCategory)
                .containsExactly("LAB");
    }

    @Test
    void ordersAnnotateAllowedActionsForCurrentDoctor() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);
        InspectionOrderVo queued = order("MO_LAB", "MOI_QUEUED", "LAB");
        queued.setStatus("QUEUED");
        InspectionOrderVo ownInProcess = order("MO_LAB", "MOI_OWN", "LAB");
        ownInProcess.setStatus("IN_PROCESS");
        ownInProcess.setAssignedDoctorId("D003");
        InspectionOrderVo otherInProcess = order("MO_LAB", "MOI_OTHER", "LAB");
        otherInProcess.setStatus("IN_PROCESS");
        otherInProcess.setAssignedDoctorId("D009");
        InspectionOrderVo completed = order("MO_LAB", "MOI_DONE", "LAB");
        completed.setStatus("COMPLETED");
        completed.setAssignedDoctorId("D009");
        medicalOrderService.orders = List.of(
                queued, ownInProcess, otherInProcess, completed);

        List<InspectionOrderVo> result = castOrders(controller.getAllLabOrders(token));

        assertThat(result)
                .extracting(InspectionOrderVo::getCanStart,
                        InspectionOrderVo::getCanWriteReport,
                        InspectionOrderVo::getCanViewReport)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(true, false, false),
                        org.assertj.core.groups.Tuple.tuple(false, true, false),
                        org.assertj.core.groups.Tuple.tuple(false, false, false),
                        org.assertj.core.groups.Tuple.tuple(false, false, true));
    }

    @Test
    void ordersRejectsReceptionDoctor() {
        String token = DoctorJwtUtil.createToken("D001", "11111111111", 2, 1);

        assertThrows(RuntimeException.class,
                () -> controller.getAllLabOrders(token));
        assertThat(medicalOrderService.getAllLabOrdersCalled).isZero();
    }

    @Test
    void doesNotExposeOrderLevelDetailOrAssignRoutes() {
        List<String> mappedPaths = new ArrayList<>();
        for (Method method : InspectionDoctorController.class.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                mappedPaths.addAll(List.of(getMapping.value()));
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                mappedPaths.addAll(List.of(postMapping.value()));
            }
        }

        assertThat(mappedPaths)
                .doesNotContain("/order/{orderId}",
                        "/order/{orderId}/assign");
    }

    @Test
    void detailRejectsMixedOrderWhenRequestedItemTypeDoesNotMatchDoctorType() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);
        medicalOrderService.orders = List.of(
                order("MO_MIXED", "MOI_EXAM", "EXAM"),
                order("MO_MIXED", "MOI_LAB", "LAB"));

        assertThrows(RuntimeException.class,
                () -> controller.getByOrderItemId(token, "MOI_EXAM"));
        assertThat(medicalOrderService.getByOrderItemIdCalled).isZero();
    }

    @Test
    void assignUsesVisibleOrderItemId() {
        String token = DoctorJwtUtil.createToken("D003", "11111111111", 2, 3);
        medicalOrderService.orders = List.of(
                order("MO_MIXED", "MOI_EXAM", "EXAM"),
                order("MO_MIXED", "MOI_LAB", "LAB"));

        assertDoesNotThrow(() -> controller.assignOrderItem(token, "MOI_LAB", null));
        assertThat(medicalOrderService.assignOrderCalled).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private List<InspectionOrderVo> castOrders(Result<?> result) {
        return (List<InspectionOrderVo>) result.getData();
    }

    private InspectionOrderVo order(String itemCategory) {
        return order("MO_" + itemCategory, itemCategory);
    }

    private InspectionOrderVo order(String orderId, String itemCategory) {
        return order(orderId, "MOI_" + itemCategory, itemCategory);
    }

    private InspectionOrderVo order(String orderId, String orderItemId, String itemCategory) {
        InspectionOrderVo order = new InspectionOrderVo();
        order.setOrderId(orderId);
        order.setOrderItemId(orderItemId);
        order.setItemCategory(itemCategory);
        return order;
    }

    private static class StubMedicalOrderService implements MedicalOrderService {
        private List<InspectionOrderVo> orders = List.of();
        private int getAllLabOrdersCalled;
        private int getByOrderItemIdCalled;
        private int assignOrderCalled;

        @Override
        public List<InspectionOrderVo> getAllLabOrders() {
            getAllLabOrdersCalled++;
            return orders;
        }

        @Override
        public MedicalOrder getByOrderItemId(String orderItemId) {
            getByOrderItemIdCalled++;
            MedicalOrder order = new MedicalOrder();
            order.setOrderId(orderItemId);
            return order;
        }

        @Override
        public MedicalOrderConfirmResponse confirm(MedicalOrderConfirmRequest request, String doctorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MedicalOrder assignOrderItem(String orderItemId, String assignedRoom) {
            assignOrderCalled++;
            MedicalOrder order = new MedicalOrder();
            order.setOrderId(orderItemId);
            order.setAssignedRoom(assignedRoom);
            return order;
        }

        @Override
        public List<MedicalReportVo> getPublishedReportsByRegisterId(String registerId) {
            throw new UnsupportedOperationException();
        }
    }
}
