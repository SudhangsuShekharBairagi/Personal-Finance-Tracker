package com.personalfinancetracker.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.personalfinancetracker.dto.BudgetNotificationResponse;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.entity.NotificationType;
import com.personalfinancetracker.security.AppUserPrincipal;
import com.personalfinancetracker.service.NotificationService;

@RestController
@RequestMapping("/api")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public Page<BudgetNotificationResponse> listNotifications(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationType type,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return notificationService.listNotifications(principal.getId(), status, pageable);
        }
        if (type != null) {
            return notificationService.listNotifications(principal.getId(), type, pageable);
        }
        return notificationService.listNotifications(principal.getId(), pageable);
    }

    @GetMapping("/notifications/{id}")
    public BudgetNotificationResponse getNotification(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id) {
        return notificationService.getNotification(principal.getId(), id);
    }
}
