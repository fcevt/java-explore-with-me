package ru.practicum.comment.service;

import ru.practicum.comment.dto.CommentDto;

import java.util.List;

public interface CommentPublicService {

    List<CommentDto> getCommentsByEventId(Long eventId);
}
