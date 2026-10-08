package com.strider.strider_gateway.filter;

import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import com.strider.strider_common_lib.response.StriderResponse;
import com.strider.strider_gateway.client.model.CheckAuthorizationResponseDto;
import com.strider.strider_gateway.client.ResponseParser;
import com.strider.strider_gateway.client.UserProfileClient;
import com.strider.strider_gateway.config.AuthProperties;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class TokenAuthFilter implements GatewayFilter {
    private final AuthProperties authProperties;
    private final UserProfileClient userProfileClient;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TokenAuthFilter(AuthProperties authProperties, UserProfileClient userProfileClient) {
        this.authProperties = authProperties;
        this.userProfileClient = userProfileClient;
    }

    private boolean pathMatches(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String requestId = exchange.getRequest().getHeaders().getFirst("request-id");

        if (!authProperties.isEnable() ||
                authProperties.getIgnores().stream()
                .map(p -> p.replaceAll("\\{[^/]+}", "*")) // 변수 패턴을 *로 변환
                .anyMatch(pattern -> pathMatches(pattern, path))) {
            log.info("[GW-AUTH] [Request-ID: {}] Ignored path matched. Skipping auth for path: {}", requestId, path);
            return chain.filter(exchange);
        }

        List<String> authHeaders = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
        if (authHeaders.isEmpty() || !authHeaders.getFirst().startsWith("Bearer ")) {
            log.error("[GW-AUTH] [Request-ID: {}] Authorization 헤더가 없거나 형식이 잘못되었습니다.", requestId);
            throw new StriderException(StriderErrorCodes.UNAUTHORIZED, "Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        }

        String token = authHeaders.getFirst();

        return userProfileClient.checkAuth(token)
                .flatMap(parsedResponse -> {
                    if (parsedResponse.getMeta().getCode() == 2000) {
                        log.info("[GW-AUTH] [Request-ID: {}] 인증 성공", requestId);
                        return chain.filter(exchange);
                    }

                    log.error("[GW-AUTH] [Request-ID: {}] 인증 실패: actualCode={}",
                            requestId, parsedResponse.getMeta().getCode());

                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("[GW-AUTH] [Request-ID: {}] WebClientResponseException 발생: status={}, body={}",
                            requestId, e.getStatusCode(), e.getResponseBodyAsString(), e);

                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("[GW-AUTH] [Request-ID: {}] 기타 예외 발생", requestId, e);

                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }
}
