package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.CreateProductRequest;
import com.bajrix.marketplace.dto.ListingDto;
import com.bajrix.marketplace.dto.ProductDetailDto;
import com.bajrix.marketplace.dto.ProductSummaryDto;
import com.bajrix.marketplace.entity.Product;
import com.bajrix.marketplace.exception.ResourceNotFoundException;
import com.bajrix.marketplace.repository.ProductRepository;
import com.bajrix.marketplace.repository.SellerListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerListingRepository sellerListingRepository;

    /**
     * Buyer-facing catalogue search. Pageable carries page/size/sort so the
     * client controls sorting (e.g. by name) while N+1 "from price" lookups
     * are done per row - acceptable at demo scale. At real scale (see
     * README) this would be denormalised (e.g. a materialised min-price
     * column refreshed on listing write) so the listing endpoint doesn't
     * have to aggregate seller_listings on every page load.
     */
    public Page<ProductSummaryDto> search(String keyword, String category, Pageable pageable) {
        Page<Product> products = productRepository.search(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                (category == null || category.isBlank()) ? null : category.trim(),
                pageable);

        return products.map(p -> new ProductSummaryDto(
                p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getUnit(),
                sellerListingRepository.findMinVisiblePrice(p.getId()),
                sellerListingRepository.countVisibleSellersForProduct(p.getId())
        ));
    }

    public ProductDetailDto getDetail(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));

        var listings = sellerListingRepository.findBuyerVisibleListingsForProduct(productId)
                .stream().map(ListingDto::from).toList();

        return new ProductDetailDto(p.getId(), p.getName(), p.getBrand(), p.getCategory(),
                p.getUnit(), p.getDescription(), listings);
    }

    /**
     * Lets a seller add a brand-new catalogue product (e.g. nothing matching
     * it exists yet) before creating a listing against it. Product creation
     * itself isn't seller-scoped data - the catalogue is shared - so this
     * intentionally does not check listing ownership.
     */
    public ProductSummaryDto create(CreateProductRequest req) {
        Product p = new Product();
        p.setName(req.getName().trim());
        p.setBrand(req.getBrand() == null ? null : req.getBrand().trim());
        p.setCategory(req.getCategory().trim());
        p.setUnit(req.getUnit());
        p.setDescription(req.getDescription());
        p = productRepository.save(p);
        return new ProductSummaryDto(p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getUnit(), null, 0);
    }
}
