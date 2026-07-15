package com.cloudbrainmed.payment.config;

import com.cloudbrainmed.payment.mapper.PayMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Ensures the prescription stock marker exists before payment requests are handled. */
@Component
public class PrescriptionStockSchemaInitializer implements CommandLineRunner {

    private final PayMapper payMapper;

    public PrescriptionStockSchemaInitializer(PayMapper payMapper) {
        this.payMapper = payMapper;
    }

    @Override
    public void run(String... args) {
        payMapper.ensurePrescriptionStockDeductedColumn();
    }
}
