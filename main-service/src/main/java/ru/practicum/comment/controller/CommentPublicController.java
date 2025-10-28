package ru.practicum.comment.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentPublicService;

import java.util.List;

@RestController
@RequestMapping("/comments")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class CommentPublicController {
    CommentPublicService commentPublicService;

    @GetMapping("/event/{eventId}")
    List<CommentDto> getCommentsByEventId(@PathVariable Long eventId) {
        log.info("getCommentsByEventId {}", eventId);
        return commentPublicService.getCommentsByEventId(eventId);
    }
}
