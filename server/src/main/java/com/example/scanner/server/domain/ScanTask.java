package com.example.scanner.server.domain;

import java.time.LocalDateTime;

public class ScanTask {


    private long taskId;
    private String path;
    private int shardId;

    private long parentTaskId;

    private String status;

    /**
     * 优先级
     *
     */
    private Integer priority;

    private String agentId;

    private String checkpoint;

    private LocalDateTime leaseExpireTime;

    private Integer fileThreshold;

    public Integer getFileThreshold() {
        return fileThreshold;
    }

    public void setFileThreshold(Integer fileThreshold) {
        this.fileThreshold = fileThreshold;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(String checkpoint) {
        this.checkpoint = checkpoint;
    }

    public LocalDateTime getLeaseExpireTime() {
        return leaseExpireTime;
    }

    public void setLeaseExpireTime(LocalDateTime leaseExpireTime) {
        this.leaseExpireTime = leaseExpireTime;
    }

    public long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(long parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }
}
