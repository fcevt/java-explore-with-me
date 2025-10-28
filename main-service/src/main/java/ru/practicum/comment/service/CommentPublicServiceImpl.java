package ru.practicum.comment.service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CommentPublicServiceImpl implements CommentPublicService {
    CommentRepository commentRepository;

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId) {
        return commentRepository.findAllByEvent_Id(eventId).stream()
                .map(com -> CommentMapper.toCommentDto(com))
                .toList();
    }
}
