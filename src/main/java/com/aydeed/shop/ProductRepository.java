package com.aydeed.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int release(@Param("id") Long id, @Param("qty") int qty);

    List<Product> findByStockLessThan(int threshold);
}