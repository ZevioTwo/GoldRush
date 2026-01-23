package net.coding.template.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNoGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public String generate(String prefix) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int seq = COUNTER.updateAndGet(i -> i >= 999 ? 1 : i + 1);
        return String.format("%s%s%03d", prefix, timestamp, seq);
    }
}
