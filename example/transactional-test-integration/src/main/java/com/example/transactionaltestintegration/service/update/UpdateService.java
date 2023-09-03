package com.example.transactionaltestintegration.service.update;

import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateService {
    private final UpdateInnerService updateInnerService;
    private final CommentRepository commentRepository;

    public void updateByEntityAndUpdateOutside(long id) {
        Comment comment = commentRepository.findById(id).get();
        updateInnerService.updateByEntityWithRequireSNew(comment);
        comment.setContent("[Comment]");
        commentRepository.save(comment);
    }

    @Transactional
    public void updateByIdAndGetEntityAndUpdateOutside(long id) {
        Comment comment = updateInnerService.updateByIdAndGetEntityWithRequiresNew(id);
        comment.setContent("[Comment]");
    }
}
