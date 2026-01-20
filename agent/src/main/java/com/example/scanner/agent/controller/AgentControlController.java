package com.example.scanner.agent.controller;

import com.example.scanner.agent.service.ScanAgentConsumer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/control")
public class AgentControlController {

    private final ScanAgentConsumer consumer;

    public AgentControlController(ScanAgentConsumer consumer) {
        this.consumer = consumer;
    }

    @PostMapping("/start")
    public String start() {
        consumer.start();
        return "started";
    }

    @PostMapping("/stop")
    public String stop() {
        consumer.shutdown();
        return "stopped";
    }
}
