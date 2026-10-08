package com.strider.strider_gateway.exception;

import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import com.strider.strider_common_lib.response.StriderResponse;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class GlobalGatewayStriderExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalGatewayStriderExceptionHandler(ErrorAttributes errorAttributes,
                                         ApplicationContext applicationContext,
                                         ServerCodecConfigurer codecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        this.setMessageReaders(codecConfigurer.getReaders());
        this.setMessageWriters(codecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::formatErrorResponse);
    }

    private Mono<ServerResponse> formatErrorResponse(ServerRequest serverRequest) {
        Throwable error = getError(serverRequest);

        StriderResponse<Object> response;
        HttpStatusCode status;

        if (error instanceof StriderException ex) {
            response = StriderResponse.error(ex.getErrorCode());
            status = ex.getErrorCode().getHttpStatus();
        } else {
            response = StriderResponse.error(StriderErrorCodes.INTERNAL_SERVER_ERROR);
            status = StriderErrorCodes.INTERNAL_SERVER_ERROR.getHttpStatus();
        }

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

}
