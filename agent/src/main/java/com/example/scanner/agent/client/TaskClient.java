package com.example.scanner.agent.client;


import com.example.scanner.agent.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * TaskClient
 * <p>
 * Agent 与后台任务服务之间的唯一通信入口
 * - 拉取任务
 * - 提交子任务
 * - 上报 checkpoint
 * - 上报完成 / 失败
 */
@Component
@Slf4j
public class TaskClient {

    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${agent.maxConcurrentTasks:2}")
    private int maxConcurrentTasks;

    @Value("${agent.leaseSeconds:300}")
    private int leaseSeconds;

    @Autowired
    private RestTemplate restTemplate;


    public TaskClient(@Value("${task.server.url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }


    /* ================= 拉取任务 ================= */

    public ScanTask pollTask(String agentId) {
        PollTaskRequest request = new PollTaskRequest(agentId, maxConcurrentTasks, leaseSeconds);
        ScanTask response =
                restTemplate.postForObject(
                        baseUrl + "/api/task/poll",
                        request,
                        ScanTask.class
                );


        return response;
    }

    /* ================= 子任务提交 ================= */

    public void submitSubTask(long parentTaskId, String path) {
        SubmitSubTaskRequest req = new SubmitSubTaskRequest(parentTaskId, path);
        post("/api/task/subtask", req, Void.class);
    }

    /* ================= checkpoint ================= */

    public void updateCheckpoint(long taskId, String checkpointPath) {
        CheckpointRequest req = new CheckpointRequest(taskId, checkpointPath);
        post("/api/task/checkpoint", req, Void.class);
    }

    /* ================= 完成 / 失败 ================= */

    public void completeTask(long taskId) {
        CompleteTaskRequest req = new CompleteTaskRequest(taskId);
        post("/api/task/complete", req, Void.class);
    }

    public void failTask(long taskId, String reason) {
        FailTaskRequest req = new FailTaskRequest(taskId, reason);
        post("/api/task/fail", req, Void.class);
    }

    /* ================= 通用 POST ================= */

    private <T> T post(String path, Object body, Class<T> respType) {
        log.info("url : " + baseUrl + path);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // 写请求体
            try (OutputStream os = conn.getOutputStream()) {
                mapper.writeValue(os, body);
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP error: " + code);
            }

            if (respType == Void.class) {
                return null;
            }

            try (InputStream is = conn.getInputStream()) {
                return mapper.readValue(is, respType);
            }

        } catch (Exception e) {
            throw new RuntimeException("Request failed: " + path, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


}

