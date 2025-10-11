package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.EventStatsResponseDto;
import ru.practicum.model.Stat;

import java.time.LocalDateTime;
import java.util.List;

public interface StatRepository extends JpaRepository<Stat, Long> {

    @Query("select new ru.practicum.EventStatsResponseDto(s.app, s.uri, count(distinct s.ip)) " +
            "from Stat as s " +
            "where s.timestamp between ?1 and ?2 " +
            "group by s.app, s.uri " +
            "order by count(distinct s.ip) desc")
    List<EventStatsResponseDto> findAllByTimestampBetweenStartAndEndWithUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.EventStatsResponseDto(s.app, s.uri, count(s.ip)) " +
            "from Stat as s " +
            "where s.timestamp between ?1 and ?2 " +
            "group by s.app, s.uri " +
            "order by count(s.ip) desc")
    List<EventStatsResponseDto> getStatByTimestampBetweenAndNotUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.EventStatsResponseDto(s.app, s.uri, count(distinct s.ip)) " +
            "from Stat as s " +
            "where s.timestamp between ?1 and ?2 and s.uri in ?3" +
            "group by s.app, s.uri " +
            "order by count(distinct s.ip) desc")
    List<EventStatsResponseDto> getStatByTimestampBetweenAndUriInAndUniqueIp(
            LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("select new ru.practicum.EventStatsResponseDto(s.app, s.uri, count(s.ip)) " +
            "from Stat as s " +
            "where s.uri in ?3 and s.timestamp between ?1 and ?2 " +
            "group by s.app, s.uri " +
            "order by count(s.ip) desc")
    List<EventStatsResponseDto> getStatByTimestampBetweenAndNotUniqueIpAndUriIn(
            LocalDateTime start, LocalDateTime end, List<String> uris);
}
