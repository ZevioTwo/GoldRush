package net.coding.template.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ContractNoGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final String PREFIX = "MJ";

    public String generate() {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        int seq = counter.updateAndGet(i -> i >= 9999 ? 1 : i + 1);
        return String.format("%s-%s-%04d", PREFIX, dateStr, seq);
    }
}
