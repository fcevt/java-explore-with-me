package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentCreateDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentPrivateService;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class CommentPrivateController {
    CommentPrivateService commentPrivateService;

    @PostMapping("/event/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @RequestBody @Valid CommentCreateDto commentCreateDto) {
        log.info("Adding comment to user {} for event {} with text {}", userId, eventId, commentCreateDto.getText());
        return commentPrivateService.addComment(commentCreateDto, userId, eventId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, @PathVariable Long userId) {
        log.info("Deleting comment with commentId {} and userId {}", commentId, userId);
        commentPrivateService.deleteComment(userId, commentId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long commentId,
                                    @PathVariable Long userId,
                                    @RequestBody @Valid CommentCreateDto commentCreateDto) {
        log.info("Updating comment with commentId {} and userId {} and new text {}", commentId, userId, commentCreateDto.getText());
        return commentPrivateService.updateComment(commentCreateDto, userId, commentId);
    }
}
