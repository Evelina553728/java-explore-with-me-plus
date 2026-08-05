package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.client.RequestClient;
import ru.practicum.interaction.dto.request.ConfirmedRequestsDto;
import ru.practicum.main.service.EventParticipationService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteEventParticipationService
        implements EventParticipationService {

    private final RequestClient requestClient;

    @Override
    public Long getConfirmedRequestsCountForEvent(Long eventId) {
        if (eventId == null) {
            return 0L;
        }

        try {
            ConfirmedRequestsDto response =
                    requestClient.getConfirmedRequestsCount(eventId);

            if (response == null
                    || response.confirmedRequests() == null) {
                return 0L;
            }

            return response.confirmedRequests();
        } catch (RuntimeException exception) {
            log.warn(
                    "Request-service недоступен. "
                            + "Для события {} возвращено "
                            + "confirmedRequests=0: {}",
                    eventId,
                    exception.getMessage()
            );

            return 0L;
        }
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCountForEvents(
            List<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> uniqueEventIds = eventIds.stream()
                .filter(eventId -> eventId != null)
                .distinct()
                .toList();

        if (uniqueEventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<ConfirmedRequestsDto> responses =
                    requestClient.getConfirmedRequestsCounts(
                            uniqueEventIds
                    );

            if (responses == null) {
                return createZeroCounts(uniqueEventIds);
            }

            Map<Long, Long> receivedCounts = responses.stream()
                    .filter(response ->
                            response != null
                                    && response.eventId() != null
                    )
                    .collect(Collectors.toMap(
                            ConfirmedRequestsDto::eventId,
                            response ->
                                    response.confirmedRequests() != null
                                            ? response.confirmedRequests()
                                            : 0L,
                            (first, second) -> first
                    ));

            Map<Long, Long> result = new LinkedHashMap<>();

            uniqueEventIds.forEach(eventId ->
                    result.put(
                            eventId,
                            receivedCounts.getOrDefault(eventId, 0L)
                    )
            );

            return result;
        } catch (RuntimeException exception) {
            log.warn(
                    "Request-service недоступен. "
                            + "Для списка событий возвращены "
                            + "нулевые значения: {}",
                    exception.getMessage()
            );

            return createZeroCounts(uniqueEventIds);
        }
    }

    private Map<Long, Long> createZeroCounts(
            List<Long> eventIds
    ) {
        return eventIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        eventId -> 0L,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }
}