package ru.practicum.comment.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentCreateDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CommentPrivateServiceImpl implements CommentPrivateService {

    CommentRepository commentRepository;
    EventRepository eventRepository;
    UserRepository userRepository;

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id " + commentId + " not found"));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("удалять комментарии может только автор");
        }
        commentRepository.deleteCommentById(commentId);
    }

    @Override
    public CommentDto addComment(CommentCreateDto commentCreateDto, Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Event with id " + eventId + " not found"));
        User author = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User with id " + userId + " not found"));
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setText(commentCreateDto.getText());
        comment.setCreatedTime(LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto updateComment(CommentCreateDto commentCreateDto, Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id " + commentId + " not found"));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("редактировать комментарии может только автор");
        }
        comment.setText(commentCreateDto.getText());
        comment.setUpdatedTime(LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }
}
