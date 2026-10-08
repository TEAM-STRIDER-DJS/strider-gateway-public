package com.strider.strider_gateway.config;

import lombok.Data;
import java.util.List;

@Data
public class RoutingConfigProperties {

    private List<RouteDefinition> routes;

    @Data
    public static class RouteDefinition {
        private String service;
        private String id;
        private String inbound;
        private String outbound;
        private List<String> predicates;
        private List<String> filters;
        private String uri;
        private boolean websocket;
    }
}