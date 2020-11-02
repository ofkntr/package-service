package com.postnl.model.response;

import com.postnl.model.Order;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonAutoDetect
public class GetOrdersResponse {
    private final String lastEvaluatedKey;
    private final List<Order> orders;
}
