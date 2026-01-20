package com.example.scanner.agent.service;


import com.example.scanner.agent.domain.ScanTask;
import com.example.scanner.agent.client.TaskClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ScanAgentConsumer {

    private final String agentId;
    @Autowired
    private final TaskClient taskClient;
    private final ExecutorService scanExecutor;

    @Value("${agent.maxConcurrentTasks:2}")
    private int maxConcurrentTasks;

    @Value("${agent.leaseSeconds:300}")
    private int leaseSeconds;

    @Value("${task.poolSize:4}")
    private int poolSize;

    @Value("${task.fileThreshold:200}")
    private long fileThreshold;

    private volatile boolean running = false;

    public ScanAgentConsumer(TaskClient taskClient) {
        this.agentId = "agent001";
        this.taskClient = taskClient;

        // 线程池大小可通过配置修改
        int poolSize =  4 ;
        this.scanExecutor = Executors.newFixedThreadPool(poolSize);
        this.fileThreshold = 200 ;
    }

    /**
     * Spring Boot 启动后自动启动消费
     */
    @PostConstruct
    public void start() {
        running = true;
        Thread loopThread = new Thread(this::pollLoop, "scan-agent-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    /**
     * 主轮询循环
     */
    private void pollLoop() {
        while (running) {
            try {
                ScanTask task = taskClient.pollTask(agentId);
                if (task == null) {
                    sleepQuietly(1000*10);
                    continue;
                }
                log.info("get Task  , ");

                scanExecutor.submit(() -> processTask(task));

            } catch (Exception e) {
                e.printStackTrace();
                sleepQuietly(2000);
            }
        }
    }

    AtomicLong all = new AtomicLong(0);

    /**
     * 处理单个任务
     */
    private void processTask(ScanTask task) {
        long taskId = task.getTaskId();
        String rootPath = task.getPath();

        AtomicLong counter = new AtomicLong(0);


        try {

            DirectoryScanner.scanWithRecursiveSplit(
                    rootPath,
                    fileThreshold,
                    counter ,
                    subDir -> taskClient.submitSubTask(taskId, subDir),
                    checkpoint -> taskClient.updateCheckpoint(taskId, checkpoint)
            );
            log.info("current task , count : " + counter.get());
            all.addAndGet(counter.get());
            log.info("all task , count : " + all.get());
            taskClient.completeTask(taskId);

        } catch (Exception e) {
            log.info("scanWithRecursiveSplit" , e);
            taskClient.failTask(taskId, e.getMessage());
        }
    }

    /**
     * Spring Boot 关闭时停止
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        scanExecutor.shutdown();
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}

