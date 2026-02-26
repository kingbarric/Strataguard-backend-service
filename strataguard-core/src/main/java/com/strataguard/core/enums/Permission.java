package com.strataguard.core.enums;

public enum Permission {
    // Estate / Units
    ESTATE_READ("estate.read"),
    ESTATE_CREATE("estate.create"),
    ESTATE_UPDATE("estate.update"),
    ESTATE_DELETE("estate.delete"),
    UNIT_READ("unit.read"),
    UNIT_CREATE("unit.create"),
    UNIT_UPDATE("unit.update"),
    UNIT_DELETE("unit.delete"),

    // Tenancy / Residents
    TENANCY_READ("tenancy.read"),
    TENANCY_CREATE("tenancy.create"),
    TENANCY_UPDATE("tenancy.update"),
    TENANCY_TERMINATE("tenancy.terminate"),
    RESIDENT_READ("resident.read"),
    RESIDENT_CREATE("resident.create"),
    RESIDENT_UPDATE("resident.update"),
    RESIDENT_DELETE("resident.delete"),
    RESIDENT_LINK_USER("resident.link_user"),

    // Vehicles
    VEHICLE_READ("vehicle.read"),
    VEHICLE_CREATE("vehicle.create"),
    VEHICLE_UPDATE("vehicle.update"),
    VEHICLE_DELETE("vehicle.delete"),
    VEHICLE_IMPORT("vehicle.import"),

    // Charges / Billing
    CHARGE_READ("charge.read"),
    CHARGE_CREATE("charge.create"),
    CHARGE_UPDATE("charge.update"),
    CHARGE_DELETE("charge.delete"),
    CHARGE_EXCLUSION_MANAGE("charge.exclusion_manage"),
    LEVY_CREATE("levy.create"),
    LEVY_GENERATE("levy.generate"),
    PENALTY_APPLY("penalty.apply"),

    // Invoices
    INVOICE_READ("invoice.read"),
    INVOICE_CREATE("invoice.create"),
    INVOICE_UPDATE("invoice.update"),
    INVOICE_VOID("invoice.void"),
    INVOICE_EXPORT("invoice.export"),
    INVOICE_SUMMARY("invoice.summary"),

    // Payments
    PAYMENT_READ("payment.read"),
    PAYMENT_INITIATE("payment.initiate"),
    PAYMENT_VERIFY("payment.verify"),
    PAYMENT_REFUND("payment.refund"),
    PAYMENT_EXPORT("payment.export"),
    WALLET_READ("wallet.read"),
    WALLET_FUND("wallet.fund"),
    WALLET_TRANSFER("wallet.transfer"),

    // Gate / Security Operations
    GATE_ENTRY("gate.entry"),
    GATE_EXIT("gate.exit"),
    GATE_SESSION_READ("gate.session_read"),
    GATE_LOG_READ("gate.log_read"),
    GATE_MANUAL_OVERRIDE("gate.manual_override"),
    EXIT_PASS_GENERATE("exit_pass.generate"),
    EXIT_APPROVAL_CREATE("exit_approval.create"),
    EXIT_APPROVAL_MANAGE("exit_approval.manage"),

    // Visitors
    VISITOR_READ("visitor.read"),
    VISITOR_CREATE("visitor.create"),
    VISITOR_UPDATE("visitor.update"),
    VISITOR_DELETE("visitor.delete"),
    VISITOR_CHECKIN("visitor.checkin"),
    VISITOR_CHECKOUT("visitor.checkout"),
    VISITOR_ALL_READ("visitor.all_read"),
    VISIT_PASS_READ("visit_pass.read"),
    VISIT_PASS_CREATE("visit_pass.create"),
    VISIT_PASS_REVOKE("visit_pass.revoke"),

    // Blacklist
    BLACKLIST_READ("blacklist.read"),
    BLACKLIST_CREATE("blacklist.create"),
    BLACKLIST_UPDATE("blacklist.update"),
    BLACKLIST_DELETE("blacklist.delete"),
    BLACKLIST_CHECK("blacklist.check"),

    // Security Incidents
    INCIDENT_READ("incident.read"),
    INCIDENT_CREATE("incident.create"),
    INCIDENT_UPDATE("incident.update"),
    INCIDENT_CLOSE("incident.close"),

    // CCTV
    CCTV_READ("cctv.read"),
    CCTV_MANAGE("cctv.manage"),
    CCTV_LIVE_FEED("cctv.live_feed"),

    // Patrol
    PATROL_READ("patrol.read"),
    PATROL_MANAGE("patrol.manage"),
    PATROL_SCAN("patrol.scan"),

    // Emergency
    EMERGENCY_READ("emergency.read"),
    EMERGENCY_CREATE("emergency.create"),
    EMERGENCY_ACKNOWLEDGE("emergency.acknowledge"),

    // Staff Management
    STAFF_READ("staff.read"),
    STAFF_MANAGE("staff.manage"),

    // Maintenance
    MAINTENANCE_READ("maintenance.read"),
    MAINTENANCE_CREATE("maintenance.create"),
    MAINTENANCE_ASSIGN("maintenance.assign"),
    MAINTENANCE_CLOSE("maintenance.close"),
    MAINTENANCE_COMMENT("maintenance.comment"),
    MAINTENANCE_RATE("maintenance.rate"),

    // Amenities / Bookings
    AMENITY_READ("amenity.read"),
    AMENITY_CREATE("amenity.create"),
    AMENITY_UPDATE("amenity.update"),
    AMENITY_DELETE("amenity.delete"),
    BOOKING_READ("booking.read"),
    BOOKING_CREATE("booking.create"),
    BOOKING_APPROVE("booking.approve"),
    BOOKING_CANCEL("booking.cancel"),

    // Utilities
    UTILITY_METER_READ("utility.meter_read"),
    UTILITY_METER_MANAGE("utility.meter_manage"),
    UTILITY_READING_CREATE("utility.reading_create"),
    UTILITY_READING_READ("utility.reading_read"),
    UTILITY_COST_MANAGE("utility.cost_manage"),

    // Artisans / Vendors
    ARTISAN_READ("artisan.read"),
    ARTISAN_CREATE("artisan.create"),
    ARTISAN_UPDATE("artisan.update"),
    ARTISAN_DELETE("artisan.delete"),
    ARTISAN_RATE("artisan.rate"),
    ARTISAN_VERIFY("artisan.verify"),

    // Announcements
    ANNOUNCEMENT_READ("announcement.read"),
    ANNOUNCEMENT_CREATE("announcement.create"),
    ANNOUNCEMENT_UPDATE("announcement.update"),
    ANNOUNCEMENT_DELETE("announcement.delete"),
    ANNOUNCEMENT_PUBLISH("announcement.publish"),

    // Polls
    POLL_READ("poll.read"),
    POLL_CREATE("poll.create"),
    POLL_UPDATE("poll.update"),
    POLL_DELETE("poll.delete"),
    POLL_VOTE("poll.vote"),
    POLL_CLOSE("poll.close"),

    // Violations
    VIOLATION_READ("violation.read"),
    VIOLATION_CREATE("violation.create"),
    VIOLATION_UPDATE("violation.update"),
    VIOLATION_CLOSE("violation.close"),
    VIOLATION_APPEAL("violation.appeal"),
    VIOLATION_APPEAL_REVIEW("violation.appeal_review"),

    // Complaints
    COMPLAINT_READ("complaint.read"),
    COMPLAINT_CREATE("complaint.create"),
    COMPLAINT_UPDATE("complaint.update"),
    COMPLAINT_ASSIGN("complaint.assign"),
    COMPLAINT_CLOSE("complaint.close"),
    COMPLAINT_ESCALATE("complaint.escalate"),

    // Notifications
    NOTIFICATION_READ("notification.read"),
    NOTIFICATION_SEND("notification.send"),
    NOTIFICATION_TEMPLATE_MANAGE("notification.template_manage"),
    NOTIFICATION_PREFERENCE_MANAGE("notification.preference_manage"),

    // Chat
    CHAT_READ("chat.read"),
    CHAT_SEND("chat.send"),

    // Reporting
    REPORT_FINANCIAL("report.financial"),
    REPORT_SECURITY("report.security"),
    REPORT_MAINTENANCE("report.maintenance"),
    REPORT_OCCUPANCY("report.occupancy"),
    REPORT_GATE("report.gate"),

    // Audit
    AUDIT_READ("audit.read"),
    AUDIT_EXPORT("audit.export"),

    // Membership Management
    MEMBERSHIP_READ("membership.read"),
    MEMBERSHIP_CREATE("membership.create"),
    MEMBERSHIP_UPDATE("membership.update"),
    MEMBERSHIP_DELETE("membership.delete"),

    // Portfolio (Phase 6)
    PORTFOLIO_READ("portfolio.read"),
    PORTFOLIO_CREATE("portfolio.create"),
    PORTFOLIO_UPDATE("portfolio.update"),
    PORTFOLIO_DELETE("portfolio.delete"),
    PORTFOLIO_ASSIGN_ESTATE("portfolio.assign_estate");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Permission fromValue(String value) {
        for (Permission p : values()) {
            if (p.value.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown permission: " + value);
    }
}
