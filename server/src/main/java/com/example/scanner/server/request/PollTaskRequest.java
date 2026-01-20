package com.example.scanner.server.request;


public class PollTaskRequest {
    /**
     * Agent 唯一标识
     */
    private String agentId;

    /**
     * Agent 当前允许的最大并发任务数
     */
    private int maxConcurrentTasks;

    /**
     * 单次任务 lease 秒数
     */
    private int leaseSeconds;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

}
