package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.main.model.User;
import ru.practicum.main.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserDto getUserById(
            @PathVariable("userId") Long userId
    ) {
        return toUserDto(userService.getUserById(userId));
    }

    @GetMapping
    public List<UserDto> getUsersByIds(
            @RequestParam("ids") List<Long> userIds
    ) {
        return userService.getUsersByIds(userIds)
                .stream()
                .map(this::toUserDto)
                .toList();
    }

    private UserDto toUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName()
        );
    }
}