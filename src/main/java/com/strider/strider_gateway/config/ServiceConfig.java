package com.strider.strider_gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "service-config")
public class ServiceConfig {
    private Map<String, String> services;

    @PostConstruct
    public void logServices() {
        log.info("Loaded services: {}", services);
    }

}
