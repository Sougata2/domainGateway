package com.domain.gateway.filter;

import com.domain.gateway.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtFilter implements GlobalFilter, Ordered {
    private final JwtService jwtService;
    private final Environment environment;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            System.out.println("JwtFilter");
            ServerHttpRequest request = exchange.getRequest();
            if (Objects.equals(environment.getProperty("env"), "dev")) {
                ServerHttpRequest mutatedRequest = request.mutate().header("X-Username", "sougata").header("X-role", "ROLE_ADMIN").build();
                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                return chain.filter(mutatedExchange);
            }

            if (request.getURI().getPath().contains("/auth")){
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                throw new RuntimeException("Authorization Header Not Found");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                throw new RuntimeException("JWT Token Not Found");
            }


            String token = authHeader.substring(7);
            String username = jwtService.validateToken(token);
            ServerHttpRequest mutatedRequest = request.mutate().header("X-Username", username).build();
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
