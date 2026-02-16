package com.strataguard.core.util;

import com.strataguard.core.dto.vehicle.VehicleCsvRow;
import com.strataguard.core.enums.VehicleType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CsvParserUtils {

    private CsvParserUtils() {
    }

    public static List<VehicleCsvRow> parseVehicleCsv(InputStream inputStream) throws IOException {
        List<VehicleCsvRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 5) {
                    throw new IllegalArgumentException(
                            "Line " + lineNumber + ": expected at least 5 fields (residentId,plateNumber,make,model,vehicleType) but found " + fields.length);
                }

                VehicleCsvRow row = VehicleCsvRow.builder()
                        .residentId(UUID.fromString(fields[0].trim()))
                        .plateNumber(fields[1].trim())
                        .make(fields[2].trim())
                        .model(fields[3].trim())
                        .vehicleType(VehicleType.valueOf(fields[4].trim().toUpperCase()))
                        .color(fields.length > 5 ? fields[5].trim() : null)
                        .stickerNumber(fields.length > 6 ? fields[6].trim() : null)
                        .build();

                rows.add(row);
            }
        }

        return rows;
    }
}
