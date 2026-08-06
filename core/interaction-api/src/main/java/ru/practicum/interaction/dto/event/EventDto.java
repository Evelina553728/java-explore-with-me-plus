package ru.practicum.interaction.dto.event;

public record EventDto(
        Long id,
        Long initiatorId,
        String state,
        Integer participantLimit,
        Boolean requestModeration
) {
}