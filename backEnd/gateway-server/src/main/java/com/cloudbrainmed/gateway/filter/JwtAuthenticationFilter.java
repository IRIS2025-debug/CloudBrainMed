package com.cloudbrainmed.gateway.filter;

import com.cloudbrainmed.common.utils.DoctorJwtUtil;
import com.cloudbrainmed.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtUtil patientJwtUtil;

    public JwtAuthenticationFilter(JwtUtil patientJwtUtil) {
        this.patientJwtUtil = patientJwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        if (isInternalPath(path)) {
            return reject(exchange, HttpStatus.NOT_FOUND);
        }

        String token = extractToken(exchange);
        if (!hasText(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            if (isAdminPath(path)) {
                Claims claims = DoctorJwtUtil.parseToken(token);
                Integer roleType = numberClaim(claims, "roleType");
                if (!Integer.valueOf(3).equals(roleType)) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }
                return continueWithClaims(exchange, chain, claims, roleType, token);
            }
            if (isDoctorPath(path)) {
                Claims claims = DoctorJwtUtil.parseToken(token);
                Integer roleType = numberClaim(claims, "roleType");
                if (!Integer.valueOf(2).equals(roleType)) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }
                return continueWithClaims(exchange, chain, claims, roleType, token);
            }
            if (isAiPath(path)) {
                Claims claims = DoctorJwtUtil.parseToken(token);
                Integer roleType = numberClaim(claims, "roleType");
                if (!Integer.valueOf(2).equals(roleType)
                        && !Integer.valueOf(3).equals(roleType)) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }
                return continueWithClaims(exchange, chain, claims, roleType, token);
            }
            if (isPaymentPath(path) || isPatientPath(path)) {
                if (patientJwtUtil == null) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED);
                }
                Claims claims = patientJwtUtil.getClaimsFromToken(token);
                ServerHttpRequest.Builder request = exchange.getRequest().mutate()
                        .header("X-Patient-Id", claims.getSubject());
                request.headers(headers -> headers.set("token", token));
                return chain.filter(exchange.mutate()
                        .request(request.build())
                        .build());
            }
        } catch (Exception exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        return reject(exchange, HttpStatus.FORBIDDEN);
    }

    private Mono<Void> continueWithClaims(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Claims claims,
            Integer roleType,
            String token) {
        ServerHttpRequest.Builder request = exchange.getRequest().mutate()
                .header("X-User-Id", claims.get("userId", String.class))
                .header("X-Role-Type", String.valueOf(roleType));
        request.headers(headers -> headers.set("token", token));
        Integer doctorType = numberClaim(claims, "doctorType");
        if (doctorType != null) {
            request.header("X-Doctor-Type", String.valueOf(doctorType));
        }
        return chain.filter(exchange.mutate().request(request.build()).build());
    }

    private Integer numberClaim(Claims claims, String key) {
        Number value = claims.get(key, Number.class);
        return value == null ? null : value.intValue();
    }

    private String extractToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst("token");
        if (hasText(token)) {
            return token.trim();
        }
        String authorization = exchange.getRequest().getHeaders()
                .getFirst("Authorization");
        if (hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth-service/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/files/avatar/");
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/internal/")
                || path.startsWith("/doctor-service/internal/");
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin-service/")
                || path.startsWith("/api/admin/");
    }

    private boolean isDoctorPath(String path) {
        return path.startsWith("/doctor-service/")
                || path.startsWith("/inspection-doctor/");
    }

    private boolean isPatientPath(String path) {
        return path.startsWith("/api/patient/")
                || path.startsWith("/patient-service/");
    }

    private boolean isPaymentPath(String path) {
        return path.startsWith("/payment-service/")
                || path.startsWith("/api/payment/");
    }

    private boolean isAiPath(String path) {
        return path.startsWith("/ai-service/");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
