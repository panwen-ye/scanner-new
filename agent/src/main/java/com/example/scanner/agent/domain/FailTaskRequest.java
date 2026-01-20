package com.example.scanner.agent.domain;


public class FailTaskRequest {
    private long taskId;
    private String reason;

    public FailTaskRequest(long taskId, String reason) {
        this.taskId = taskId;
        this.reason = reason;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
