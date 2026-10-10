package com.aydeed.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int release(@Param("id") Long id, @Param("qty") int qty);

    List<Product> findByStockLessThan(int threshold);

        @Query(value = """
            SELECT p.id, p.name, p.description, p.price, p.stock
            FROM products p
            WHERE p.search_vector @@ websearch_to_tsquery('english', :q)
              AND p.price >= :minPrice
              AND p.price <= :maxPrice
              AND (:inStock = false OR p.stock > 0)
            ORDER BY ts_rank(p.search_vector, websearch_to_tsquery('english', :q)) DESC, p.id
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> search(@Param("q") String q,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("inStock") boolean inStock,
                         @Param("limit") int limit);
}