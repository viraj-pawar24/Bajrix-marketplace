package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.entity.Seller;
import com.bajrix.marketplace.entity.SellerStatus;

public class SellerDto {
    private Long id;
    private String name;
    private String contactEmail;
    private SellerStatus status;

    public static SellerDto from(Seller s) {
        SellerDto dto = new SellerDto();
        dto.id = s.getId();
        dto.name = s.getName();
        dto.contactEmail = s.getContactEmail();
        dto.status = s.getStatus();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getContactEmail() { return contactEmail; }
    public SellerStatus getStatus() { return status; }
}
