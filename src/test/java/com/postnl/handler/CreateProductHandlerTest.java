package com.postnl.handler;

import com.postnl.services.lambda.runtime.TestContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class CreateProductHandlerTest {

    private CreateProductHandler sut = new CreateProductHandler();

    @Test
    public void handleRequest_whenCreateProductInputStreamEmpty_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        sut.handleRequest(new ByteArrayInputStream(new byte[0]), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Body was null"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateProductInputStreamHasNoBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{}";
        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Body was null"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateProductInputStreamHasNullBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"null\"}";
        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Request was null"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateProductInputStreamHasWrongTypeForBody_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"1\"}";
        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Invalid JSON"));
        assertTrue(os.toString().contains("400"));
    }

    @Test
    public void handleRequest_whenCreateProductInputStreamHasEmptyBodyDict_puts400InOutputStream() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String input = "{\"body\": \"{}\"}";
        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, TestContext.builder().build());
        assertTrue(os.toString().contains("Require productType to create an product"));
        assertTrue(os.toString().contains("400"));
    }

}
