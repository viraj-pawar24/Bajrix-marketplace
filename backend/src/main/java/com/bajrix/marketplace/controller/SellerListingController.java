package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.auth.SellerContext;
import com.bajrix.marketplace.dto.CreateListingRequest;
import com.bajrix.marketplace.dto.ListingDto;
import com.bajrix.marketplace.dto.UpdateListingRequest;
import com.bajrix.marketplace.service.SellerListingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Seller-scoped listing management. Every method here relies on
 * SellerAuthInterceptor (registered on /api/seller/**) having already
 * populated SellerContext from the X-Seller-Id header, and on
 * SellerListingService checking listing ownership before any mutation.
 */
@RestController
@RequestMapping("/api/seller/listings")
public class SellerListingController {

    @Autowired
    private SellerListingService sellerListingService;

    @Autowired
    private SellerContext sellerContext;

    @GetMapping
    public List<ListingDto> myListings() {
        return sellerListingService.listForSeller(sellerContext.getSellerId());
    }

    @PostMapping
    public ResponseEntity<ListingDto> create(@Valid @RequestBody CreateListingRequest request) {
        ListingDto dto = sellerListingService.create(sellerContext.getSellerId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ListingDto update(@PathVariable Long id, @Valid @RequestBody UpdateListingRequest request) {
        return sellerListingService.update(sellerContext.getSellerId(), id, request);
    }

    /** Soft-delete: marks the listing inactive rather than removing the row. Body may carry {"version": n}. */
    @DeleteMapping("/{id}")
    public ListingDto stopSelling(@PathVariable Long id, @RequestBody(required = false) Map<String, Long> body) {
        Long expectedVersion = body == null ? null : body.get("version");
        return sellerListingService.stopSelling(sellerContext.getSellerId(), id, expectedVersion);
    }
}
