package com.strider.strider_gateway.client;

import com.strider.strider_common_lib.response.StriderResponse;
import com.strider.strider_gateway.client.model.CheckAuthorizationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserProfileClient {

    private final WebClient webClient;

    @Value("${service-config.services.user-profile}")
    private String userProfileBaseUrl;

    public Mono<StriderResponse<CheckAuthorizationResponseDto>> checkAuth(String token) {
        return webClient.get()
                .uri(userProfileBaseUrl + "/api/v1/user/profile/auth/token")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(String.class)
                .map(body ->
                        ResponseParser.parse(body, CheckAuthorizationResponseDto.class)
                );
    }
}
