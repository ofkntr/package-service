package com.postnl.handler;

import com.postnl.config.DaggerProductTestComponent;
import com.postnl.config.ProductTestComponent;
import org.junit.After;
import org.junit.Before;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import javax.inject.Inject;

/**
 * This class serves as the base class for Integration tests. do not include I T in
 * the class name so that it does not get picked up by failsafe.
 */
public abstract class ProductHandlerTestBase {

    private static final String TABLE_NAME = "products_table";

    private final ProductTestComponent productTestComponent;

    @Inject
    DynamoDbClient dynamoDb;

    public ProductHandlerTestBase() {
        productTestComponent = DaggerProductTestComponent.builder().build();
        productTestComponent.inject(this);
    }

    @Before
    public void setup() {
        dynamoDb.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .keyType(KeyType.HASH)
                        .attributeName("productId")
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("productId")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .provisionedThroughput(
                        ProvisionedThroughput.builder()
                                .readCapacityUnits(1L)
                                .writeCapacityUnits(1L)
                                .build())
                .build());

    }

    @After
    public void teardown() {
        dynamoDb.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
    }

}
