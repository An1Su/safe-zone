package com.buyapp.orderservice.repository;

import com.buyapp.orderservice.model.Order;
import com.buyapp.orderservice.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Find all orders by user ID
     * @param userId the user ID
     * @return List of orders for the user
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find orders by user ID and status
     * @param userId the user ID
     * @param status the order status
     * @return List of orders matching the criteria
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, OrderStatus status);

    /**
     * Find orders containing items from a specific seller
     * @param sellerId the seller ID
     * @return List of orders containing seller's products
     */
    List<Order> findByItemsSellerIdOrderByCreatedAtDesc(String sellerId);

    /**
     * Find orders by seller ID and status
     * @param sellerId the seller ID
     * @param status the order status
     * @return List of orders matching the criteria
     */
    List<Order> findByItemsSellerIdAndStatusOrderByCreatedAtDesc(String sellerId, OrderStatus status);

    /**
     * Find orders created between two dates
     * @param startDate the start date
     * @param endDate the end date
     * @return List of orders created in the date range
     */
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find orders by user ID created between two dates
     * @param userId the user ID
     * @param startDate the start date
     * @param endDate the end date
     * @return List of orders matching the criteria
     */
    List<Order> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Delete orders by user ID
     * @param userId the user ID
     */
    void deleteByUserId(String userId);
}
