package com.bajrix.marketplace.repository;

import com.bajrix.marketplace.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
}
