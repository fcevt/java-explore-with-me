package ru.practicum.comment.service;

import ru.practicum.comment.dto.CommentDto;

public interface CommentAdminService {

    void deleteComment(Long commentId);

    CommentDto getCommentById(Long commentId);
}
