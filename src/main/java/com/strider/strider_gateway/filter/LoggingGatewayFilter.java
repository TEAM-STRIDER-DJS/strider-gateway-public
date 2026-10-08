package com.strider.strider_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2)
public class LoggingGatewayFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().toString();
        String query = exchange.getRequest().getQueryParams().toString();
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null) {
            ip = exchange.getRequest().getRemoteAddress() != null ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        String headers = exchange.getRequest().getHeaders().toString();

        String requestId = exchange.getRequest().getHeaders().getFirst("request-id");
        if (requestId == null) {
            requestId = "unknown";
        }

        log.info("[GW-LOG] [Request-ID: {}] IP: {}, Method: {}, URL: {}?{},\n Headers: {}", requestId, ip, method, path, query, headers);
        return chain.filter(exchange);
    }
}
