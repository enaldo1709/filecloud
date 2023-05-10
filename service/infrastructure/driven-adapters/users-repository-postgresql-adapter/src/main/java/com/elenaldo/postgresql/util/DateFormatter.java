package com.elenaldo.postgresql.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;


public class DateFormatter {
    private DateTimeFormatter format;
    private static DateFormatter instance;

    public DateFormatter() {
        
        format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS");
    }

    public static DateFormatter getInstance() {
        if (Objects.isNull(instance)) {
            instance = new DateFormatter();
        }
        return instance;
    }

    public String format(TemporalAccessor date) {
        return format.format(date);
    }

    public LocalDateTime parse(String date) {
        return LocalDateTime.from(format.parse(date));
    }
}
