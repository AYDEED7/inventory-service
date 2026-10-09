package com.aydeed.shop;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findByStatusAndExpiresAtBefore(OrderStatus status, Instant time);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ShopOrder o SET o.status = :toStatus WHERE o.id = :id AND o.status = :fromStatus")
    int changeStatus(@Param("id") Long id,
                     @Param("fromStatus") OrderStatus fromStatus,
                     @Param("toStatus") OrderStatus toStatus);

} 