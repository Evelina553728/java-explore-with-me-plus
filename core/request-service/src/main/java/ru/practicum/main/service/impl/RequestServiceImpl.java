package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.client.EventClient;
import ru.practicum.interaction.client.UserClient;
import ru.practicum.interaction.dto.event.EventDto;
import ru.practicum.main.dto.request.RequestDto;
import ru.practicum.main.dto.request.RequestStatusUpdateDto;
import ru.practicum.main.dto.request.RequestStatusUpdateResult;
import ru.practicum.main.enumeration.RequestStatus;
import ru.practicum.main.enumeration.RequestStatusToUpdate;
import ru.practicum.main.exception.EventIsNotPublishedException;
import ru.practicum.main.exception.ParticipantLimitException;
import ru.practicum.main.exception.RequestAlreadyConfirmedException;
import ru.practicum.main.exception.RequestAlreadyExistException;
import ru.practicum.main.exception.RequestNotExistException;
import ru.practicum.main.exception.WrongDataException;
import ru.practicum.main.exception.WrongUserException;
import ru.practicum.main.mapper.RequestMapper;
import ru.practicum.main.model.Request;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final RequestMapper requestMapper;

    @Override
    public List<RequestDto> getRequestsByOwnerOfEvent(
            Long userId,
            Long eventId
    ) {
        List<Request> requests =
                getParticipationRequests(userId, eventId);

        return requestMapper.toRequestDtoList(requests);
    }

    @Override
    @Transactional
    public RequestDto createRequest(
            Long userId,
            Long eventId
    ) {
        if (requestRepository
                .existsByRequesterIdAndEventId(userId, eventId)) {

            throw new RequestAlreadyExistException(
                    "Request already exists"
            );
        }

        EventDto event = eventClient.getEventById(eventId);

        userClient.getUserById(userId);

        if (event.initiatorId().equals(userId)) {
            throw new WrongUserException(
                    "Can't create request by initiator"
            );
        }

        if (!"PUBLISHED".equals(event.state())) {
            throw new EventIsNotPublishedException(
                    "Event is not published yet"
            );
        }

        int participantLimit =
                event.participantLimit() != null
                        ? event.participantLimit()
                        : 0;

        int confirmedRequests =
                requestRepository.countByEventIdAndStatus(
                        event.id(),
                        RequestStatus.CONFIRMED
                );

        if (participantLimit > 0
                && confirmedRequests >= participantLimit) {

            throw new ParticipantLimitException(
                    "Member limit exceeded"
            );
        }

        Request request = new Request();

        request.setCreated(LocalDateTime.now());
        request.setEventId(event.id());
        request.setRequesterId(userId);

        if (!Boolean.TRUE.equals(event.requestModeration())
                || participantLimit == 0) {

            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        Request savedRequest =
                requestRepository.save(request);

        return requestMapper.toRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public RequestStatusUpdateResult updateRequests(
            Long userId,
            Long eventId,
            RequestStatusUpdateDto requestStatusUpdateDto
    ) {
        EventDto event = eventClient.getEventById(eventId);

        int participantLimit =
                event.participantLimit() != null
                        ? event.participantLimit()
                        : 0;

        if (!Boolean.TRUE.equals(event.requestModeration())
                || participantLimit == 0) {

            throw new WrongDataException(
                    "Нет доступа или количество заявок равно 0"
            );
        }

        List<Request> requests =
                getParticipationRequests(userId, eventId);

        List<Request> requestsToUpdate = requests.stream()
                .filter(request ->
                        requestStatusUpdateDto
                                .getRequestIds()
                                .contains(request.getId())
                )
                .collect(Collectors.toList());

        boolean rejectingConfirmedRequest =
                requestsToUpdate.stream()
                        .anyMatch(request ->
                                request.getStatus()
                                        == RequestStatus.CONFIRMED
                                        && requestStatusUpdateDto
                                        .getStatus()
                                        == RequestStatusToUpdate.REJECTED
                        );

        if (rejectingConfirmedRequest) {
            throw new RequestAlreadyConfirmedException(
                    "Request already confirmed"
            );
        }

        int confirmedRequests =
                requestRepository.countByEventIdAndStatus(
                        event.id(),
                        RequestStatus.CONFIRMED
                );

        if (requestStatusUpdateDto.getStatus()
                == RequestStatusToUpdate.CONFIRMED
                && confirmedRequests + requestsToUpdate.size()
                > participantLimit) {

            throw new ParticipantLimitException(
                    "Exceeding the limit of participants"
            );
        }

        RequestStatus newStatus =
                RequestStatus.valueOf(
                        requestStatusUpdateDto
                                .getStatus()
                                .name()
                );

        requestsToUpdate.forEach(
                request -> request.setStatus(newStatus)
        );

        List<Request> savedRequests =
                requestRepository.saveAll(requestsToUpdate);

        RequestStatusUpdateResult result =
                new RequestStatusUpdateResult();

        if (newStatus == RequestStatus.CONFIRMED) {
            result.setConfirmedRequests(
                    requestMapper.toRequestDtoList(
                            savedRequests
                    )
            );
        }

        if (newStatus == RequestStatus.REJECTED) {
            result.setRejectedRequests(
                    requestMapper.toRequestDtoList(
                            savedRequests
                    )
            );
        }

        return result;
    }

    @Override
    public List<RequestDto> getCurrentUserRequests(
            Long userId
    ) {
        userClient.getUserById(userId);

        List<Request> requests =
                requestRepository.findAllByRequesterId(userId);

        return requestMapper.toRequestDtoList(requests);
    }

    @Override
    @Transactional
    public RequestDto cancelRequests(
            Long userId,
            Long requestId
    ) {
        Request request = requestRepository
                .findByRequesterIdAndId(userId, requestId)
                .orElseThrow(
                        () -> new RequestNotExistException(
                                String.format(
                                        "Request with id=%s was not found",
                                        requestId
                                )
                        )
                );

        request.setStatus(RequestStatus.CANCELED);

        return requestMapper.toRequestDto(
                requestRepository.save(request)
        );
    }

    private List<Request> getParticipationRequests(
            Long userId,
            Long eventId
    ) {
        EventDto event = eventClient.getEventById(eventId);

        userClient.getUserById(userId);

        if (!userId.equals(event.initiatorId())) {
            throw new WrongDataException(
                    "Пользователь "
                            + userId
                            + " не инициатор события "
                            + eventId
            );
        }

        return requestRepository.findAllByEventId(eventId);
    }
}