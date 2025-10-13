package ru.practicum.service;

import ru.practicum.EventHitDto;
import ru.practicum.EventStatsResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {

    void hit(EventHitDto eventHitDto);

    List<EventStatsResponseDto> getStats(LocalDateTime start, LocalDateTime end,List<String> uris, boolean unique);

}
