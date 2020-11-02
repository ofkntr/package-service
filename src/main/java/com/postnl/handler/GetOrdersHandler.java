package com.postnl.handler;

import com.postnl.config.DaggerOrderComponent;
import com.postnl.config.OrderComponent;
import com.postnl.dao.OrderDao;
import com.postnl.model.OrderPage;
import com.postnl.model.response.GatewayResponse;
import com.postnl.model.response.GetOrdersResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import javax.inject.Inject;

/**
 *
 */
public class GetOrdersHandler implements OrderRequestStreamHandler {
    @Inject
    ObjectMapper objectMapper;
    @Inject
    OrderDao orderDao;
    private final OrderComponent orderComponent;

    public GetOrdersHandler() {
        orderComponent = DaggerOrderComponent.builder().build();
        orderComponent.inject(this);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output,
                              Context context) throws IOException {
        final JsonNode event;
        try {
            event = objectMapper.readTree(input);
        } catch (JsonMappingException e) {
            writeInvalidJsonInStreamResponse(objectMapper, output, e.getMessage());
            return;
        }
        final JsonNode queryParameterMap = event.findValue("queryParameters");
        final String exclusiveStartKeyQueryParameter = Optional.ofNullable(queryParameterMap)
                .map(mapNode -> mapNode.get("exclusive_start_key").asText())
                .orElse(null);

        OrderPage page = orderDao.getOrders(exclusiveStartKeyQueryParameter);
        //TODO handle exceptions
        objectMapper.writeValue(output, new GatewayResponse<>(
                objectMapper.writeValueAsString(
                        new GetOrdersResponse(page.getLastEvaluatedKey(), page.getOrders())),
                APPLICATION_JSON, SC_OK));
    }
}
