package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.CreateListingRequest;
import com.bajrix.marketplace.dto.ListingDto;
import com.bajrix.marketplace.dto.UpdateListingRequest;
import com.bajrix.marketplace.entity.Product;
import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.entity.SellerListing;
import com.bajrix.marketplace.exception.BusinessValidationException;
import com.bajrix.marketplace.exception.DuplicateListingException;
import com.bajrix.marketplace.exception.ForbiddenException;
import com.bajrix.marketplace.exception.ResourceNotFoundException;
import com.bajrix.marketplace.repository.ProductRepository;
import com.bajrix.marketplace.repository.SellerListingRepository;
import com.bajrix.marketplace.repository.SellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SellerListingService {

    @Autowired
    private SellerListingRepository listingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerRepository sellerRepository;

    public List<ListingDto> listForSeller(Long sellerId) {
        return listingRepository.findBySellerIdOrderByUpdatedAtDesc(sellerId)
                .stream().map(ListingDto::from).toList();
    }

    @Transactional
    public ListingDto create(Long sellerId, CreateListingRequest req) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller " + sellerId + " not found"));
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + req.getProductId() + " not found"));

        if (listingRepository.existsByProductIdAndSellerId(product.getId(), sellerId)) {
            throw new DuplicateListingException(
                    "You already have a listing for this product. Update the existing listing instead of creating a new one.");
        }
        validateBusinessRules(req.getPrice(), req.getStock(), req.getMinOrderQty());

        SellerListing listing = new SellerListing();
        listing.setProduct(product);
        listing.setSeller(seller);
        listing.setPrice(req.getPrice());
        listing.setStock(req.getStock());
        listing.setMinOrderQty(req.getMinOrderQty());
        listing.setActive(true);

        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }

    @Transactional
    public ListingDto update(Long sellerId, Long listingId, UpdateListingRequest req) {
        SellerListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing " + listingId + " not found"));

        // Authorization boundary: a seller may only ever modify their own listings.
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You do not have permission to modify this listing.");
        }

        // Optimistic-concurrency guard: reject stale writes explicitly (with a
        // clear message) rather than only relying on JPA's @Version check,
        // which would still catch it but with a less friendly error.
        if (!listing.getVersion().equals(req.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(SellerListing.class, listingId);
        }

        java.math.BigDecimal newPrice = req.getPrice() != null ? req.getPrice() : listing.getPrice();
        Integer newStock = req.getStock() != null ? req.getStock() : listing.getStock();
        Integer newMinOrderQty = req.getMinOrderQty() != null ? req.getMinOrderQty() : listing.getMinOrderQty();
        validateBusinessRules(newPrice, newStock, newMinOrderQty);

        listing.setPrice(newPrice);
        listing.setStock(newStock);
        listing.setMinOrderQty(newMinOrderQty);
        if (req.getActive() != null) {
            listing.setActive(req.getActive());
        }

        // save() is not strictly required inside @Transactional (dirty
        // checking would flush it anyway) but it's kept explicit for clarity
        // and so the @Version check fires predictably here.
        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }

    /** "Stop selling" - a soft delete via active=false, so history/orders referencing the listing stay intact. */
    @Transactional
    public ListingDto stopSelling(Long sellerId, Long listingId, Long expectedVersion) {
        SellerListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing " + listingId + " not found"));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenException("You do not have permission to modify this listing.");
        }
        if (expectedVersion != null && !listing.getVersion().equals(expectedVersion)) {
            throw new ObjectOptimisticLockingFailureException(SellerListing.class, listingId);
        }

        listing.setActive(false);
        listing = listingRepository.save(listing);
        return ListingDto.from(listing);
    }

    private void validateBusinessRules(java.math.BigDecimal price, Integer stock, Integer minOrderQty) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessValidationException("Price must be greater than 0.");
        }
        if (stock == null || stock < 0) {
            throw new BusinessValidationException("Stock cannot be negative.");
        }
        if (minOrderQty == null || minOrderQty < 1) {
            throw new BusinessValidationException("Minimum order quantity must be at least 1.");
        }
        // Not a hard error: a seller may legitimately be temporarily out of
        // stock below their usual MOQ. We surface this as "not orderable"
        // to buyers (see ListingDto.orderable) instead of rejecting the write.
    }
}
