package com.strider.strider_gateway.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import com.strider.strider_gateway.config.RoutingConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RouteDefinitionLoader {

    private static final String ROUTE_PATH = "classpath:routes/routes_*.yml";
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public List<RoutingConfigProperties.RouteDefinition> loadRoutes() {
        List<RoutingConfigProperties.RouteDefinition> allRoutes = new ArrayList<>();

        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(ROUTE_PATH);
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    RoutingConfigProperties props = yamlMapper.readValue(is, RoutingConfigProperties.class);
                    List<RoutingConfigProperties.RouteDefinition> routes = props.getRoutes();
                    if (routes != null) {
                        String service = routes.getFirst().getService();
                        routes.removeFirst();
                        routes.forEach(
                                route -> {
                                    log.info("Route ID: {}, service: {}, inbound: {}, outbound: {}",
                                            route.getId(), route.getService(), route.getInbound(), route.getOutbound());
                                    route.setService(service);
                                    if (route.getInbound() != null && route.getOutbound() != null) {
                                        boolean hasPathVariable = route.getInbound().contains("{") && route.getOutbound().contains("{");

                                        // 1) predicate는 항상 세팅
                                        route.setPredicates(List.of("Path=" + route.getInbound()));

                                        // 2) RewritePath 필터: inbound != outbound 이면 항상 필요
                                        if(hasPathVariable){
                                            String pattern = route.getInbound()
                                                    .replaceAll("\\{([^/}]+)}", "(?<$1>[^/]+)");
                                            String replacement = route.getOutbound()
                                                    .replaceAll("\\{([^/}]+)}", "\\${$1}");
                                            route.setFilters(List.of("RewritePath=" + pattern + ", " + replacement));
                                        } else if(!route.getInbound().equals(route.getOutbound())){
                                            // path variable은 없지만 경로가 다르면 단순 RewritePath
                                            route.setFilters(List.of("RewritePath=" + route.getInbound() + ", " + route.getOutbound()));
                                        }

                                        // 3) URI 스킴: websocket 플래그에 따라 분기
                                        String scheme = Boolean.TRUE.equals(route.isWebsocket()) ? "ws://" : "http://";
                                        route.setUri(scheme + service);
                                    }
                                }
                        );
                        log.debug("**** Routing info : " + props.getRoutes());
                        allRoutes.addAll(props.getRoutes());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load route files: ", e);
            throw new StriderException(StriderErrorCodes.INTERNAL_SERVER_ERROR, "Failed to load route files");
        }

        return allRoutes;
    }
}
