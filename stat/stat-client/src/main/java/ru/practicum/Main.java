package ru.practicum;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        StatClient client = new StatClientImpl("http://localhost:9090");
        client.hit(EventHitDto.builder()
                .app("sdffs")
                .uri("dsdsd")
                .ip("123123")
                .timestamp(LocalDateTime.now())
                .build());
        List<EventStatsResponseDto> list = client.getStats(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), List.of(), false);
        System.out.println(list);
    }
}
