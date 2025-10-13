package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.EventHitDto;
import ru.practicum.EventStatsResponseDto;
import ru.practicum.model.Stat;
import ru.practicum.repository.StatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private final StatRepository statRepository;

    public void hit(EventHitDto eventHitDto) {
       Stat stat = statRepository.save(
                Stat.builder()
                        .app(eventHitDto.getApp())
                        .uri(eventHitDto.getUri())
                        .ip(eventHitDto.getIp())
                        .timestamp(eventHitDto.getTimestamp())
                        .build()

       );
    }

    @Override
    public List<EventStatsResponseDto> getStats(LocalDateTime start,
                                                LocalDateTime end, List<String> uris, boolean unique) {
        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return statRepository.findAllByTimestampBetweenStartAndEndWithUniqueIp(start, end);
            }
            return statRepository.getStatByTimestampBetweenAndNotUniqueIp(start, end);
        } else {
            if (unique) {
                log.info("Found unique uris: {}", uris);
                return statRepository.getStatByTimestampBetweenAndUriInAndUniqueIp(start, end, uris);
            }
            log.info("Found uris: {}", uris);
            return statRepository.getStatByTimestampBetweenAndNotUniqueIpAndUriIn(start, end, uris);
        }
    }
}
