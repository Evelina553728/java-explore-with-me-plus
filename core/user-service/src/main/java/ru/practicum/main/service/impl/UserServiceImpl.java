package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.exception.NameAlreadyExistException;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.exception.UserNotExistException;
import ru.practicum.main.mapper.UserMapper;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.UserService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            log.warn(
                    "Can't create user with email: {}, the email was used by another user",
                    userDto.getEmail()
            );

            throw new NameAlreadyExistException(
                    String.format(
                            "Can't create user with email: %s, the email was used by another user",
                            userDto.getEmail()
                    )
            );
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());

        User savedUser = userRepository.save(user);

        log.debug(
                "User was created with id: {}, name: {}, email: {}",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );

        return userMapper.toUserDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        log.debug("Received users");

        Pageable page = PageRequest.of(from / size, size);

        if (ids == null || ids.isEmpty()) {
            return userMapper.toUserDtoList(
                    userRepository.findAll(page).toList()
            );
        }

        return userMapper.toUserDtoList(
                userRepository.findAllById(ids)
        );
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotExistException(
                        String.format("User with id=%s was not found", id)
                ));

        userRepository.delete(user);
        log.debug("User with id: {} was deleted", id);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotExistException(
                        String.format("User with id=%s was not found", userId)
                ));
    }

    @Override
    public List<User> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userRepository.findAllById(userIds);
    }
}