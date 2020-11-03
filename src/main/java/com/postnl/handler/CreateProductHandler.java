package com.postnl.handler;

import com.postnl.config.DaggerProductComponent;
import com.postnl.config.ProductComponent;
import com.postnl.dao.ProductDao;
import com.postnl.exception.CouldNotCreateProductException;
import com.postnl.model.Product;
import com.postnl.dto.request.CreateProductRequest;
import com.postnl.dto.response.ErrorMessage;
import com.postnl.dto.response.GatewayResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;

public class CreateProductHandler implements DefaultRequestStreamHandler {

    private static final ErrorMessage REQUIRE_PRODUCT_TYPE_ERROR
            = new ErrorMessage("Require productType to create an product", SC_BAD_REQUEST);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ProductDao productDao;

    private final ProductComponent productComponent;

    public CreateProductHandler() {
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
        JsonNode createProductRequestBody = event.findValue("body");
        if (createProductRequestBody == null) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage("Body was null",
                                            SC_BAD_REQUEST)),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        final CreateProductRequest request;
        try {
            request = objectMapper.treeToValue(
                    objectMapper.readTree(createProductRequestBody.asText()),
                    CreateProductRequest.class);
        } catch (JsonParseException | JsonMappingException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage("Invalid JSON in body: "
                                            + e.getMessage(), SC_BAD_REQUEST)),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        if (request == null) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUEST_WAS_NULL_ERROR),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        if (isNullOrEmpty(request.getProductType())) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(REQUIRE_PRODUCT_TYPE_ERROR),
                            APPLICATION_JSON, SC_BAD_REQUEST));
            return;
        }

        try {
            final Product product = productDao.createProduct(request);
            objectMapper.writeValue(output,
                    new GatewayResponse<>(objectMapper.writeValueAsString(product),
                            APPLICATION_JSON, SC_CREATED));
        } catch (CouldNotCreateProductException e) {
            objectMapper.writeValue(output,
                    new GatewayResponse<>(
                            objectMapper.writeValueAsString(
                                    new ErrorMessage(e.getMessage(),
                                            SC_INTERNAL_SERVER_ERROR)),
                            APPLICATION_JSON, SC_INTERNAL_SERVER_ERROR));
        }
    }
}
