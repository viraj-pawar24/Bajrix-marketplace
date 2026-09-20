package com.bajrix.marketplace.config;

import com.bajrix.marketplace.auth.SellerAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS is wide-open here because this is a take-home project meant to run
 * locally with a Vite dev server on a different port. Do not ship this
 * CORS config as-is to a real environment.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SellerAuthInterceptor sellerAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sellerAuthInterceptor)
                .addPathPatterns("/api/seller/**");
    }
}
