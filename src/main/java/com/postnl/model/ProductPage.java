package com.postnl.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ProductPage {
    private final List<Product> products;
    private final String lastEvaluatedKey;
}
