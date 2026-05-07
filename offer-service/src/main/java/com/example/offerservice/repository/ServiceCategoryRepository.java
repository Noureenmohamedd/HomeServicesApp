package com.example.offerservice.repository;

import com.example.offerservice.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    Optional<ServiceCategory> findByNormalizedName(String normalizedName);

    boolean existsByNormalizedName(String normalizedName);

    List<ServiceCategory> findByActiveTrueOrderByNameAsc();
}
