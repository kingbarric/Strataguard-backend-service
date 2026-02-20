package com.strataguard.core.entity;

import com.strataguard.core.enums.AmenityStatus;
import com.strataguard.core.enums.AmenityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "amenities", indexes = {
        @Index(name = "idx_amenities_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_amenities_estate_id", columnList = "estate_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Amenity extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "amenity_type", nullable = false)
    private AmenityType amenityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmenityStatus status = AmenityStatus.ACTIVE;

    private Integer capacity;

    @Column(name = "price_per_hour", precision = 15, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "price_per_session", precision = 15, scale = 2)
    private BigDecimal pricePerSession;

    @Column(name = "requires_booking", nullable = false)
    private boolean requiresBooking = true;

    @Column(name = "max_booking_duration_hours")
    private Integer maxBookingDurationHours;

    @Column(name = "min_booking_duration_hours")
    private Integer minBookingDurationHours;

    @Column(name = "advance_booking_days")
    private Integer advanceBookingDays;

    @Column(name = "cancellation_hours_before")
    private Integer cancellationHoursBefore;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "operating_days")
    private String operatingDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", columnDefinition = "jsonb")
    private String rules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private String photoUrls;

    @Column(name = "contact_info")
    private String contactInfo;

    @Column(nullable = false)
    private boolean active = true;
}
