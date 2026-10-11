package com.trustpay.backend.repository;

import com.trustpay.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByName(String name);

    boolean existsByEmailIgnoreCase(String email);

    List<Company> findAllByOrderByNameAsc();
}
