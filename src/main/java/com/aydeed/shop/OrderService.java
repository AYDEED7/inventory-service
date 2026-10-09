package com.aydeed.shop;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ShopOrderRepository orderRepository;
    private final long reservationMinutes;

    public OrderService(ProductRepository productRepository,
                        ShopOrderRepository orderRepository,
                        @Value("${shop.reservation-minutes:10}") long reservationMinutes) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reservationMinutes = reservationMinutes;
    }

    @Transactional
    public ShopOrder create(Long productId, int quantity) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (productRepository.reserve(productId, quantity) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock");
        }
        Instant now = Instant.now();
        ShopOrder order = new ShopOrder();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setExpiresAt(now.plus(Duration.ofMinutes(reservationMinutes)));
        return orderRepository.save(order);
    }

    public ShopOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Transactional
    public ShopOrder pay(Long id) {
        ShopOrder order = get(id);
        if (Instant.now().isAfter(order.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation has expired");
        }
        if (orderRepository.changeStatus(id, OrderStatus.PENDING, OrderStatus.PAID) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not pending");
        }
        return get(id);
    }

    @Transactional
    public ShopOrder cancel(Long id) {
        ShopOrder order = get(id);
        if (orderRepository.changeStatus(id, OrderStatus.PENDING, OrderStatus.CANCELLED) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not pending");
        }
        productRepository.release(order.getProductId(), order.getQuantity());
        return get(id);
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void expireOldOrders() {
        List<ShopOrder> old = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());
        for (ShopOrder order : old) {
            if (orderRepository.changeStatus(order.getId(), OrderStatus.PENDING, OrderStatus.EXPIRED) == 1) {
                productRepository.release(order.getProductId(), order.getQuantity());
            }
        }
    }
}