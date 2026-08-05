package ru.practicum.main.controller.privateAccess;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.RequestDto;
import ru.practicum.main.dto.request.RequestStatusUpdateDto;
import ru.practicum.main.dto.request.RequestStatusUpdateResult;
import ru.practicum.main.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventRequestController {

    private final RequestService requestService;

    @GetMapping("/{eventId}/requests")
    public List<RequestDto> getRequestsByOwnerOfEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId
    ) {
        return requestService.getRequestsByOwnerOfEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public RequestStatusUpdateResult updateRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody RequestStatusUpdateDto requestStatusUpdateDto
    ) {
        return requestService.updateRequests(
                userId,
                eventId,
                requestStatusUpdateDto
        );
    }
}