package ru.practicum.event.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EventStatsResponseDto;
import ru.practicum.StatClient;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryRepository;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.mapper.LocationMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.JpaSpecifications;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestStatus;
import ru.practicum.request.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class EventAdminServiceImpl implements EventAdminService {

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    RequestRepository requestRepository;
    CommentRepository commentRepository;
    StatClient statClient;

    // Поиск событий
    @Override
    public List<EventFullDto> getAllEventsByParams(EventAdminParams params) {
        Pageable pageable = PageRequest.of(
                params.getFrom().intValue() / params.getSize().intValue(),
                params.getSize().intValue()
        );
        List<Event> events = eventRepository.findAll(JpaSpecifications.adminFilters(params), pageable).getContent();

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestRepository.getConfirmedRequestsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));
        Map<Long,Long> viewsMap = getViewsForListEvents(eventIds);
        Map<Long, List<CommentDto>> commentsMap = getCommentsForListEvents(events);

        return events.stream()
                .map(e -> EventMapper.toEventFullDto(e, confirmedRequestsMap.get(e.getId()),
                        viewsMap.get(e.getId()), commentsMap.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    // Редактирование данных события и его статуса (отклонение/публикация).
    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventDto updateEventDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateEventDto.getCategory() != null) {
            Category category = categoryRepository.findById(updateEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateEventDto.getCategory() + " not found"));
            event.setCategory(category);
        }

        if (updateEventDto.getTitle() != null) event.setTitle(updateEventDto.getTitle());
        if (updateEventDto.getAnnotation() != null) event.setAnnotation(updateEventDto.getAnnotation());
        if (updateEventDto.getDescription() != null) event.setDescription(updateEventDto.getDescription());
        if (updateEventDto.getLocation() != null)
            event.setLocation(LocationMapper.toEntity(updateEventDto.getLocation()));
        if (updateEventDto.getPaid() != null) event.setPaid(updateEventDto.getPaid());
        if (updateEventDto.getParticipantLimit() != null)
            event.setParticipantLimit(updateEventDto.getParticipantLimit());
        if (updateEventDto.getRequestModeration() != null)
            event.setRequestModeration(updateEventDto.getRequestModeration());
        if (updateEventDto.getEventDate() != null) event.setEventDate(updateEventDto.getEventDate());

        if (Objects.equals(updateEventDto.getStateAction(), StateAction.REJECT_EVENT)) {
            // событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)
            if (Objects.equals(event.getState(), State.PUBLISHED)) {
                throw new ConflictException("Event in PUBLISHED state can not be rejected");
            }
            event.setState(State.CANCELED);
        } else if (Objects.equals(updateEventDto.getStateAction(), StateAction.PUBLISH_EVENT)) {
            // дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (Ожидается код ошибки 409)
            if (LocalDateTime.now().plusHours(1).isAfter(event.getEventDate())) {
                throw new ConflictException("Event time must be at least 1 hours from publish time");
            }
            // событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)
            if (!Objects.equals(event.getState(), State.PENDING)) {
                throw new ConflictException("Event should be in PENDING state");
            }
            event.setState(State.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }

        eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        Long views = getViews(eventId);
        List<CommentDto> commentDtoList = commentRepository.findAllByEvent_Id(eventId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
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

    private Map<Long, List<CommentDto>> getCommentsForListEvents(List<Event> events) {
        Map<Long, List<CommentDto>> commentDtoMap = new HashMap<>();
        List<CommentDto> commentList = commentRepository.findCommentsByEventIn(events).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
        for (CommentDto comment : commentList) {
            if (!commentDtoMap.containsKey(comment.getEventId())) {
                commentDtoMap.put(comment.getEventId(), List.of(comment));
            } else {
                commentDtoMap.get(comment.getEventId()).add(comment);
            }
        }
        return commentDtoMap;
    }

}
