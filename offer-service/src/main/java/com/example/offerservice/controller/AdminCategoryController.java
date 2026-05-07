package com.example.offerservice.controller;

import com.example.offerservice.config.AuthenticatedUser;
import com.example.offerservice.dto.AddServiceCategoryRequest;
import com.example.offerservice.dto.ServiceCategoryResponse;
import com.example.offerservice.service.ServiceCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private static final Logger LOGGER = Logger.getLogger(AdminCategoryController.class.getName());

    private final ServiceCategoryService serviceCategoryService;

    public AdminCategoryController(ServiceCategoryService serviceCategoryService) {
        this.serviceCategoryService = serviceCategoryService;
    }

    @PostMapping
    public ResponseEntity<ServiceCategoryResponse> addCategory(
            @Valid @RequestBody AddServiceCategoryRequest request
    ) {
        AuthenticatedUser user = getAuthenticatedAdmin();
        LOGGER.info("Admin add category request: adminId=" + user.userId() + ", category=" + request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCategoryService.addCategory(request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceCategoryResponse>> getCategories() {
        AuthenticatedUser user = getAuthenticatedAdmin();
        LOGGER.info("Admin category list request: adminId=" + user.userId());
        return ResponseEntity.ok(serviceCategoryService.getActiveCategories());
    }

    private AuthenticatedUser getAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!"ADMIN".equalsIgnoreCase(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
        return user;
    }
}
