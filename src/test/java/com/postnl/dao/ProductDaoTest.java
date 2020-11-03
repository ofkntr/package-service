package com.postnl.dao;

import com.postnl.exception.CouldNotCreateProductException;
import com.postnl.exception.ProductDoesNotExistException;
import com.postnl.exception.TableDoesNotExistException;
import com.postnl.dto.request.CreateProductRequest;
import com.postnl.model.Product;
import com.postnl.model.ProductPage;

import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ProductDaoTest {

    private static final String PRODUCT_ID = "some product id";

    private final DynamoDbClient dynamoDb = mock(DynamoDbClient.class);

    private final ProductDao sut = new ProductDao(dynamoDb, "table_name", 10);

    @Test(expected = IllegalArgumentException.class)
    public void createProduct_whenRequestNull_throwsIllegalArgumentException() {
        sut.createProduct(null);
    }

    @Test(expected = TableDoesNotExistException.class)
    public void createProduct_whenTableDoesNotExist_throwsTableDoesNotExistException() {
        doThrow(ResourceNotFoundException.builder().build()).when(dynamoDb).putItem(any(PutItemRequest.class));
        sut.createProduct(CreateProductRequest.builder()
                .productType("box").deliveryDate("2020-10-08 13:00 - 15:00").build());
    }

    @Test(expected = TableDoesNotExistException.class)
    public void getProduct_whenTableDoesNotExist_throwsTableDoesNotExistException() {
        doThrow(ResourceNotFoundException.builder().build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = TableDoesNotExistException.class)
    public void getProducts_whenTableDoesNotExist_throwsTableDoesNotExistException() {
        doThrow(ResourceNotFoundException.builder().build()).when(dynamoDb).scan(any(ScanRequest.class));
        sut.getProducts(any());
    }

    @Test
    public void getProducts_whenTableEmpty_returnsEmptyPage() {
        doReturn(ScanResponse.builder()
                .items(new ArrayList<>())
                .lastEvaluatedKey(null)
                .build()).when(dynamoDb).scan(any(ScanRequest.class));
        ProductPage page = sut.getProducts(any());
        assertNotNull(page);
        assertNotNull(page.getProducts());
        assertTrue(page.getProducts().isEmpty());
        assertNull(page.getLastEvaluatedKey());
    }

    @Test(expected = IllegalStateException.class)
    public void getProducts_whenTableNotEmpty_butLastEvaluatedKeyHasProductIdSetToWrongType_throwsIllegalStateException() {
        doReturn(ScanResponse.builder()
                .items(Collections.singletonList(new HashMap<>()))
                .lastEvaluatedKey(Collections.singletonMap("productId", AttributeValue.builder().nul(true).build()))
                .build()
        ).when(dynamoDb).scan(any(ScanRequest.class));
        sut.getProducts(any());
    }

    @Test(expected = IllegalStateException.class)
    public void getProducts_whenTableNotEmpty_butLastEvaluatedKeyHasProductIdSetToUnsetAv_throwsIllegalStateException() {
        doReturn(ScanResponse.builder()
                .items(Collections.singletonList(new HashMap<>()))
                .lastEvaluatedKey(Collections.singletonMap("productId", AttributeValue.builder().build()))
                .build()
        ).when(dynamoDb).scan(any(ScanRequest.class));
        sut.getProducts(any());
    }

    @Test(expected = IllegalStateException.class)
    public void getProducts_whenTableNotEmpty_butLastEvaluatedKeyHasProductIdSetToEmptyString_throwsIllegalStateException() {
        doReturn(ScanResponse.builder()
                .items(Collections.singletonList(new HashMap<>()))
                .lastEvaluatedKey(Collections.singletonMap("productId", AttributeValue.builder().s("").build()))
                .build()
        ).when(dynamoDb).scan(any(ScanRequest.class));
        sut.getProducts(any());
    }

    @Test
    public void getProducts_whenTableNotEmpty_returnsPage() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("productId", AttributeValue.builder().s("p").build());
        item.put("productType", AttributeValue.builder().s("box").build());
        item.put("deliveryDate", AttributeValue.builder().s("2020-10-08").build());
        doReturn(ScanResponse.builder()
                .items(Collections.singletonList(item))
                .lastEvaluatedKey(Collections.singletonMap("productId", AttributeValue.builder().s("p").build()))
                .build()).when(dynamoDb).scan(any(ScanRequest.class));
        sut.getProducts(any());
    }

    @Test(expected = CouldNotCreateProductException.class)
    public void createProduct_whenAlreadyExists_throwsCouldNotCreateProductException() {
        doThrow(ConditionalCheckFailedException.builder().build()).when(dynamoDb).putItem(any(PutItemRequest.class));
        sut.createProduct(CreateProductRequest.builder()
                .productType("box").deliveryDate("2020-10-08 13:00 - 15:00").build());
    }

    @Test
    public void createProduct_whenOrDoesNotExist_createsProductWithPopulatedProductId() {
        Map<String, AttributeValue> createdItem = new HashMap<>();
        createdItem.put("productId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        createdItem.put("productType", AttributeValue.builder().s("box").build());
        createdItem.put("deliveryDate", AttributeValue.builder().s("2020-10-08 13:00 - 15:00").build());
        doReturn(PutItemResponse.builder().attributes(createdItem).build()).when(dynamoDb).putItem(any(PutItemRequest.class));

        Product product = sut.createProduct(CreateProductRequest.builder()
                .productType("box")
                .deliveryDate("2020-10-08 13:00 - 15:00")
                .build());
        //for a new item, object mapper sets version to 1
        assertEquals("box", product.getProductType());
        assertNotNull(product.getDeliveryDate());
        assertNotNull(UUID.fromString(product.getProductId()));
    }

    @Test(expected = ProductDoesNotExistException.class)
    public void getProduct_whenProductDoesNotExist_throwsProductDoesNotExist() {
        doReturn(GetItemResponse.builder().item(null).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = ProductDoesNotExistException.class)
    public void getProduct_whenGetItemReturnsEmptyHashMap_throwsIllegalStateException() {
        doReturn(GetItemResponse.builder().item(new HashMap<>()).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithProductIdWrongType_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().nul(true).build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithUnsetProductIdAV_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithEmptyProductId_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("").build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithCustomerIdWrongType_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().nul(true).build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithUnsetCustomerIdAV_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void getProduct_whenGetItemReturnsHashMapWithEmptyCustomerId_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().s("").build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test
    public void getProduct_whenGetItemReturnsHashMapWithPreTaxWrongType_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().s("a").build());
        map.put("deliveryDate", AttributeValue.builder().nul(true).build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test
    public void getProduct_whenGetItemReturnsHashMapWithUnsetPreTaxAV_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().s("a").build());
        map.put("deliveryDate", AttributeValue.builder().build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test
    public void getProduct_whenGetItemReturnsHashMapWithInvalidPreTax_throwsIllegalStateException() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("productId", AttributeValue.builder().s("a").build());
        map.put("productType", AttributeValue.builder().s("a").build());
        map.put("deliveryDate", AttributeValue.builder().s("2020-10-08 13:00 - 15:00").build());
        doReturn(GetItemResponse.builder().item(map).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        sut.getProduct(PRODUCT_ID);
    }

    @Test
    public void getProduct_whenProductExists_returnsProduct() {
        Map<String, AttributeValue> productItem = new HashMap<>();
        productItem.put("productId", AttributeValue.builder().s(PRODUCT_ID).build());
        productItem.put("productType", AttributeValue.builder().s("box").build());
        productItem.put("deliveryDate", AttributeValue.builder().s("2020-10-08 13:00 - 15:00").build());
        doReturn(GetItemResponse.builder().item(productItem).build()).when(dynamoDb).getItem(any(GetItemRequest.class));
        Product product = sut.getProduct(PRODUCT_ID);
        assertEquals(PRODUCT_ID, product.getProductId());
        assertEquals("box", product.getProductType());
        assertEquals("2020-10-08 13:00 - 15:00", product.getDeliveryDate());
    }

}
