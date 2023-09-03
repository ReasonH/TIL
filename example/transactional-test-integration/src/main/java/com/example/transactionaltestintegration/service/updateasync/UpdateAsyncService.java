package com.example.transactionaltestintegration.service.updateasync;

import com.example.transactionaltestintegration.entity.Comment;
import com.example.transactionaltestintegration.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateAsyncService {

    private final CommentRepository commentRepository;
    private final UpdateAsyncInnerService updateAsyncInnerService;
    private final TaskExecutor taskExecutor;

    @Transactional
    public void updateAsync(long id) {
        Comment comment = commentRepository.findById(id).get();
        taskExecutor.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
            updateAsyncInnerService.updateSync(comment);
        });
    }

    @Transactional
    public void updateAsyncAndWait(long id) {
        Comment comment = commentRepository.findById(id).get();

        taskExecutor.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
            updateAsyncInnerService.updateSync(comment);
        });
        try {
            Thread.sleep(1500);
        } catch (Exception e) {}
    }
}
