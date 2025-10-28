package ru.practicum.comment.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentAdminService;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class CommentAdminController {
    CommentAdminService commentAdminService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteComment(@PathVariable Long commentId) {
        log.info("Admin Deleting comment with commentId {}", commentId);
        commentAdminService.deleteComment(commentId);
    }

    @GetMapping("/{commentId}")
    CommentDto getComment(@PathVariable Long commentId) {
        log.info("getComment {}", commentId);
        return commentAdminService.getCommentById(commentId);
    }
}
