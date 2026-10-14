package com.personalfinancetracker.service;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.personalfinancetracker.entity.BudgetNotification;
import com.personalfinancetracker.entity.NotificationStatus;
import com.personalfinancetracker.repository.BudgetNotificationRepository;

@Service
public class BudgetNotificationDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(BudgetNotificationDeliveryService.class);
    private static final List<Long> RETRY_BACKOFF_MINUTES = List.of(1L, 5L, 15L, 60L, 180L);
    private static final Duration DELIVERY_LEASE = Duration.ofHours(3);

    private final BudgetNotificationRepository notificationRepository;
    private final MailService mailService;
    private final TransactionTemplate requiresNewTransaction;
    private final int maxAttempts;

    public BudgetNotificationDeliveryService(
            BudgetNotificationRepository notificationRepository,
            MailService mailService,
            PlatformTransactionManager transactionManager,
            @Value("${app.notifications.max-attempts:5}") int maxAttempts) {
        this.notificationRepository = notificationRepository;
        this.mailService = mailService;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.maxAttempts = maxAttempts;
    }

    public void processNotification(Long notificationId) {
        DeliveryDetails delivery = requiresNewTransaction.execute(
                transactionStatus -> claimNotification(notificationId));
        if (delivery == null) {
            return;
        }

        try {
            mailService.sendBudgetAlert(
                    delivery.recipientEmail(),
                    delivery.category(),
                    delivery.period(),
                    delivery.thresholdPercent(),
                    delivery.actualSpending(),
                    delivery.budgetLimit());
        } catch (Exception exception) {
            logger.warn("Budget alert send failed for notification id {}: {}",
                    notificationId, exception.getClass().getSimpleName());
            requiresNewTransaction.execute(transactionStatus -> markFailedAttempt(delivery));
            return;
        }
        requiresNewTransaction.execute(transactionStatus -> markSent(delivery));
    }

    private DeliveryDetails claimNotification(Long notificationId) {
        BudgetNotification notification = notificationRepository.findByIdForUpdate(notificationId).orElse(null);
        if (notification == null
                || (notification.getStatus() != NotificationStatus.PENDING
                        && notification.getStatus() != NotificationStatus.RETRYING)) {
            return null;
        }

        Instant now = Instant.now();
        if (notification.getNextAttemptAt() != null && notification.getNextAttemptAt().isAfter(now)) {
            return null;
        }
        if (notification.getAttemptCount() >= maxAttempts) {
            notification.markFailed("Retry limit reached.");
            notificationRepository.save(notification);
            return null;
        }

        notification.markAttemptStarted(now.plus(DELIVERY_LEASE));
        notificationRepository.save(notification);
        return new DeliveryDetails(
                notification.getId(),
                notification.getAttemptCount(),
                notification.getRecipientEmail(),
                notification.getCategory(),
                String.format(Locale.ROOT, "%d-%02d",
                        notification.getBudgetYear(), notification.getBudgetMonth()),
                notification.getThresholdPercent().toPlainString(),
                notification.getActualSpending().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                notification.getBudgetLimit().setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private Void markSent(DeliveryDetails delivery) {
        notificationRepository.findByIdForUpdate(delivery.notificationId())
                .filter(notification -> isCurrentAttempt(notification, delivery))
                .ifPresent(notification -> {
                    notification.markSent();
                    notificationRepository.save(notification);
                });
        return null;
    }

    private Void markFailedAttempt(DeliveryDetails delivery) {
        notificationRepository.findByIdForUpdate(delivery.notificationId())
                .filter(notification -> isCurrentAttempt(notification, delivery))
                .ifPresent(notification -> {
                    if (delivery.attemptCount() >= maxAttempts) {
                        notification.markFailed("Delivery failed.");
                    } else {
                        long delayMinutes = RETRY_BACKOFF_MINUTES.get(
                                Math.min(delivery.attemptCount() - 1, RETRY_BACKOFF_MINUTES.size() - 1));
                        notification.markRetrying("Delivery failed.", Instant.now().plusSeconds(delayMinutes * 60));
                    }
                    notificationRepository.save(notification);
                });
        return null;
    }

    private boolean isCurrentAttempt(BudgetNotification notification, DeliveryDetails delivery) {
        return notification.getStatus() == NotificationStatus.RETRYING
                && notification.getAttemptCount() == delivery.attemptCount();
    }

    private record DeliveryDetails(
            Long notificationId,
            int attemptCount,
            String recipientEmail,
            String category,
            String period,
            String thresholdPercent,
            String actualSpending,
            String budgetLimit) {
    }
}
