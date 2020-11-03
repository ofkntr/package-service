package com.postnl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postnl.dao.ProductDao;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;


/**
 * Singleton scope bean configurations
 */
@Module
public class ProductModule {

    @Singleton
    @Provides
    @Named("tableName")
    String tableName() {
        return Optional.ofNullable(System.getenv("TABLE_NAME")).orElse("products_table");
    }

    @Singleton
    @Provides
    DynamoDbClient dynamoDb() {
        final String endpoint = System.getenv("ENDPOINT_OVERRIDE");
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        builder.httpClient(ApacheHttpClient.builder().build());
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Singleton
    @Provides
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Singleton
    @Provides
    public ProductDao productDao(DynamoDbClient dynamoDb, @Named("tableName") String tableName) {
        return new ProductDao(dynamoDb, tableName,10);
    }

}
