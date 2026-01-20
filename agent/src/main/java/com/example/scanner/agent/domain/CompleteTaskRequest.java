package com.example.scanner.agent.domain;


public class CompleteTaskRequest {
    private long taskId;

    public CompleteTaskRequest(long taskId) {
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
