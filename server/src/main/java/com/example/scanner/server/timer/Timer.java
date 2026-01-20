package com.example.scanner.server.timer;

import com.example.scanner.server.dao.TaskQueueDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Timer {

    @Autowired
    private TaskQueueDao taskQueueDao;


    @Scheduled(fixedDelay = 30000)
    public void recoverTasks() {
        int count = taskQueueDao.recoverExpiredTasks();
        if (count > 0) {
//            log.warn("Recovered {} expired scan tasks", count);
        }
    }

}
