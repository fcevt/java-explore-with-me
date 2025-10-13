package ru.practicum;

import java.time.LocalDateTime;
import java.util.List;

public interface StatClient {

    void hit(EventHitDto eventHitDto);

    List<EventStatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
