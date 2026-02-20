package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "maintenance_comments", indexes = {
        @Index(name = "idx_maintenance_comments_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_maintenance_comments_request_id", columnList = "request_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceComment extends BaseEntity {

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_role", nullable = false)
    private String authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachment_urls", columnDefinition = "jsonb")
    private String attachmentUrls;

    @Column(name = "is_internal", nullable = false)
    private boolean internal = false;
}
