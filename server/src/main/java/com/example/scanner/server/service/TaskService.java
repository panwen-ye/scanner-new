package com.example.scanner.server.service;


import com.example.scanner.server.dao.TaskQueueDao;
import com.example.scanner.server.domain.ScanTask;
import com.example.scanner.server.emun.TaskStatus;
import com.example.scanner.server.mappers.ScannerMapper;
import com.example.scanner.server.request.CheckpointRequest;
import com.example.scanner.server.request.PollTaskRequest;
import com.example.scanner.server.request.SubmitSubTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TaskService {

    private final TaskQueueDao dao;

    @Autowired
    private ScannerMapper scanMapper;

    public TaskService(TaskQueueDao dao) {
        this.dao = dao;
    }

    @Transactional
    public ScanTask pollTask(PollTaskRequest request ) {
        Optional<ScanTask> optionalTask = dao.pollTask(request.getAgentId() , request.getLeaseSeconds() , request.getMaxConcurrentTasks());
        if (!optionalTask.isPresent()) {
            return null;
        }
        ScanTask task = optionalTask.get();
        return task;
    }

    public void submitSubTask(SubmitSubTaskRequest req) {
        long taskId= req.getParentTaskId();
        String  path = req.getPath();
        ScanTask parentTask = scanMapper.queryScanTask(taskId);
        ScanTask scanTaskSub = new ScanTask();
        scanTaskSub.setParentTaskId(taskId);
        scanTaskSub.setPath(path);
        scanTaskSub.setStatus("NEW");
        scanTaskSub.setPriority(parentTask.getPriority());
        dao.insertTask(scanTaskSub);
    }

    public void updateCheckpoint(CheckpointRequest req) {
        dao.updateCheckpoint(req.getTaskId(), req.getCheckpointPath());
    }

    public void completeTask(long taskId) {
        dao.markCompleted(taskId);
    }

    public void failTask(long taskId, String reason) {
        dao.markFailed(taskId, TaskStatus.FAILED.name());
    }
}
