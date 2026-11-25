package com.skillchain.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts MySQL TIMESTAMP instants to the computer's local display time.
 */
public final class DatabaseTime {

    private DatabaseTime() {
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null
                ? null
                : timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
