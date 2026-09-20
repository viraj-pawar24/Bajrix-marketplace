package com.bajrix.marketplace.auth;

import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.exception.UnauthenticatedException;
import com.bajrix.marketplace.repository.SellerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves the mocked seller identity for every /api/seller/** request.
 *
 * Mechanism: the caller sends "X-Seller-Id: <id>". This stands in for a
 * real session/JWT. A production system would replace this single class
 * with real auth and everything downstream (SellerContext, services) would
 * keep working unchanged.
 */
@Component
public class SellerAuthInterceptor implements HandlerInterceptor {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private SellerContext sellerContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    	
    	if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(SELLER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthenticatedException(
                    "Missing " + SELLER_ID_HEADER + " header. Seller endpoints require a mocked seller identity.");
        }
        Long sellerId;
        try {
            sellerId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new UnauthenticatedException(SELLER_ID_HEADER + " must be a numeric seller id.");
        }
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new UnauthenticatedException("No seller found for id " + sellerId));

        sellerContext.setSellerId(seller.getId());
        return true;
    }
}
