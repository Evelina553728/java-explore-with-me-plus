package ru.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.interaction.dto.user.UserDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/internal/users"
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUserById(
            @PathVariable("userId") Long userId
    );

    @GetMapping
    List<UserDto> getUsersByIds(
            @RequestParam("ids") List<Long> userIds
    );
}