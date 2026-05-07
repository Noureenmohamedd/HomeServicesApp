package org.example.notificationservice.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationStore notificationStore;

    public NotificationController(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    @GetMapping("/customers/{customerId}")
    public List<NotificationResponse> getCustomerNotifications(@PathVariable Long customerId) {
        return notificationStore.findByCustomerId(customerId);
    }

    @GetMapping("/providers/{providerId}")
    public List<NotificationResponse> getProviderNotifications(@PathVariable Long providerId) {
        return notificationStore.findByProviderId(providerId);
    }
}
