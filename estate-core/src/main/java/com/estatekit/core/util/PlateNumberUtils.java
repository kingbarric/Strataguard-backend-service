package com.estatekit.core.util;

public final class PlateNumberUtils {

    private PlateNumberUtils() {
    }

    public static String normalize(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            return plateNumber;
        }
        return plateNumber.toUpperCase()
                .replaceAll("[\\s-]", "");
    }
}
