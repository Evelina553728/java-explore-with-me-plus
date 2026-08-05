package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction.dto.request.ConfirmedRequestsDto;

import java.util.List;

@FeignClient(
        name = "request-service",
        path = "/internal/requests"
)
public interface RequestClient {

    @GetMapping("/confirmed/{eventId}")
    ConfirmedRequestsDto getConfirmedRequestsCount(
            @PathVariable("eventId") Long eventId
    );

    @GetMapping("/confirmed")
    List<ConfirmedRequestsDto> getConfirmedRequestsCounts(
            @RequestParam("eventIds") List<Long> eventIds
    );
}