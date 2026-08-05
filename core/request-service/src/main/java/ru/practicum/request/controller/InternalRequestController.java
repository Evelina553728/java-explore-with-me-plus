package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.dto.request.ConfirmedRequestsDto;
import ru.practicum.main.service.EventParticipationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final EventParticipationService eventParticipationService;

    @GetMapping("/confirmed/{eventId}")
    public ConfirmedRequestsDto getConfirmedRequestsCount(
            @PathVariable("eventId") Long eventId
    ) {
        Long confirmedRequests =
                eventParticipationService
                        .getConfirmedRequestsCountForEvent(eventId);

        return new ConfirmedRequestsDto(
                eventId,
                confirmedRequests
        );
    }

    @GetMapping("/confirmed")
    public List<ConfirmedRequestsDto> getConfirmedRequestsCounts(
            @RequestParam("eventIds") List<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> counts =
                eventParticipationService
                        .getConfirmedRequestsCountForEvents(eventIds);

        return eventIds.stream()
                .distinct()
                .map(eventId -> new ConfirmedRequestsDto(
                        eventId,
                        counts.getOrDefault(eventId, 0L)
                ))
                .toList();
    }
}