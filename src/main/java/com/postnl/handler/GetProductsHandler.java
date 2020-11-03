package com.postnl.handler;

import com.postnl.config.DaggerProductComponent;
import com.postnl.config.ProductComponent;
import com.postnl.dao.ProductDao;
import com.postnl.model.ProductPage;
import com.postnl.dto.response.GatewayResponse;
import com.postnl.dto.response.GetProductsResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import javax.inject.Inject;

public class GetProductsHandler implements DefaultRequestStreamHandler {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ProductDao productDao;

    private final ProductComponent productComponent;

    public GetProductsHandler() {
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
        final JsonNode queryParameterMap = event.findValue("queryParameters");
        final String offset = Optional.ofNullable(queryParameterMap)
                .map(mapNode -> mapNode.get("exclusive_start_key").asText())
                .orElse(null);

        ProductPage page = productDao.getProducts(offset);

        objectMapper.writeValue(output, new GatewayResponse<>(
                objectMapper.writeValueAsString(
                        new GetProductsResponse(page.getProducts().size(), page.getProducts())),
                APPLICATION_JSON, SC_OK));
    }

}
