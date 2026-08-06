package ru.practicum.main.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.main.dto.request.RequestDto;
import ru.practicum.main.model.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RequestMapper {

    public RequestDto toRequestDto(Request request) {
        if (request == null) {
            return null;
        }

        RequestDto dto = new RequestDto();

        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setEvent(request.getEventId());
        dto.setRequester(request.getRequesterId());
        dto.setStatus(String.valueOf(request.getStatus()));

        return dto;
    }

    public List<RequestDto> toRequestDtoList(
            List<Request> requests
    ) {
        if (requests == null) {
            return new ArrayList<>();
        }

        return requests.stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }
}