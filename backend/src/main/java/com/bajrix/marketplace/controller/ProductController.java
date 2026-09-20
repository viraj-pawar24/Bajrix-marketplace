package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.dto.CreateProductRequest;
import com.bajrix.marketplace.dto.ProductDetailDto;
import com.bajrix.marketplace.dto.ProductSummaryDto;
import com.bajrix.marketplace.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Buyer-facing catalogue endpoints - no seller auth required. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Page<ProductSummaryDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        int safeSize = Math.min(Math.max(size, 1), 100); // cap page size to protect the DB at scale
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String safeSortBy = List.of("name", "category", "createdAt").contains(sortBy) ? sortBy : "name";
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(dir, safeSortBy));

        return productService.search(q, category, pageable);
    }

    @GetMapping("/{id}")
    public ProductDetailDto getOne(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    /** Any seller can add a catalogue product ahead of listing it; the catalogue itself is shared, not owned. */
    @PostMapping
    public ResponseEntity<ProductSummaryDto> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }
}
