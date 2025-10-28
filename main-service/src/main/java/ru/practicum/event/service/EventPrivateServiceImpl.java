package ru.practicum.event.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestStatus;
import ru.practicum.request.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPrivateServiceImpl implements EventPrivateService {

    UserRepository userRepository;
    CategoryRepository categoryRepository;
    EventRepository eventRepository;
    RequestRepository requestRepository;
    CommentRepository commentRepository;
    StatClient statClient;

    // Добавление нового события
    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        Event newEvent = EventMapper.toEvent(newEventDto, initiator, category);
        eventRepository.save(newEvent);
        return EventMapper.toEventFullDto(newEvent, 0L, 0L, List.of());
    }

    // Получение полной информации о событии добавленном текущим пользователем
    @Override
    public EventFullDto getEventByUserIdAndEventId(Long userId, Long eventId) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!Objects.equals(initiator.getId(), event.getInitiator().getId())) {
            throw new ConflictException("User " + userId + " is not an initiator of event " + eventId, "Forbidden action");
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);
        Long views = getViews(eventId);
        List<CommentDto> commentDtoList = commentRepository.findAllByEvent_Id(eventId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
        return EventMapper.toEventFullDto(event, confirmedRequests, views, commentDtoList);
    }

    // Получение событий, добавленных текущим пользователем
    @Override
    public List<EventShortDto> getEventsByUserId(Long userId, Long from, Long size) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Pageable pageable = PageRequest.of(
                from.intValue() / size.intValue(),
                size.intValue(),
                Sort.by("eventDate").descending()
        );

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsMap = requestRepository.getConfirmedRequestsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));
        Map<Long,Long> viewsMap = getViewsForListEvents(eventIds);

        return events.stream()
                .map(e -> EventMapper.toEventShortDto(e, confirmedRequestsMap.get(e.getId()), viewsMap.get(e.getId())))
                .toList();
    }

    // Изменение события добавленного текущим пользователем
    @Override
    @Transactional
    public EventFullDto updateEventByUserIdAndEventId(Long userId, Long eventId, UpdateEventDto updateEventDto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!Objects.equals(initiator.getId(), event.getInitiator().getId())) {
            throw new ConflictException("User " + userId + " is not an initiator of event " + eventId, "Forbidden action");
        }

        // изменить можно только отмененные события или события в состоянии ожидания модерации (Ожидается код ошибки 409)
        if (event.getState() != State.PENDING && event.getState() != State.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        // дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента (Ожидается код ошибки 409)
        if (updateEventDto.getEventDate() != null &&
                updateEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

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

        if (Objects.equals(updateEventDto.getStateAction(), StateAction.CANCEL_REVIEW)) {
            event.setState(State.CANCELED);
        } else if (Objects.equals(updateEventDto.getStateAction(), StateAction.SEND_TO_REVIEW)) {
            event.setState(State.PENDING);
        }

        eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);
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
}
