package com.example.scanner.server.request;


public class CheckpointRequest {
    private long taskId;
    private String checkpointPath;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getCheckpointPath() {
        return checkpointPath;
    }

    public void setCheckpointPath(String checkpointPath) {
        this.checkpointPath = checkpointPath;
    }

    public CheckpointRequest(long taskId, String checkpointPath) {
        this.taskId = taskId;
        this.checkpointPath = checkpointPath;
    }
}
