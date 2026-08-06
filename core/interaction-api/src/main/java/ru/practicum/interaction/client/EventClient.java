package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.dto.event.EventDto;

@FeignClient(
        name = "event-service",
        path = "/internal/events"
)
public interface EventClient {

    @GetMapping("/{eventId}")
    EventDto getEventById(
            @PathVariable("eventId") Long eventId
    );
}