package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.main.enumeration.RequestStatus;
import ru.practicum.main.model.Request;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository
        extends JpaRepository<Request, Long> {

    Boolean existsByRequesterIdAndEventId(
            Long requesterId,
            Long eventId
    );

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    Optional<Request> findByRequesterIdAndId(
            Long requesterId,
            Long requestId
    );

    Integer countByEventIdAndStatus(
            Long eventId,
            RequestStatus status
    );

    List<Request> findAllByEventIdInAndStatus(
            List<Long> eventIds,
            RequestStatus status
    );
}