package com.example.scanner.server.dao;

import com.example.scanner.server.domain.ScanTask;

import java.util.Optional;

public interface TaskQueueDao {

    Optional<ScanTask> pollTask(String agentId,
                                int leaseSeconds,
                                int maxConcurrentTasks);

    void markCompleted(long taskId);

    void markFailed(long taskId, String errorMessage);

    void updateCheckpoint(long taskId, String checkpoint);

    /**
     * 回收 lease 超时任务
     */
    int recoverExpiredTasks();

    void insertTask(ScanTask task);

}

