package com.bajrix.marketplace.repository;

import com.bajrix.marketplace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Keyword + category search. Uses LOWER(...) LIKE for a simple case-insensitive
     * partial match, which is fine at demo scale. At the ~1M product scale
     * mentioned in the brief this should move to a real search index
     * (MySQL FULLTEXT index, or an external engine like
     * OpenSearch) - see README "Scalability" notes.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                     OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR p.category = :category)
            """)
    Page<Product> search(@Param("keyword") String keyword,
                          @Param("category") String category,
                          Pageable pageable);
}
