package ru.practicum.main.service;

import java.util.List;
import java.util.Map;

public interface EventParticipationService {

    Long getConfirmedRequestsCountForEvent(Long eventId);

    Map<Long, Long> getConfirmedRequestsCountForEvents(List<Long> eventIds);
}