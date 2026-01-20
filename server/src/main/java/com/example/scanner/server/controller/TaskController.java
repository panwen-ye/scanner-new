package com.example.scanner.server.controller;


import com.example.scanner.server.domain.ScanTask;
import com.example.scanner.server.request.*;
import com.example.scanner.server.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }


    @PostMapping("/poll")
    public ScanTask poll(@RequestBody PollTaskRequest request ) {
        return taskService.pollTask(request);
    }

    @PostMapping("/subtask")
    public void submitSubTask(@RequestBody SubmitSubTaskRequest req) {
        taskService.submitSubTask(req);
    }

    @PostMapping("/checkpoint")
    public void checkpoint(@RequestBody CheckpointRequest req) {
        taskService.updateCheckpoint(req);
    }

    @PostMapping("/complete")
    public void complete(@RequestBody CompleteTaskRequest req) {
        taskService.completeTask(req.getTaskId());
    }

    @PostMapping("/fail")
    public void fail(@RequestBody FailTaskRequest req) {
        taskService.failTask(req.getTaskId(), req.getReason());
    }
}
