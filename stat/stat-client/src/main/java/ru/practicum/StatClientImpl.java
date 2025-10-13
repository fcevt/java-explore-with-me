package ru.practicum;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Service
public class StatClientImpl implements StatClient {
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestClient restClient;

    public StatClientImpl(@Value("${explore-with-me.stat-server.url}") String statUrl) {
        this.restClient = RestClient
                .builder()
                .baseUrl(statUrl)
                .build();
    }

    @Override
    public void hit(EventHitDto eventHitDto) {
        try {
            restClient
                    .post()
                    .uri("/hit")
                    .body(eventHitDto)
                    .contentType(APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Не удалось сохранить информацию о запросе к эндпоинту: {}", e.getMessage());
        }
    }

    @Override
    public List<EventStatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("/stats")
                    .queryParam("start", start.format(formatter))
                    .queryParam("end", end.format(formatter));

            if (uris != null && !uris.isEmpty()) {
                uriBuilder.queryParam("uris", String.join(",", uris));
            }
            if (unique != null) {
                uriBuilder.queryParam("unique", unique);
            }

            String uri = uriBuilder.build().toUriString();

            return restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Collection<EventStatsResponseDto>>(){})
                    .stream().toList();
        } catch (RestClientException e) {
            log.error("Не удалось получить статистику по посещениям: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
