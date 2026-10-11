package com.trustpay.backend.dto.response;

import com.trustpay.backend.entity.Company;

import java.util.UUID;

public record CompanyResponse(UUID id, String name, String email) {

    public static CompanyResponse fromEntity(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getEmail());
    }
}
