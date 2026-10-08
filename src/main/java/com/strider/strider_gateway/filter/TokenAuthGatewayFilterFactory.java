package com.strider.strider_gateway.filter;

import com.strider.strider_gateway.client.UserProfileClient;
import com.strider.strider_gateway.config.AuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class TokenAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<TokenAuthGatewayFilterFactory.Config> {

    private final AuthProperties authProperties;
    private final UserProfileClient userProfileClient;

    public TokenAuthGatewayFilterFactory(AuthProperties authProperties, @Lazy UserProfileClient userProfileClient) {
        super(Config.class);
        this.authProperties = authProperties;
        this.userProfileClient = userProfileClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new TokenAuthFilter(authProperties, userProfileClient);
    }

    public static class Config {
    }
}
