package com.example.transactionaltestintegration.service.update;

import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateInnerService {
    private final CommentRepository commentRepository;

    @Transactional
    public void updateByEntityWithRequireSNew(Comment comment) {
        comment.setContent("[New Comment]");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Comment updateByIdAndGetEntityWithRequiresNew(Long id) {
        Comment comment = commentRepository.findById(id).get();
        comment.setContent("[New Comment]");

        return comment;
    }
}
