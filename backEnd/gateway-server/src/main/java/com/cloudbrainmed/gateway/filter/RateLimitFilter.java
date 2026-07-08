package com.cloudbrainmed.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter implements GlobalFilter {

    private final int requestsPerMinute;
    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> counters =
            new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMinute = new AtomicLong(Long.MIN_VALUE);

    public RateLimitFilter(
            @Value("${cloudbrainmed.gateway.rate-limit.requests-per-minute:120}")
            int requestsPerMinute) {
        this(requestsPerMinute, Clock.systemUTC());
    }

    RateLimitFilter(int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = clientIp(exchange);
        long minute = clock.millis() / 60_000;
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new WindowCounter(minute);
            }
            return existing;
        });
        cleanupStaleCounters(minute);
        if (counter.count.incrementAndGet() > requestsPerMinute) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private void cleanupStaleCounters(long minute) {
        long previousCleanupMinute = lastCleanupMinute.get();
        if (previousCleanupMinute == minute
                || !lastCleanupMinute.compareAndSet(previousCleanupMinute, minute)) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().minute < minute);
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        if (address != null && address.getAddress() != null) {
            return address.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private static final class WindowCounter {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long minute) {
            this.minute = minute;
        }
    }
}
