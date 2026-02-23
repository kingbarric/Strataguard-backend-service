package com.strataguard.service.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.notification.SendNotificationRequest;
import com.strataguard.core.entity.*;
import com.strataguard.core.enums.ChargeType;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.infrastructure.repository.*;
import com.strataguard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeReminderScheduler {

    private final ChargeInvoiceRepository invoiceRepository;
    private final EstateChargeRepository estateChargeRepository;
    private final TenantChargeRepository tenantChargeRepository;
    private final ChargeReminderLogRepository reminderLogRepository;
    private final EstateRepository estateRepository;
    private final UnitRepository unitRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${billing.reminder.cron:0 0 8 * * *}")
    @Transactional
    public void processReminders() {
        log.info("Starting charge reminder processing");

        int remindersSent = 0;
        int overdueSent = 0;

        // 1. Upcoming due date reminders
        List<ChargeInvoice> upcomingInvoices = invoiceRepository.findAllUpcomingUnpaidInvoices();
        for (ChargeInvoice invoice : upcomingInvoices) {
            try {
                TenantContext.setTenantId(invoice.getTenantId());
                remindersSent += processInvoiceReminders(invoice);
            } catch (Exception e) {
                log.warn("Failed to process reminder for invoice {}: {}", invoice.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        // 2. Overdue reminders
        List<ChargeInvoice> overdueInvoices = invoiceRepository.findAllOverdueInvoices();
        for (ChargeInvoice invoice : overdueInvoices) {
            try {
                TenantContext.setTenantId(invoice.getTenantId());
                overdueSent += processOverdueReminder(invoice);
            } catch (Exception e) {
                log.warn("Failed to process overdue reminder for invoice {}: {}", invoice.getId(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Reminder processing complete: {} due reminders, {} overdue reminders sent", remindersSent, overdueSent);
    }

    private int processInvoiceReminders(ChargeInvoice invoice) {
        List<Integer> reminderDays = getReminderDays(invoice);
        if (reminderDays == null || reminderDays.isEmpty()) return 0;

        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), invoice.getDueDate());
        int sent = 0;

        for (int daysBefore : reminderDays) {
            if (daysUntilDue == daysBefore) {
                if (!reminderLogRepository.existsByInvoiceIdAndDaysBeforeAndTenantId(
                        invoice.getId(), daysBefore, invoice.getTenantId())) {
                    sendDueReminder(invoice, daysBefore);
                    logReminder(invoice, daysBefore);
                    sent++;
                }
            }
        }
        return sent;
    }

    private int processOverdueReminder(ChargeInvoice invoice) {
        // Use -1 as daysBefore marker for "overdue"
        if (!reminderLogRepository.existsByInvoiceIdAndDaysBeforeAndTenantId(
                invoice.getId(), -1, invoice.getTenantId())) {
            sendOverdueReminder(invoice);
            logReminder(invoice, -1);
            return 1;
        }
        return 0;
    }

    private List<Integer> getReminderDays(ChargeInvoice invoice) {
        // 1. Try charge-specific settings
        if (invoice.getChargeType() == ChargeType.ESTATE_CHARGE) {
            List<Integer> days = estateChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .map(EstateCharge::getReminderDaysBefore)
                    .orElse(null);
            if (days != null && !days.isEmpty()) return days;
        } else if (invoice.getChargeType() == ChargeType.TENANT_CHARGE) {
            List<Integer> days = tenantChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .map(TenantCharge::getReminderDaysBefore)
                    .orElse(null);
            if (days != null && !days.isEmpty()) return days;
        }

        // 2. Fall back to estate default
        return getEstateDefault(invoice);
    }

    private List<Integer> getEstateDefault(ChargeInvoice invoice) {
        UUID estateId = getEstateIdFromInvoice(invoice);
        if (estateId == null) return Collections.emptyList();

        return estateRepository.findByIdAndTenantId(estateId, invoice.getTenantId())
                .map(estate -> parseDefaultReminderDays(estate.getSettings()))
                .orElse(Collections.emptyList());
    }

    private UUID getEstateIdFromInvoice(ChargeInvoice invoice) {
        if (invoice.getChargeType() == ChargeType.ESTATE_CHARGE) {
            return estateChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .map(EstateCharge::getEstateId)
                    .orElse(null);
        } else if (invoice.getChargeType() == ChargeType.TENANT_CHARGE) {
            return tenantChargeRepository.findByIdAndTenantId(invoice.getChargeId(), invoice.getTenantId())
                    .map(TenantCharge::getEstateId)
                    .orElse(null);
        }
        // For UTILITY type, try to get estate from unit
        return unitRepository.findByIdAndTenantId(invoice.getUnitId(), invoice.getTenantId())
                .map(com.strataguard.core.entity.Unit::getEstateId)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Integer> parseDefaultReminderDays(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) return Collections.emptyList();
        try {
            Map<String, Object> settings = objectMapper.readValue(settingsJson, Map.class);
            Object days = settings.get("defaultReminderDays");
            if (days instanceof List<?> list) {
                return list.stream()
                        .map(d -> d instanceof Number n ? n.intValue() : Integer.parseInt(d.toString()))
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Failed to parse estate settings for reminder days: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private void sendDueReminder(ChargeInvoice invoice, int daysBefore) {
        if (invoice.getResidentId() == null) return;
        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(invoice.getResidentId())
                    .type(NotificationType.CHARGE_DUE_REMINDER)
                    .title("Payment Reminder")
                    .body("Your invoice " + invoice.getInvoiceNumber() + " of " + invoice.getTotalAmount() +
                            " is due in " + daysBefore + " day(s).")
                    .data(Map.of(
                            "invoiceId", invoice.getId().toString(),
                            "invoiceNumber", invoice.getInvoiceNumber(),
                            "amount", invoice.getTotalAmount().toString(),
                            "dueDate", invoice.getDueDate().toString(),
                            "daysBefore", String.valueOf(daysBefore)
                    ))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send due reminder for invoice {}: {}", invoice.getId(), e.getMessage());
        }
    }

    private void sendOverdueReminder(ChargeInvoice invoice) {
        if (invoice.getResidentId() == null) return;
        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientId(invoice.getResidentId())
                    .type(NotificationType.CHARGE_OVERDUE)
                    .title("Overdue Payment Notice")
                    .body("Your invoice " + invoice.getInvoiceNumber() + " of " + invoice.getTotalAmount() +
                            " was due on " + invoice.getDueDate() + " and is now overdue.")
                    .data(Map.of(
                            "invoiceId", invoice.getId().toString(),
                            "invoiceNumber", invoice.getInvoiceNumber(),
                            "amount", invoice.getTotalAmount().toString(),
                            "dueDate", invoice.getDueDate().toString()
                    ))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to send overdue reminder for invoice {}: {}", invoice.getId(), e.getMessage());
        }
    }

    private void logReminder(ChargeInvoice invoice, int daysBefore) {
        ChargeReminderLog logEntry = new ChargeReminderLog();
        logEntry.setTenantId(invoice.getTenantId());
        logEntry.setInvoiceId(invoice.getId());
        logEntry.setDaysBefore(daysBefore);
        logEntry.setSentAt(Instant.now());
        reminderLogRepository.save(logEntry);
    }
}
