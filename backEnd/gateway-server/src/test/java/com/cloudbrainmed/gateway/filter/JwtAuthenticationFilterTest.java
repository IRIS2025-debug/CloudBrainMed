package com.cloudbrainmed.gateway.filter;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.common.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(null);
    private final JwtUtil patientJwtUtil = patientJwtUtil();

    @Test
    void allowsAuthPathsWithoutToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/auth-service/patient/login", null);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void hidesInternalPathsFromGateway() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/internal/doctor/consult/context", null);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsAdminPathWithoutAdminToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D001", "13900000001", 2, 1);
        MockServerWebExchange exchange = exchange("/admin-service/account/list", doctorToken);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void allowsAdminPathWithAdminTokenAndPropagatesIdentity() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String adminToken = DoctorJwtUtil.createToken("A001", "13700000001", 3);
        MockServerWebExchange exchange = exchange("/admin-service/account/list", adminToken);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("A001");
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Role-Type"))
                .isEqualTo("3");
    }

    @Test
    void forwardsLegacyTokenHeaderWhenRequestUsesBearerAuthorization() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D002", "13900000002", 2, 1);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/doctor-service/consult/list")
                        .header("Authorization", "Bearer " + doctorToken)
                        .build());

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("token"))
                .isEqualTo(doctorToken);
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("D002");
    }

    @Test
    void authenticatedDoctorIdentityHeadersOverrideClientSuppliedValues() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D002", "13900000002", 2, 1);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/doctor-service/consult/list")
                        .header("token", doctorToken)
                        .header("X-User-Id", "ATTACKER")
                        .header("X-Role-Type", "3")
                        .header("X-Doctor-Type", "3")
                        .build());

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("D002");
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Role-Type"))
                .isEqualTo("2");
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Doctor-Type"))
                .isEqualTo("1");
    }

    @Test
    void rejectsDoctorCtAssetPathWithQueryToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D002", "13900000002", 2, 2);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/doctor-service/exam/ct-lesion/preview/scan_preview.png")
                        .queryParam("token", doctorToken)
                        .build());

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsDoctorCtAssetPathWithHeaderToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D002", "13900000002", 2, 2);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/doctor-service/exam/ct-lesion/preview/scan_preview.png")
                        .header("token", doctorToken)
                        .build());

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("D002");
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Doctor-Type"))
                .isEqualTo("2");
    }

    @Test
    void rejectsQueryTokenOnNonAssetDoctorApi() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D002", "13900000002", 2, 1);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/doctor-service/consult/list")
                        .queryParam("token", doctorToken)
                        .build());

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsPaymentPathWithInvalidToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(
                "/payment-service/pay/success/pay001", "not-a-jwt");

        new JwtAuthenticationFilter(patientJwtUtil)
                .filter(exchange, chain(chained))
                .block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsPaymentPathWithPatientTokenAndPropagatesIdentity() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String patientToken = patientJwtUtil.generateToken("P001", "13800000001");
        MockServerWebExchange exchange = exchange(
                "/api/payment/pay/history/P001", patientToken);

        new JwtAuthenticationFilter(patientJwtUtil)
                .filter(exchange, chain(chained))
                .block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Patient-Id"))
                .isEqualTo("P001");
    }

    @Test
    void authenticatedPatientIdentityHeaderOverridesClientSuppliedValue() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String patientToken = patientJwtUtil.generateToken("P001", "13800000001");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payment/pay/history/P001")
                        .header("token", patientToken)
                        .header("X-Patient-Id", "ATTACKER")
                        .build());

        new JwtAuthenticationFilter(patientJwtUtil)
                .filter(exchange, chain(chained))
                .block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Patient-Id"))
                .isEqualTo("P001");
    }

    @Test
    void rejectsAiPathWithInvalidToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(
                "/ai-service/report/analyze", "not-a-jwt");

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsAiPathWithDoctorTokenAndPropagatesIdentity() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D003", "13900000003", 2, 1);
        MockServerWebExchange exchange = exchange(
                "/ai-service/report/analyze", doctorToken);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo("D003");
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Role-Type"))
                .isEqualTo("2");
    }

    @Test
    void rejectsUnclassifiedProtectedPathEvenWithToken() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String doctorToken = DoctorJwtUtil.createToken("D004", "13900000004", 2, 1);
        MockServerWebExchange exchange = exchange("/service-api/anything", doctorToken);

        filter.filter(exchange, chain(chained)).block();

        assertThat(chained).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private MockServerWebExchange exchange(String path, String token) {
        MockServerHttpRequest.BaseBuilder<?> request =
                MockServerHttpRequest.get(path);
        if (token != null) {
            request.header("token", token);
        }
        return MockServerWebExchange.from(request);
    }

    private GatewayFilterChain chain(AtomicBoolean chained) {
        return new GatewayFilterChain() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange) {
                chained.set(true);
                return Mono.empty();
            }
        };
    }

    private JwtUtil patientJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "cloudbrainmedSecretKey2024SecureLongKeyForJWT");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);
        return jwtUtil;
    }
}
