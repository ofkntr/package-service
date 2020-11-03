package com.postnl.handler;

import com.postnl.services.lambda.runtime.TestContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetProductHandlerTest {

    private GetProductHandler sut = new GetProductHandler();

    @Test
    public void handleRequest_whenGetProductInputStreamEmpty_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        sut.handleRequest(new ByteArrayInputStream(new byte[0]), os, TestContext.builder().build());
        assertTrue(os.toString().contains("product_id was not set"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenGetProductInputStreamHasNoMappedProductIdPathParam_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"pathParameters\": { }}";
        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("product_id was not set"));
        assertTrue(os.toString().contains("400"));
    }

}
