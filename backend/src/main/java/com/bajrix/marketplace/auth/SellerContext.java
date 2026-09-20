package com.bajrix.marketplace.auth;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Holds the "authenticated" seller for the current HTTP request.
 *
 * This is intentionally NOT real authentication (no passwords, no tokens,
 * no sessions) - the brief explicitly says that's out of scope. Instead the
 * caller identifies itself with an "X-Seller-Id" header, which
 * SellerAuthInterceptor validates and stores here before any controller
 * method runs. Every seller-scoped service method then reads the current
 * seller id from this bean instead of trusting an id supplied in a request
 * body/path, which is what actually prevents one seller from touching
 * another seller's listings.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SellerContext {

    private Long sellerId;

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
}
