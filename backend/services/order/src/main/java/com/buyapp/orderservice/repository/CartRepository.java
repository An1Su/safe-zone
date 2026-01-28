package com.buyapp.orderservice.repository;

import com.buyapp.orderservice.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {

    /**
     * Find cart by user ID
     * @param userId the user ID
     * @return Optional containing the cart if found
     */
    Optional<Cart> findByUserId(String userId);

    /**
     * Delete cart by user ID
     * @param userId the user ID
     */
    void deleteByUserId(String userId);
}
