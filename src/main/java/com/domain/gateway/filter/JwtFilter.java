package com.domain.gateway.filter;

import com.domain.gateway.exception.classes.AuthenticationException;
import com.domain.gateway.jwt.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtFilter implements GlobalFilter, Ordered {
    private final JwtService jwtService;
    private final Environment environment;
    private final WebClient.Builder builder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            if (Objects.equals(environment.getProperty("env"), "dev")) {
                ServerHttpRequest mutatedRequest = request.mutate().header("X-Username", "sougata").header("X-role", "ROLE_ADMIN").build();
                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                return chain.filter(mutatedExchange);
            }

            if (request.getURI().getPath().contains("/auth")) {
                return chain.filter(exchange);
            }

            String token = null;

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null && request.getCookies().containsKey(HttpHeaders.AUTHORIZATION)) {
                token = Objects.requireNonNull(request.getCookies().getFirst(HttpHeaders.AUTHORIZATION))
                        .getValue();
            }

            if (token == null) {
                throw new AuthenticationException("JWT Token not found");
            }

            String username = jwtService.validateToken(token);


            return builder.build()
                    .get()
                    .uri("lb://auth-service/auth/verify-user/%s".formatted(username))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            clientResponse -> Mono.error(new RuntimeException("Invalid username in token")))
                    .bodyToMono(String.class)
                    .flatMap(response -> {

                        ServerHttpRequest mutatedRequest = request
                                .mutate()
                                .header("X-username", username)
                                .header("X-role", response)
                                .build();

                        ServerWebExchange mutatedExchange = exchange
                                .mutate()
                                .request(mutatedRequest)
                                .build();

                        return chain.filter(mutatedExchange);
                    });
        } catch (AuthenticationException | ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
