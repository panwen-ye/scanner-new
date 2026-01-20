package com.example.scanner.server.request;


public class CompleteTaskRequest {
    private long taskId;

    public CompleteTaskRequest(long taskId) {
        this.taskId = taskId;
    }

    public CompleteTaskRequest() {
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
