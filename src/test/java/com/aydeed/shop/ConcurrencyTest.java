package com.aydeed.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class ConcurrencyTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopOrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void neverOversells() throws Exception {
        Product product = new Product();
        product.setName("Concurrency test item");
        product.setPrice(new BigDecimal("1.00"));
        product.setStock(10);
        product = productRepository.save(product);
        Long productId = product.getId();

        int buyers = 50;
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(buyers);
        ExecutorService pool = Executors.newFixedThreadPool(buyers);

        for (int i = 0; i < buyers; i++) {
            pool.submit(() -> {
                try {
                    startSignal.await();
                    orderService.create(productId, 1);
                    succeeded.incrementAndGet();
                } catch (ResponseStatusException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startSignal.countDown();
        finished.await();
        pool.shutdown();

        int stockLeft = productRepository.findById(productId).orElseThrow().getStock();

        List<ShopOrder> allOrders = orderRepository.findAll();
        for (ShopOrder order : allOrders) {
            if (order.getProductId().equals(productId)) {
                orderRepository.delete(order);
            }
        }
        productRepository.deleteById(productId);

        assertEquals(buyers, succeeded.get() + rejected.get());
        assertEquals(10, succeeded.get());
        assertEquals(40, rejected.get());
        assertEquals(0, stockLeft);
    }
}