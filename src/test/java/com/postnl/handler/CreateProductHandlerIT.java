package com.postnl.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.postnl.services.lambda.runtime.TestContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class CreateProductHandlerIT extends ProductHandlerTestBase {

    private final CreateProductHandler sut = new CreateProductHandler();
    private final GetProductHandler getProduct = new GetProductHandler();
    private final GetPackagesHandler getPackages = new GetPackagesHandler();

    @Test
    public void handleRequest_whenCreateProductInputStreamOk_puts200InOutputStream() throws IOException {
        Context ctxt = TestContext.builder().build();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        String input = "{\"body\": \"{\\\"productType\\\": \\\"box\\\", \\\"deliveryDate\\\": \\\"2020-10-08 13:00 - 15:00\\\"}\"}";

        sut.handleRequest(new ByteArrayInputStream(input.getBytes()), os, ctxt);
        Item outputWrapper = Item.fromJSON(os.toString());
        assertTrue(outputWrapper.hasAttribute("headers"));
        Map<String, Object> headers = outputWrapper.getMap("headers");
        assertNotNull(headers);
        assertEquals(1, headers.size());
        assertTrue(headers.containsKey("Content-Type"));
        assertEquals("application/json", headers.get("Content-Type"));
        assertTrue(outputWrapper.hasAttribute("statusCode"));
        assertEquals(201, outputWrapper.getInt("statusCode"));
        assertTrue(outputWrapper.hasAttribute("body"));
        String bodyString = outputWrapper.getString("body");
        assertNotNull(bodyString);
        Item body = Item.fromJSON(bodyString);
        verifyProductItem(body, "box", "2020-10-08 13:00 - 15:00");

        //now that we verified the created product, lets see if we can get it anew
        os = new ByteArrayOutputStream();
        String productId = body.getString("productId");

        getProduct.handleRequest(new ByteArrayInputStream(("{\"pathParameters\": { \"product_id\": \"" + productId + "\"}}").getBytes()), os, ctxt);

        outputWrapper = Item.fromJSON(os.toString());
        assertTrue(outputWrapper.hasAttribute("headers"));
        headers = outputWrapper.getMap("headers");
        assertNotNull(headers);
        assertEquals(1, headers.size());
        assertTrue(headers.containsKey("Content-Type"));
        assertEquals("application/json", headers.get("Content-Type"));
        assertTrue(outputWrapper.hasAttribute("statusCode"));
        assertEquals(200, outputWrapper.getInt("statusCode"));
        assertTrue(outputWrapper.hasAttribute("body"));
        bodyString = outputWrapper.getString("body");
        assertNotNull(bodyString);
        body = Item.fromJSON(bodyString);
        verifyProductItem(body, "box", "2020-10-08 13:00 - 15:00");

        //now that we can get the singleton lets see if we can get it in a page
        os = new ByteArrayOutputStream();
        getPackages.handleRequest(new ByteArrayInputStream("{}".getBytes()), os, ctxt);
        assertTrue(os.toString().contains(productId));

        assertTrue(os.toString().contains("200")); //SC_OK
    }

    private void verifyProductItem(Item body, String productType, String deliveryDate) {
        assertTrue(body.hasAttribute("productId"));
        String productId = body.getString("productId");
        assertNotNull(productId);
        assertTrue(productId.contains("-"));
        assertTrue(body.hasAttribute("productType"));
        String type = body.getString("productType");
        assertEquals(type, productType);
        assertTrue(body.hasAttribute("deliveryDate"));
        String date = body.getString("deliveryDate");
        assertEquals(date, deliveryDate);
    }

}
