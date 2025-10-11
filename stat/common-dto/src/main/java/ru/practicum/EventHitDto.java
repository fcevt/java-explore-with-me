package ru.practicum;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventHitDto {

    @NotNull(message = "поле 'app' не может быть null")
    private String app;

    @NotNull(message = " field 'uri' can't be null")
    private String uri;

    @NotNull(message = "field 'ip' can't be null")
    private String ip;

    @NotNull(message = "field 'timestamp' can't be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
