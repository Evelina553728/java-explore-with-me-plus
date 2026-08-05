package ru.practicum.main.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.interaction.dto.user.UserDto;
import ru.practicum.main.config.ConfigUtil;
import ru.practicum.main.dto.category.CategoryDto;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventShortDto {

    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ConfigUtil.DATE)
    private LocalDateTime eventDate;

    private UserDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}