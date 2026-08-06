package ru.practicum.main.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.main.config.ConfigUtil;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.enumeration.EventState;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final LocationMapper locationMapper;
    private final UserClient userClient;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(ConfigUtil.DATE);

    public EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        UserDto initiator = getUser(event.getInitiatorId());

        return toEventFullDto(event, initiator);
    }

    public EventFullDto toEventFullDto(
            Event event,
            UserDto initiator
    ) {
        if (event == null) {
            return null;
        }

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .participantLimit((long) event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .publishedOn(event.getPublishedOn())
                .createdOn(formatCreatedOn(event))
                .initiator(initiator)
                .category(
                        event.getCategory() == null
                                ? null
                                : categoryMapper.toCategoryDto(event.getCategory())
                )
                .location(
                        event.getLocation() == null
                                ? null
                                : locationMapper.toLocationDto(event.getLocation())
                )
                .views(0L)
                .confirmedRequests(0L)
                .build();
    }

    public Event toEventModel(NewEventDto dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();

        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(dto.getLocation());
        event.setPaid(dto.isPaid());
        event.setParticipantLimit(dto.getParticipantLimit());

        event.setRequestModeration(
                dto.getRequestModeration() != null
                        ? dto.getRequestModeration()
                        : true
        );

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        if (dto.getCategory() != null) {
            Category category = new Category();
            category.setId(dto.getCategory());
            event.setCategory(category);
        }

        return event;
    }

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        UserDto initiator = getUser(event.getInitiatorId());

        return toEventShortDto(event, initiator);
    }

    private EventShortDto toEventShortDto(
            Event event,
            UserDto initiator
    ) {
        if (event == null) {
            return null;
        }

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate())
                .paid(event.getPaid())
                .initiator(initiator)
                .category(
                        event.getCategory() == null
                                ? null
                                : categoryMapper.toCategoryDto(event.getCategory())
                )
                .views(0L)
                .confirmedRequests(0L)
                .build();
    }

    public List<EventShortDto> toEventShortDtoList(
            List<Event> events
    ) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, UserDto> users = getUsers(events);

        return events.stream()
                .map(event -> toEventShortDto(
                        event,
                        users.get(event.getInitiatorId())
                ))
                .collect(Collectors.toList());
    }

    public List<EventFullDto> toEventFullDtoList(
            List<Event> events
    ) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, UserDto> users = getUsers(events);

        return events.stream()
                .map(event -> toEventFullDto(
                        event,
                        users.get(event.getInitiatorId())
                ))
                .collect(Collectors.toList());
    }

    private String formatCreatedOn(Event event) {
        if (event.getCreatedOn() == null) {
            return null;
        }

        return event.getCreatedOn().format(dateFormatter);
    }

    private UserDto getUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return userClient.getUserById(userId);
    }

    private Map<Long, UserDto> getUsers(List<Event> events) {
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userClient.getUsersByIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserDto::id,
                        Function.identity(),
                        (first, second) -> first
                ));
    }
}