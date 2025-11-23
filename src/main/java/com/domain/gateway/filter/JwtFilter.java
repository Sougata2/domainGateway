package com.domain.gateway.filter;

import com.domain.gateway.jwt.service.JwtService;
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


            return builder.build()
                    .get()
                    .uri("lb://auth-service/user/get-default-role/%s".formatted(username))
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
