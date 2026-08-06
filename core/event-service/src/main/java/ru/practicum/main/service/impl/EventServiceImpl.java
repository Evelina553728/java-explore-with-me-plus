package ru.practicum.main.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.main.config.ConfigUtil;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.dto.event.UpdateEventAdminDto;
import ru.practicum.main.dto.event.UpdateEventUserDto;
import ru.practicum.main.enumeration.EventState;
import ru.practicum.main.enumeration.SortValue;
import ru.practicum.main.enumeration.StateActionForAdmin;
import ru.practicum.main.enumeration.StateActionForUser;
import ru.practicum.main.exception.EventNotExistException;
import ru.practicum.main.exception.WrongTimeException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.CategoryService;
import ru.practicum.main.service.EventParticipationService;
import ru.practicum.main.service.EventService;
import ru.practicum.main.utils.EventPredicateUtil;
import ru.practicum.main.utils.EventUpdater;
import ru.practicum.main.utils.EventValidator;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventValidator eventValidator;
    private final EventUpdater eventUpdater;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final UserClient userClient;
    private final EventParticipationService eventParticipationService;
    private final StatsClient statsClient;
    private final EntityManager entityManager;

    private final String datePattern = ConfigUtil.DATE;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(datePattern);

    @Override
    @Transactional
    public EventFullDto createEvent(
            Long userId,
            NewEventDto newEventDto
    ) {
        eventValidator.validateNewEventDate(
                newEventDto.getEventDate()
        );

        Category category =
                categoryService.getCategoryModelById(
                        newEventDto.getCategory()
                );

        Event event = eventMapper.toEventModel(newEventDto);
        event.setCategory(category);

        UserDto user = userClient.getUserById(userId);

        event.setInitiatorId(user.id());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);

        return setConfirmedRequest(savedEvent, user);
    }

    @Override
    public List<EventShortDto> getEvents(
            Long userId,
            Integer from,
            Integer size
    ) {
        Pageable page = PageRequest.of(from / size, size);

        List<Event> events =
                eventRepository.findAllByInitiatorId(
                        userId,
                        page
                ).toList();

        return eventMapper.toEventShortDtoList(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(
            Long eventId,
            UpdateEventAdminDto dto
    ) {
        Event event = getEventById(eventId);

        if (dto == null) {
            return setConfirmedRequest(event);
        }

        eventUpdater.applyAdminUpdate(event, dto);

        if (dto.getEventDate() != null) {
            eventValidator.validateEventDateUpdate(
                    dto.getEventDate()
            );

            event.setEventDate(dto.getEventDate());
        }

        if (dto.getStateAction() != null) {
            eventValidator.validateAdminPublish(event);

            if (dto.getStateAction()
                    == StateActionForAdmin.PUBLISH_EVENT) {

                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);

        return setConfirmedRequest(savedEvent);
    }

    @Override
    @Transactional
    public EventFullDto publishEvent(Long eventId) {
        UpdateEventAdminDto dto = new UpdateEventAdminDto();

        dto.setStateAction(
                StateActionForAdmin.PUBLISH_EVENT
        );

        return updateEvent(eventId, dto);
    }

    @Override
    @Transactional
    public EventFullDto rejectEvent(Long eventId) {
        UpdateEventAdminDto dto = new UpdateEventAdminDto();

        dto.setStateAction(
                StateActionForAdmin.REJECT_EVENT
        );

        return updateEvent(eventId, dto);
    }

    @Override
    public List<EventFullDto> getPendingEvents(
            Integer from,
            Integer size
    ) {
        return getEventsWithParamsByAdmin(
                null,
                EventState.PENDING,
                null,
                null,
                null,
                from,
                size
        );
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(
            Long userId,
            Long eventId,
            UpdateEventUserDto dto
    ) {
        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(
                        () -> new EventNotExistException("")
                );

        eventValidator.validateUserUpdate(event);

        if (dto == null) {
            return setConfirmedRequest(event);
        }

        eventUpdater.applyUserUpdate(event, dto);

        if (dto.getEventDate() != null) {
            eventValidator.validateEventDateUpdate(
                    dto.getEventDate()
            );

            event.setEventDate(dto.getEventDate());
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction()
                    == StateActionForUser.SEND_TO_REVIEW) {

                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        Event savedEvent = eventRepository.save(event);

        return setConfirmedRequest(savedEvent);
    }

    @Override
    public EventFullDto getEventByUser(
            Long userId,
            Long eventId
    ) {
        Event event = eventRepository
                .findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(
                        () -> new EventNotExistException("")
                );

        return setConfirmedRequest(event);
    }

    @Override
    public List<EventFullDto> getEventsWithParamsByAdmin(
            List<Long> users,
            EventState states,
            List<Long> categoriesId,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Integer from,
            Integer size
    ) {
        CriteriaBuilder builder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> query =
                builder.createQuery(Event.class);

        Root<Event> root = query.from(Event.class);

        Predicate criteria = builder.conjunction();

        List<Category> categories = null;

        if (categoriesId != null && !categoriesId.isEmpty()) {
            categories =
                    categoryService.getCategoriesByIds(
                            categoriesId
                    );
        }

        criteria = EventPredicateUtil.addCategoryFilter(
                criteria,
                builder,
                root,
                categoriesId,
                categories
        );

        criteria = EventPredicateUtil.addUserFilter(
                criteria,
                builder,
                root,
                users
        );

        criteria = EventPredicateUtil.addStateFilter(
                criteria,
                builder,
                root,
                states
        );

        criteria = EventPredicateUtil.addDateFilter(
                criteria,
                builder,
                root,
                rangeStart,
                "eventDate",
                true
        );

        criteria = EventPredicateUtil.addDateFilter(
                criteria,
                builder,
                root,
                rangeEnd,
                "eventDate",
                false
        );

        query.select(root)
                .where(criteria)
                .orderBy(
                        builder.desc(root.get("createdOn"))
                );

        List<Event> events = entityManager
                .createQuery(query)
                .setFirstResult(from)
                .setMaxResults(size)
                .getResultList();

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventFullDto> dtos =
                eventUpdater.processEvents(events);

        setView(dtos);

        return dtos;
    }

    @Override
    public List<EventFullDto> getEventsWithParamsByUser(
            String text,
            List<Long> users,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            SortValue sort,
            Integer from,
            Integer size,
            String ip,
            String uri,
            List<String> states
    ) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        try {
            if (rangeStart != null) {
                start = LocalDateTime.parse(
                        rangeStart,
                        dateFormatter
                );
            }

            if (rangeEnd != null) {
                end = LocalDateTime.parse(
                        rangeEnd,
                        dateFormatter
                );
            }
        } catch (DateTimeParseException exception) {
            log.debug(
                    "Неверный формат даты: {}",
                    exception.getMessage()
            );
        }

        checkDateTime(start, end);

        CriteriaBuilder builder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> query =
                builder.createQuery(Event.class);

        Root<Event> root = query.from(Event.class);

        Predicate predicate = builder.conjunction();

        predicate = EventPredicateUtil.addTextFilter(
                predicate,
                builder,
                root,
                text
        );

        predicate = EventPredicateUtil.addUserFilter(
                predicate,
                builder,
                root,
                users
        );

        List<Category> categoryEntities = null;

        if (categories != null && !categories.isEmpty()) {
            categoryEntities =
                    categoryService.getCategoriesByIds(
                            categories
                    );
        }

        predicate = EventPredicateUtil.addCategoryFilter(
                predicate,
                builder,
                root,
                categories,
                categoryEntities
        );

        predicate = EventPredicateUtil.addPaidFilter(
                predicate,
                builder,
                root,
                paid
        );

        predicate = EventPredicateUtil.addDateFilter(
                predicate,
                builder,
                root,
                start,
                "eventDate",
                true
        );

        predicate = EventPredicateUtil.addDateFilter(
                predicate,
                builder,
                root,
                end,
                "eventDate",
                false
        );

        predicate = EventPredicateUtil.addStateFilter(
                predicate,
                builder,
                root,
                states
        );

        query.select(root).where(predicate);

        if (sort != null) {
            if (sort == SortValue.EVENT_DATE) {
                query.orderBy(
                        builder.asc(root.get("eventDate"))
                );
            } else {
                query.orderBy(
                        builder.desc(root.get("views"))
                );
            }
        } else {
            query.orderBy(
                    builder.asc(root.get("eventDate"))
            );
        }

        List<Event> events = entityManager
                .createQuery(query)
                .setFirstResult(from != null ? from : 0)
                .setMaxResults(size != null ? size : 10)
                .getResultList();

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventFullDto> dtos =
                eventUpdater.processEvents(events);

        setView(dtos);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            dtos = dtos.stream()
                    .filter(dto ->
                            dto.getConfirmedRequests()
                                    < dto.getParticipantLimit()
                    )
                    .collect(Collectors.toList());
        }

        sendStat(events, ip, uri);

        return dtos;
    }

    @Override
    public EventFullDto getEvent(
            Long id,
            String ip,
            String uri
    ) {
        Event event = eventRepository
                .findByIdAndPublishedOnIsNotNull(id)
                .orElseThrow(
                        () -> new EventNotExistException(
                                String.format(
                                        "Can't find event with id = %s "
                                                + "event doesn't exist",
                                        id
                                )
                        )
                );

        EventFullDto eventFullDto =
                setConfirmedRequest(event);

        sendStat(eventFullDto, ip, uri);

        Long views = setView(event);

        eventFullDto.setViews(
                views != null ? views + 1 : 1L
        );

        return eventFullDto;
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    @Override
    public List<Event> getEventsByIds(
            List<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        return eventRepository.findAllByIdIn(eventIds);
    }

    @Override
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(
                        () -> new EventNotExistException(
                                String.format(
                                        "Event with id=%s was not found",
                                        eventId
                                )
                        )
                );
    }

    private void sendStat(
            EventFullDto event,
            String ip,
            String uri
    ) {
        LocalDateTime now = LocalDateTime.now();
        String nameService = "main-service";

        EndpointHitDto requestDto = new EndpointHitDto();

        requestDto.setTimestamp(
                now.format(dateFormatter)
        );

        requestDto.setUri("/events");
        requestDto.setApp(nameService);
        requestDto.setIp(ip);

        statsClient.addStats(requestDto);

        sendStatForTheEvent(
                event.getId(),
                ip,
                now,
                nameService
        );
    }

    private void sendStat(
            List<Event> events,
            String ip,
            String uri
    ) {
        LocalDateTime now = LocalDateTime.now();
        String nameService = "main-service";

        EndpointHitDto requestDto = new EndpointHitDto();

        requestDto.setTimestamp(
                now.format(dateFormatter)
        );

        requestDto.setUri("/events");
        requestDto.setApp(nameService);
        requestDto.setIp(ip);

        statsClient.addStats(requestDto);
    }

    public void setView(List<EventFullDto> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        LocalDateTime start;

        try {
            start = LocalDateTime.parse(
                    events.getFirst().getCreatedOn(),
                    dateFormatter
            );
        } catch (Exception exception) {
            start = LocalDateTime.now().minusYears(1);
        }

        List<String> uris = new ArrayList<>();

        Map<String, EventFullDto> eventsUri =
                new HashMap<>();

        for (EventFullDto event : events) {
            try {
                LocalDateTime createdOn =
                        LocalDateTime.parse(
                                event.getCreatedOn(),
                                dateFormatter
                        );

                if (createdOn.isBefore(start)) {
                    start = createdOn;
                }
            } catch (Exception exception) {
                log.debug(
                        "Ошибка парсинга createdOn "
                                + "для события id={}: {}",
                        event.getId(),
                        exception.getMessage()
                );
            }

            String eventUri =
                    "/events/" + event.getId();

            uris.add(eventUri);
            eventsUri.put(eventUri, event);

            event.setViews(0L);
        }

        String startTime =
                start.format(dateFormatter);

        String endTime =
                LocalDateTime.now().format(dateFormatter);

        List<ViewStatsDto> stats =
                getStats(
                        startTime,
                        endTime,
                        uris
                );

        stats.forEach(stat -> {
            EventFullDto dto =
                    eventsUri.get(stat.getUri());

            if (dto != null) {
                dto.setViews(stat.getHits());
            }
        });
    }

    public Long setView(Event event) {
        if (event == null || event.getCreatedOn() == null) {
            return 0L;
        }

        String startTime =
                event.getCreatedOn().format(dateFormatter);

        String endTime =
                LocalDateTime.now().format(dateFormatter);

        List<String> uris =
                List.of("/events/" + event.getId());

        List<ViewStatsDto> stats =
                getStats(
                        startTime,
                        endTime,
                        uris
                );

        if (stats.size() == 1) {
            return stats.getFirst().getHits();
        }

        return 0L;
    }

    private void checkDateTime(
            LocalDateTime start,
            LocalDateTime end
    ) {
        LocalDateTime actualStart =
                start != null
                        ? start
                        : LocalDateTime.now().minusYears(100);

        LocalDateTime actualEnd =
                end != null
                        ? end
                        : LocalDateTime.now();

        if (actualStart.isAfter(actualEnd)) {
            throw new WrongTimeException(
                    "Некорректный запрос. "
                            + "Дата окончания события "
                            + "задана позже даты старта"
            );
        }
    }

    private List<ViewStatsDto> getStats(
            String startTime,
            String endTime,
            List<String> uris
    ) {
        return statsClient.getStats(
                startTime,
                endTime,
                uris,
                false
        );
    }

    private void sendStatForTheEvent(
            Long eventId,
            String ip,
            LocalDateTime now,
            String nameService
    ) {
        EndpointHitDto requestDto = new EndpointHitDto();

        requestDto.setTimestamp(
                now.format(dateFormatter)
        );

        requestDto.setUri("/events/" + eventId);
        requestDto.setApp(nameService);
        requestDto.setIp(ip);

        statsClient.addStats(requestDto);
    }

    private EventFullDto setConfirmedRequest(
            Event event
    ) {
        UserDto user =
                userClient.getUserById(
                        event.getInitiatorId()
                );

        return setConfirmedRequest(event, user);
    }

    private EventFullDto setConfirmedRequest(
            Event event,
            UserDto user
    ) {
        Long confirmed =
                eventParticipationService
                        .getConfirmedRequestsCountForEvent(
                                event.getId()
                        );

        EventFullDto eventFullDto =
                eventMapper.toEventFullDto(
                        event,
                        user
                );

        eventFullDto.setConfirmedRequests(confirmed);

        return eventFullDto;
    }
}