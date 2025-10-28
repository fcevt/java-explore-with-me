package ru.practicum.comment.service;

import ru.practicum.comment.dto.CommentCreateDto;
import ru.practicum.comment.dto.CommentDto;


public interface CommentPrivateService {

    CommentDto addComment(CommentCreateDto commentCreateOrUpdateDto, Long userId, Long eventId);

    CommentDto updateComment(CommentCreateDto commentCreateDto,
                             Long userId, Long commentId);

    void deleteComment(Long userId, Long commentId);
}
