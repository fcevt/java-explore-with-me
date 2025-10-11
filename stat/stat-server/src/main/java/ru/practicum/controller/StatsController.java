package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EventHitDto;
import ru.practicum.EventStatsResponseDto;
import ru.practicum.exception.IllegalArgumentException;
import ru.practicum.service.StatService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatsController {

    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void hit(@RequestBody @Valid EventHitDto eventHitDto) {
        log.info("hit {}", eventHitDto);
        statService.hit(eventHitDto);
    }

    @GetMapping("/stats")
    public List<EventStatsResponseDto> getStats(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        log.info("Getting stats with args: {}, {}, {}, {}",start, end, uris, unique);
        return statService.getStats(start, end, uris, unique);
    }
}
