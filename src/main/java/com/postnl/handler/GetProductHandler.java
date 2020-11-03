package com.postnl.handler;

import com.postnl.config.DaggerProductComponent;
import com.postnl.config.ProductComponent;
import com.postnl.dao.ProductDao;
import com.postnl.exception.ProductDoesNotExistException;
import com.postnl.model.Product;
import com.postnl.dto.response.ErrorMessage;
import com.postnl.dto.response.GatewayResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import javax.inject.Inject;

public class GetProductHandler implements DefaultRequestStreamHandler {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ProductDao productDao;

    private final ProductComponent productComponent;

    public GetProductHandler() {
        productComponent = DaggerProductComponent.builder().build();
        productComponent.inject(this);
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
        if (event == null) {
            writeInvalidJsonInStreamResponse(objectMapper, output, "event was null");
            return;
        }
        final JsonNode pathParameterMap = event.findValue("pathParameters");
        final String productId = Optional.ofNullable(pathParameterMap)
                .map(mapNode -> mapNode.get("product_id"))
                .map(JsonNode::asText)
                .orElse(null);
        if (isNullOrEmpty(productId)) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(PRODUCT_ID_WAS_NOT_SET),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }
        try {
            Product product = productDao.getProduct(productId);
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(product),
                            APPLICATION_JSON, SC_OK));
        } catch (ProductDoesNotExistException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage(e.getMessage(), SC_NOT_FOUND)),
                            APPLICATION_JSON, SC_NOT_FOUND));
        }
    }
}
