package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.enumeration.RequestStatus;
import ru.practicum.main.model.Request;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.EventParticipationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventParticipationServiceImpl implements EventParticipationService {

    private final RequestRepository requestRepository;

    @Override
    public Long getConfirmedRequestsCountForEvent(Long eventId) {
        Integer count = requestRepository.countByEventIdAndStatus(
                eventId,
                RequestStatus.CONFIRMED
        );

        return count != null ? count.longValue() : 0L;
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountForEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Request> confirmedRequests =
                requestRepository.findAllByEventIdInAndStatus(
                        eventIds,
                        RequestStatus.CONFIRMED
                );

        return confirmedRequests.stream()
                .collect(Collectors.groupingBy(
                        Request::getEventId,
                        Collectors.counting()
                ));
    }
}