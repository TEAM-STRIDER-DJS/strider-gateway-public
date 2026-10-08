package com.strider.strider_gateway;

import com.strider.strider_gateway.config.AuthProperties;
import com.strider.strider_gateway.config.ServiceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties({ServiceConfig.class, AuthProperties.class})
public class StriderGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(StriderGatewayApplication.class, args);
	}
}
