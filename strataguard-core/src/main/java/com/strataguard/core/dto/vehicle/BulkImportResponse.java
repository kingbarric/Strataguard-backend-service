package com.strataguard.core.dto.vehicle;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkImportResponse {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors;
}
