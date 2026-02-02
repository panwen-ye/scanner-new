# 分布式文件扫描系统设计说明书（补充版）

## 一、整体架构图（Mermaid）

```sequenceDiagram
    autonumber
    participant Agent
    participant Server
    participant DB as Task Queue(DB)

    loop pullSubTask
        Agent->>Server: pullSubTask(taskId, agentId)
        Server->>DB: select subTask where status='new'
        
        alt subTask != null
            Server->>DB: update subtask_queue set status='running', pcIp='ip' where id=subTaskId
            DB-->>Server: subTask / null
            Server-->>Agent: subTask / null
            Agent->>Agent: ScannerFileSearchService.startFileScanning(subTaskPath)
            Agent->>Agent: threadPool.execute(fileReadTask)

            loop 文件读取（多线程）
                Agent->>Agent: readFile(file)
                alt 命中规则
                    Agent->>Server: reportHit(taskId, subTaskId, result)
                    Server->>DB: insert scan_result
                end
            end

            Agent->>Server: reportSubTaskCompleted(taskId, subTaskId)
            Server->>DB: update subtask_queue set status='completed'

        else subTask == null
            Agent->>Server: queryTaskStatus(taskId)
            Server->>DB: select status from task where id=taskId
            DB-->>Server: taskStatus
            Server-->>Agent: taskStatus

            alt taskStatus == Closed
                Agent->>Agent: break pullSubTask loop
            else taskStatus != Closed
                Agent->>Agent: sleep(60s)
            end
        end
    end

    Agent->>Server: notifyTaskClosed(taskId, agentId)
    Server->>DB: update task set status='closed_confirmed'
    Agent->>Agent: shutdown threadPool






```

---

## 二、深度递归 + 阈值拆分算法（核心设计）

### 2.1 设计目标

- 避免一次性遍历海量文件（亿级）
- 控制任务粒度，保证 Agent 并行度
- 保证 DB 队列消息数量可控
- 支持断点续扫

### 2.2 核心思想

> **只要目录下文件总数未超过阈值，就继续向下递归；一旦超过阈值，立即停止递归，并将当前目录的一级子目录作为子任务入队**

### 2.3 关键阈值

| 参数 | 说明 |
|----|----|
| `CHUNK_SIZE` | 单任务最大文件数（如 5000） |
| `MAX_DEPTH` | 最大递归深度保护 |
| `MAX_TASK_SPLIT` | 单次最多拆分子任务数 |

### 2.4 算法伪代码

```java
scanDir(dir):
    totalFiles = countFilesRecursively(dir, CHUNK_SIZE)

    if totalFiles <= CHUNK_SIZE:
        submitScanTask(dir)
        return

    for each subDir in listSubDirs(dir):
        enqueueTask(subDir)
```

### 2.5 递归计数优化策略

- 使用 **提前终止计数**
- 一旦达到阈值立即返回
- 避免 Files.walk 全量遍历

```java
long countFiles(Path dir, long threshold) {
    AtomicLong counter = new AtomicLong(0);

    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (counter.incrementAndGet() >= threshold) {
                return FileVisitResult.TERMINATE;
            }
            return CONTINUE;
        }
    });
    return counter.get();
}
```

### 2.6 性能收益分析

| 对比项 | 传统方案 | 本方案 |
|-----|-----|-----|
| 单点压力 | 高 | 低 |
| 内存占用 | 不可控 | 稳定 |
| 并行能力 | 差 | 极强 |
| 失败恢复 | 困难 | 天然支持 |
| 扩展性 | 差 | 水平扩展 |

---

## 三、适用场景总结

- 深层目录 + 局部超大目录
- 文件总量亿级
- 多 Agent 分布式扫描
- 强需求：断点续扫 / 优先级 / 可控资源

---

**本文件可直接用于方案评审 / 技术交付 / 架构评估**
