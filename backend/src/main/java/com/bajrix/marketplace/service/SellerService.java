package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.SellerDto;
import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.exception.ResourceNotFoundException;
import com.bajrix.marketplace.repository.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SellerService {

    @Autowired
    private SellerRepository sellerRepository;

    /**
     * Used to populate the "log in as seller" selector in the demo UI, since
     * there's no real login. Returns everyone (including PENDING/REJECTED)
     * so the seller dashboard can be exercised for every status.
     */
    public List<SellerDto> listAll() {
        return sellerRepository.findAll().stream().map(SellerDto::from).toList();
    }

    public SellerDto getById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller " + id + " not found"));
        return SellerDto.from(seller);
    }
}
