package com.aydeed.shop;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShopOrder create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request.productId(), request.quantity());
    }

    @GetMapping("/{id}")
    public ShopOrder get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/pay")
    public ShopOrder pay(@PathVariable Long id) {
        return service.pay(id);
    }

    @PostMapping("/{id}/cancel")
    public ShopOrder cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
    
    
}