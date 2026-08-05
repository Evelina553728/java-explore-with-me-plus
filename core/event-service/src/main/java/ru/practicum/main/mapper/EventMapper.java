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

        EventFullDto dto = new EventFullDto();

        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit((long) event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setInitiator(initiator);

        if (event.getCreatedOn() != null) {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(ConfigUtil.DATE);

            dto.setCreatedOn(event.getCreatedOn().format(formatter));
        }

        if (event.getCategory() != null) {
            dto.setCategory(
                    categoryMapper.toCategoryDto(event.getCategory())
            );
        }

        if (event.getLocation() != null) {
            dto.setLocation(
                    locationMapper.toLocationDto(event.getLocation())
            );
        }

        dto.setViews(0L);
        dto.setConfirmedRequests(0L);

        return dto;
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
        EventShortDto dto = new EventShortDto();

        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid());
        dto.setInitiator(initiator);

        if (event.getCategory() != null) {
            dto.setCategory(
                    categoryMapper.toCategoryDto(event.getCategory())
            );
        }

        dto.setViews(0L);
        dto.setConfirmedRequests(0L);

        return dto;
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