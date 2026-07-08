package com.cloudbrainmed.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void usesRemoteAddressInsteadOfForwardedForForRateLimitKey() {
        RateLimitFilter filter = new RateLimitFilter(
                1,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        AtomicInteger chained = new AtomicInteger();

        MockServerWebExchange first = exchange("203.0.113.1");
        filter.filter(first, chain(chained)).block();

        MockServerWebExchange second = exchange("203.0.113.2");
        filter.filter(second, chain(chained)).block();

        assertThat(chained).hasValue(1);
        assertThat(first.getResponse().getStatusCode()).isNull();
        assertThat(second.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void removesStaleClientCountersWhenMinuteAdvances() throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-01-01T00:00:00Z"));
        RateLimitFilter filter = new RateLimitFilter(10, clock);
        AtomicInteger chained = new AtomicInteger();

        filter.filter(exchange("203.0.113.1", "10.0.0.1"), chain(chained)).block();
        filter.filter(exchange("203.0.113.2", "10.0.0.2"), chain(chained)).block();
        assertThat(counterSize(filter)).isEqualTo(2);

        clock.setInstant(Instant.parse("2026-01-01T00:01:00Z"));
        filter.filter(exchange("203.0.113.3", "10.0.0.3"), chain(chained)).block();

        assertThat(counterSize(filter)).isEqualTo(1);
    }

    private MockServerWebExchange exchange(String forwardedFor) {
        return exchange(forwardedFor, "10.0.0.1");
    }

    private MockServerWebExchange exchange(String forwardedFor, String remoteAddress) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/doctor-service/consult/list")
                        .remoteAddress(new InetSocketAddress(remoteAddress, 12345))
                        .header("X-Forwarded-For", forwardedFor)
                        .build());
    }

    private int counterSize(RateLimitFilter filter) throws Exception {
        Field field = RateLimitFilter.class.getDeclaredField("counters");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(filter)).size();
    }

    private GatewayFilterChain chain(AtomicInteger chained) {
        return new GatewayFilterChain() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange) {
                chained.incrementAndGet();
                return Mono.empty();
            }
        };
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
