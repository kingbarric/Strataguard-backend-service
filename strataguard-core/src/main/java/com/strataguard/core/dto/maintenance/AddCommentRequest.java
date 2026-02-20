package com.strataguard.core.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;
    private String attachmentUrls;
    private boolean internal;
}
