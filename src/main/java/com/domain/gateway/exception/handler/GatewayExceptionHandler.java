package com.domain.gateway.exception.handler;

import com.domain.gateway.exception.classes.AuthenticationException;
import com.domain.gateway.exception.dto.ErrorDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;

@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ErrorDto errorDto = new ErrorDto();
        if (ex instanceof ExpiredJwtException || ex instanceof AuthenticationException) {
            errorDto.setMessage(ex.getMessage());
            errorDto.setPath("[%s] : %s".formatted(exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath()));
            errorDto.setStatus(HttpStatus.UNAUTHORIZED);
            errorDto.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());

            try {
                byte[] responseBytes = new ObjectMapper().writeValueAsBytes(errorDto);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else if (ex instanceof Exception) {
            errorDto.setMessage(ex.getMessage());
            errorDto.setPath("[%s] : %s".formatted(exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath()));
            errorDto.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            errorDto.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());

            try {
                byte[] responseBytes = new ObjectMapper().writeValueAsBytes(errorDto);
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return Mono.error(ex);
    }
}
