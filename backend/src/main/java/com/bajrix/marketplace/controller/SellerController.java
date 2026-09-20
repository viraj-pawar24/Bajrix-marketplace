package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.dto.SellerDto;
import com.bajrix.marketplace.service.SellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only seller directory, used by the frontend's "log in as seller" mock selector. */
@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @GetMapping
    public List<SellerDto> listAll() {
        return sellerService.listAll();
    }

    @GetMapping("/{id}")
    public SellerDto getOne(@PathVariable Long id) {
        return sellerService.getById(id);
    }
}
