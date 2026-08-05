package ru.practicum.interaction.dto.request;

public record ConfirmedRequestsDto(
        Long eventId,
        Long confirmedRequests
) {
}