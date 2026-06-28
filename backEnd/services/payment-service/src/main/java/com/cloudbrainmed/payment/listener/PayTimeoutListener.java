package com.cloudbrainmed.payment.listener;

import com.cloudbrainmed.payment.service.PayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PayTimeoutListener {

    private static final Logger logger = LoggerFactory.getLogger(PayTimeoutListener.class);

    private final PayService payService;

    public PayTimeoutListener(PayService payService) {
        this.payService = payService;
    }

    /**
     * 监听支付超时队列，自动取消未支付订单
     */
    @RabbitListener(queues = "pay_timeout_queue")
    public void handlePayTimeout(String payId) {
        logger.info("收到支付超时消息，payId: {}", payId);
        try {
            boolean result = payService.cancelPay(payId);
            if (result) {
                logger.info("支付订单已取消，payId: {}", payId);
            } else {
                logger.warn("取消支付订单失败，payId: {}", payId);
            }
        } catch (Exception e) {
            logger.error("取消支付订单异常，payId: {}", payId, e);
        }
    }
}