package com.example.offerservice.service;

import com.example.offerservice.dto.AddServiceCategoryRequest;
import com.example.offerservice.dto.ServiceCategoryResponse;
import com.example.offerservice.entity.ServiceCategory;
import com.example.offerservice.repository.ServiceCategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServiceCategoryService {

    private static final Map<String, String> DEFAULT_CATEGORIES = Map.of(
            "plumbing", "PLUMBER",
            "carpentry", "CARPENTER",
            "electrical", "ELECTRICIAN",
            "cleaning", "CLEANER",
            "painting", "PAINTER"
    );

    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCategoryService(ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultCategories() {
        DEFAULT_CATEGORIES.forEach((name, professionType) -> {
            String normalizedName = normalizeName(name);
            if (!serviceCategoryRepository.existsByNormalizedName(normalizedName)) {
                serviceCategoryRepository.save(ServiceCategory.builder()
                        .name(name)
                        .normalizedName(normalizedName)
                        .professionType(professionType)
                        .active(true)
                        .build());
            }
        });
    }

    @Transactional
    public ServiceCategoryResponse addCategory(AddServiceCategoryRequest request) {
        String name = normalizeDisplayName(request.getName());
        String normalizedName = normalizeName(name);
        if (serviceCategoryRepository.existsByNormalizedName(normalizedName)) {
            throw new IllegalArgumentException("Service category already exists: " + name);
        }

        ServiceCategory category = ServiceCategory.builder()
                .name(name)
                .normalizedName(normalizedName)
                .professionType(normalizeProfessionType(request.getProfessionType()))
                .active(true)
                .build();

        return mapToResponse(serviceCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getActiveCategories() {
        return serviceCategoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceCategory getActiveCategoryOrThrow(String categoryName) {
        String normalizedName = normalizeName(categoryName);
        return serviceCategoryRepository.findByNormalizedName(normalizedName)
                .filter(ServiceCategory::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid service category: " + categoryName));
    }

    public boolean professionCanUseCategory(String professionType, String categoryName) {
        ServiceCategory category = getActiveCategoryOrThrow(categoryName);
        String categoryProfessionType = category.getProfessionType();
        if (categoryProfessionType == null || categoryProfessionType.isBlank()) {
            return true;
        }
        return categoryProfessionType.equalsIgnoreCase(normalizeProfessionType(professionType));
    }

    private ServiceCategoryResponse mapToResponse(ServiceCategory category) {
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .professionType(category.getProfessionType())
                .active(category.isActive())
                .build();
    }

    private String normalizeDisplayName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        String displayName = normalizeDisplayName(name);
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        return displayName;
    }

    private String normalizeProfessionType(String professionType) {
        if (professionType == null || professionType.isBlank()) {
            return null;
        }
        return professionType.trim().toUpperCase(Locale.ROOT);
    }
}
