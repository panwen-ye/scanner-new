package com.example.scanner.server.request;


public class SubmitSubTaskRequest {
    private long parentTaskId;
    private String path;

    public long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SubmitSubTaskRequest(long parentTaskId, String path) {
        this.parentTaskId = parentTaskId;
        this.path = path;
    }
}
