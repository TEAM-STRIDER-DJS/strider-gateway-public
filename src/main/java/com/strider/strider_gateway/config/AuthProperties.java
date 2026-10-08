package com.strider.strider_gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "spring.strider-cloud-properties.filter.auth")
@Data
@Slf4j
public class AuthProperties {
    private boolean enable;
    private List<String> ignores;

    @PostConstruct
    public void logServices() {
        log.info("Loaded auth filter - ignored apis: {}", ignores);
    }
}