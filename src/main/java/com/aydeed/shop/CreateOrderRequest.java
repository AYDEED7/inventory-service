package com.aydeed.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull Long productId, @Min(1) int quantity) {
    
}
