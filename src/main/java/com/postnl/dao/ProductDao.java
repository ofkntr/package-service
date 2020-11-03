package com.postnl.dao;

import com.postnl.exception.CouldNotCreateProductException;
import com.postnl.exception.ProductDoesNotExistException;
import com.postnl.exception.TableDoesNotExistException;
import com.postnl.dto.request.CreateProductRequest;
import com.postnl.model.Product;
import com.postnl.model.ProductPage;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProductDao {

    private static final String PRODUCT_ID = "productId";
    private static final String DELIVERY_DATE_WAS_NULL = "preTaxAmount was null";

    private final String tableName;
    private final DynamoDbClient dynamoDb;
    private final int pageSize;

    /**
     * Constructs an ProductDao.
     * @param dynamoDb dynamodb client
     * @param tableName name of table to use for products
     * @param pageSize size of pages for getProducts
     */
    public ProductDao(final DynamoDbClient dynamoDb, final String tableName,
                    final int pageSize) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.pageSize = pageSize;
    }

    /**
     * Returns an product or throws if the product does not exist.
     * @param productId id of product to get
     * @return the product if it exists
     * @throws ProductDoesNotExistException if the product does not exist
     */
    public Product getProduct(final String productId) {
        try {
            return Optional.ofNullable(
                    dynamoDb.getItem(GetItemRequest.builder()
                            .tableName(tableName)
                            .key(Collections.singletonMap(PRODUCT_ID,
                                    AttributeValue.builder().s(productId).build()))
                            .build()))
                    .map(GetItemResponse::item)
                    .map(this::convert)
                    .orElseThrow(() -> new ProductDoesNotExistException(String.format("Product %s does not exist", productId)));
        } catch (ResourceNotFoundException e) {
            throw new TableDoesNotExistException(String.format("Product table %s does not exist", tableName));
        }
    }

    /**
     * Get products.
     * @param offset the exclusive start id for the next page.
     * @return a page of products.
     * @throws TableDoesNotExistException if the product table does not exist
     */
    public ProductPage getProducts(final String offset) {
        final ScanResponse result;

        try {
            ScanRequest.Builder scanBuilder = ScanRequest.builder()
                    .tableName(tableName)
                    .limit(pageSize);
            if (!isNullOrEmpty(offset)) {
                scanBuilder.exclusiveStartKey(Collections.singletonMap(PRODUCT_ID,
                        AttributeValue.builder().s(offset).build()));
            }
            result = dynamoDb.scan(scanBuilder.build());
        } catch (ResourceNotFoundException e) {
            throw new TableDoesNotExistException(String.format("Product table %s does not exist", tableName));
        }

        final List<Product> products = result.items().stream()
                .map(this::convert)
                .collect(Collectors.toList());

        ProductPage.ProductPageBuilder builder = ProductPage.builder().products(products);
        if (result.lastEvaluatedKey() != null && !result.lastEvaluatedKey().isEmpty()) {
            if ((!result.lastEvaluatedKey().containsKey(PRODUCT_ID)
                    || isNullOrEmpty(result.lastEvaluatedKey().get(PRODUCT_ID).s()))) {
                throw new IllegalStateException(
                    "productId did not exist or was not a non-empty string in the lastEvaluatedKey");
            } else {
                builder.lastEvaluatedKey(result.lastEvaluatedKey().get(PRODUCT_ID).s());
            }
        }
        return builder.build();
    }

    /**
     * Creates an product.
     * @param createProductRequest details of product to create
     * @return created product
     */
    public Product createProduct(final CreateProductRequest createProductRequest) {
        if (createProductRequest == null) {
            throw new IllegalArgumentException("CreateProductRequest was null");
        }
        int tries = 0;
        while (tries < 10) {
            try {
                Map<String, AttributeValue> item = createProductItem(createProductRequest);
                dynamoDb.putItem(PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("attribute_not_exists(productId)")
                        .build());
                return Product.builder()
                        .productId(item.get(PRODUCT_ID).s())
                        .productType(item.get("productType").s())
                        .deliveryDate(item.get("deliveryDate").s())
                        .build();
            } catch (ConditionalCheckFailedException e) {
                tries++;
            } catch (ResourceNotFoundException e) {
                throw new TableDoesNotExistException(String.format("Product table %s does not exist", tableName));
            }
        }
        throw new CouldNotCreateProductException(
                "Unable to generate unique product id after 10 tries");
    }

    private Product convert(final Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        Product.ProductBuilder builder = Product.builder();

        try {
            builder.productId(item.get(PRODUCT_ID).s());
        } catch (NullPointerException e) {
            throw new IllegalStateException("item did not have an productId attribute or it was not a String");
        }

        try {
            builder.productType(item.get("productType").s());
        } catch (NullPointerException e) {
            throw new IllegalStateException("item did not have an productType attribute or it was not a String");
        }

        try {
            builder.deliveryDate(item.get("deliveryDate").s());
        } catch (NullPointerException e) {
            throw new IllegalStateException("item did not have an deliveryDate attribute or it was not a String");
        }

        return builder.build();
    }

    private Map<String, AttributeValue> createProductItem(final CreateProductRequest product) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(PRODUCT_ID, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("productType", AttributeValue.builder().s(validateCustomerId(product.getProductType())).build());
        try {
            item.put("deliveryDate",
                    AttributeValue.builder().s(product.getDeliveryDate()).build());
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(DELIVERY_DATE_WAS_NULL);
        }
        return item;
    }

    private String validateCustomerId(final String customerId) {
        if (isNullOrEmpty(customerId)) {
            throw new IllegalArgumentException("customerId was null or empty");
        }
        return customerId;
    }

    private static boolean isNullOrEmpty(final String string) {
        return string == null || string.isEmpty();
    }
}

