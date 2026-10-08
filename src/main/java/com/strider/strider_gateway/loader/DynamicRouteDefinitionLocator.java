package com.strider.strider_gateway.loader;

import com.strider.strider_gateway.config.RoutingConfigProperties;
import com.strider.strider_gateway.config.ServiceConfig;
import com.strider.strider_gateway.filter.TokenAuthGatewayFilterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DynamicRouteDefinitionLocator {

    private final RouteDefinitionLoader routeDefinitionLoader;
    private final ServiceConfig serviceConfig;
    private final TokenAuthGatewayFilterFactory tokenAuthGatewayFilterFactory;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        List<RoutingConfigProperties.RouteDefinition> definitions = routeDefinitionLoader.loadRoutes();

        for (RoutingConfigProperties.RouteDefinition def : definitions) {
            String rawUrl = serviceConfig.getServices().get(def.getService());
            if (rawUrl == null) {
                log.warn("Service '{}' not found in application.yml", def.getService());
                continue;
            }

            // WS 라우트는 스킴을 ws/wss로 승격해야 WebsocketRoutingFilter가 프레임을 프록시한다.
            // (raw WebSocket만 사용하고 SockJS 평문 폴백을 쓰지 않으므로 안전)
            final String baseUrl = def.isWebsocket()
                    ? rawUrl.replaceFirst("^http://", "ws://").replaceFirst("^https://", "wss://")
                    : rawUrl;

            // // WS 라우트도 http 스킴을 그대로 둔다.
            // // 스킴을 ws/wss로 바꾸면 WebsocketRoutingFilter가 Upgrade 헤더 유무와 무관하게
            // // 모든 요청을 핸드셰이크로 처리해서, 평문 GET(SockJS /info 등)이 400/500으로 떨어진다.
            // // http로 두면 Upgrade: websocket이 있을 때만 SCG가 자동으로 ws로 승격시킨다.
            // final String baseUrl = rawUrl;

            String inbound = def.getInbound();
            String outbound = def.getOutbound();

            if (inbound == null || inbound.isBlank()) {
                log.warn("Inbound path is null or blank for route '{}'", def.getId());
                continue;
            }

            String pathPattern = inbound.replaceAll("\\{[^/]+}", "*");

            final String rewriteFrom;
            final String replacement;
            if (inbound.endsWith("/**")) {
                // 와일드카드 경로: /ws-chat/** → 하위 경로 전체를 그대로 전달
                String inboundBase = inbound.substring(0, inbound.length() - 3);
                String outboundBase = outbound.endsWith("/**")
                        ? outbound.substring(0, outbound.length() - 3)
                        : outbound;
                rewriteFrom = inboundBase + "/(?<wcsegment>.*)";
                replacement = outboundBase + "/${wcsegment}";
            } else {
                rewriteFrom = inbound.replaceAll("\\{([^/}]+)}", "(?<$1>[^/]+)");
                replacement = outbound.replaceAll("\\{([^/}]+)}", "\\${$1}");
            }

            routes.route(def.getId(), r -> r
                    .path(pathPattern)
                    .filters(f -> {
                        if(!def.isWebsocket()) { // WS는 rewrite 스킵, 토큰 인증 스킵 (인증 책임은 chat service)
                            f.filter(tokenAuthGatewayFilterFactory.apply(new TokenAuthGatewayFilterFactory.Config()));
                            f.rewritePath(rewriteFrom, replacement);
                        }
                        return f;
                    })
                    .uri(baseUrl)
            );

            log.info("✅ Route registered: [{}] {} -> {} via {}", def.getId(), inbound, outbound, baseUrl);
        }

        return routes.build();
    }
}