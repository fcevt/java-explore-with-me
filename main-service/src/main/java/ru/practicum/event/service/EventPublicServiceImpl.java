package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EventHitDto;
import ru.practicum.EventStatsResponseDto;
import ru.practicum.StatClient;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.JpaSpecifications;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestStatus;
import ru.practicum.request.RequestRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class EventPublicServiceImpl implements EventPublicService {

    StatClient statClient;
    EventRepository eventRepository;
    RequestRepository requestRepository;
    CommentRepository commentRepository;

    // Получение событий с возможностью фильтрации
    @Override
    public List<EventShortDto> getAllEventsByParams(EventParams params, HttpServletRequest request) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null && params.getRangeEnd().isBefore(params.getRangeStart())) {
            throw new BadRequestException("rangeStart should be before rangeEnd");
        }

        // если в запросе не указан диапазон дат [rangeStart-rangeEnd], то нужно выгружать события, которые произойдут позже текущей даты и времени
        if (params.getRangeStart() == null) params.setRangeStart(LocalDateTime.now());

        // сортировка и пагинация
        Sort sort = Sort.by(Sort.Direction.ASC, "eventDate");
        if (EventSort.VIEWS.equals(params.getEventSort())) sort = Sort.by(Sort.Direction.DESC, "views");
        PageRequest pageRequest = PageRequest.of(params.getFrom().intValue() / params.getSize().intValue(),
                params.getSize().intValue(), sort);

        Page<Event> events = eventRepository.findAll(JpaSpecifications.publicFilters(params), pageRequest);
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        // информация о каждом событии должна включать в себя количество просмотров и количество уже одобренных заявок на участие
        Map<Long, Long> confirmedRequestsMap = requestRepository.getConfirmedRequestsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));
        //получение просмотров
        Map<Long,Long> viewsMap = getViewsForListEvents(eventIds);
        // информацию о том, что по этому эндпоинту был осуществлен и обработан запрос, нужно сохранить в сервисе статистики
        statClient.hit(EventHitDto.builder()
                .ip(request.getRemoteAddr())
                .uri(request.getRequestURI())
                .app("ewm-main-service")
                .timestamp(LocalDateTime.now())
                .build());

        return events.stream()
                .map(e -> EventMapper.toEventShortDto(e, confirmedRequestsMap.get(e.getId()), viewsMap.get(e.getId())))
                .toList();
    }

    // Получение подробной информации об опубликованном событии по его идентификатору
    @Override
    @Transactional
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        // событие должно быть опубликовано
        Event event = eventRepository.findByIdAndState(eventId, State.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // информация о событии должна включать в себя количество просмотров и количество подтвержденных запросов
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        Long views = getViews(eventId);

        //получаем список комментов
        List<CommentDto> commentDtoList = commentRepository.findAllByEvent_Id(eventId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
        // информацию о том, что по этому эндпоинту был осуществлен и обработан запрос, нужно сохранить в сервисе статистики
        statClient.hit(EventHitDto.builder()
                .ip(request.getRemoteAddr())
                .uri(request.getRequestURI())
                .app("ewm-main-service")
                .timestamp(LocalDateTime.now())
                .build());

        return EventMapper.toEventFullDto(event, confirmedRequests, views, commentDtoList);
    }

    private Map<Long, Long> getViewsForListEvents(List<Long> eventIds) {
        String uri = "/events/";
        List<String> uris = eventIds.stream()
                .map(id -> new StringBuilder(uri).append(id.toString()).toString())
                .toList();
        List<EventStatsResponseDto> listStats = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1), uris, true);
        Map<Long, Long> viewsMap = new HashMap<>();
        for (EventStatsResponseDto stat : listStats) {
            viewsMap.put(Long.valueOf(stat.getUri().split("/")[2]), stat.getHits());
        }
        return viewsMap;
    }

    private Long getViews(long eventId) {
        Long views;
        String uri = new StringBuilder("/events/").append(eventId).toString();
        List<EventStatsResponseDto> listStats = statClient.getStats(
                LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1), List.of(uri), true);
        if (listStats == null || listStats.isEmpty()) {
            views = 0L;
        } else {
            views = listStats.get(0).getHits();
        }
        return views;
    }

}