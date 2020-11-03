package com.postnl.dto.response;

import com.postnl.model.Product;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonAutoDetect
public class GetProductsResponse {
    private final int total;
    private final List<Product> products;
}
