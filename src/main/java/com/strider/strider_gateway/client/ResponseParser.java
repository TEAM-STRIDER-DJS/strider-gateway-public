package com.strider.strider_gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import com.strider.strider_common_lib.response.Meta;
import com.strider.strider_common_lib.response.StriderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class ResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> StriderResponse<T> parse(String json, Class<T> clazz) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            Meta meta = objectMapper.readValue(rootNode.get("meta").toString(), Meta.class);

            var dataNode = rootNode.get("data");
            T data = dataNode == null || dataNode.isNull() ? null : objectMapper.treeToValue(dataNode, clazz);

            return StriderResponse.<T>builder()
                    .data(data)
                    .meta(meta)
                    .build();
        } catch (Exception e) {
            log.error("Fail to parse Json: ", e);
            throw new StriderException(StriderErrorCodes.INTERNAL_SERVER_ERROR, "Fail to parse JSON");
        }
    }
}
