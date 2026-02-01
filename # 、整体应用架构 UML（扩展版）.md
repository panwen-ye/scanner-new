# 、整体应用架构 UML（扩展版）

> 覆盖：
>
> - 策略管理
> - 任务管理
> - Agent 扫描
> - 结果上报与报告

```
flowchart TB
    User[用户 / 管理员]

    %% ===== Server =====
    subgraph Server[Server 后台系统]

        %% ===== Strategy Management =====
        subgraph StrategyMgmt[策略管理模块]

            StrategySvc[策略服务]

            subgraph StrategyConfig[策略配置]
                SConfig[- 关键字组<br/> -  扫描类型<br/>- 文件类型<br/>]
               
            end

            subgraph KeywordModule[关键字模块]
               
                  
                KeywordGroup[ <br/>关键字配置 <br/>- 关键字<br/> -  词组id<br/>- 批次名 <br/>]
            end

        end

        %% ===== Task Management =====
        subgraph TaskMgmt[任务管理模块]
            TaskSvc[任务调度服务]
            TaskCfg[任务配置<br/>- 扫描策略<br/>- 扫描路径<br/>- 过滤条件<br/>]
        end

        %% ===== Report =====
        subgraph ReportMgmt[报告与结果模块]
            ReportSvc[扫描结果服务]
        end

        StrategyDB[(策略库)]
        TaskDB[(任务 / 子任务表)]
        ResultDB[(扫描结果库)]
    end

    %% ===== Agent =====
    subgraph AgentCluster[扫描 Agent 集群]
        Agent1[Scan Agent]
        AgentN[Scan Agent]
    end

    %% ===== User Flow =====
    User --> StrategyMgmt
    User --> TaskMgmt
    User --> ReportMgmt

    %% ===== Strategy Relations =====
    StrategySvc --> StrategyConfig
    StrategySvc --> KeywordModule
    StrategySvc --> StrategyDB

    %% ===== Task Relations =====
    TaskMgmt --> StrategySvc
    TaskSvc --> TaskDB

    %% ===== Agent Interaction =====
    TaskSvc --> Agent1
    TaskSvc --> AgentN

    Agent1 --> TaskDB
    AgentN --> TaskDB

    Agent1 --> ResultDB
    AgentN --> ResultDB

    ReportSvc --> ResultDB

```

### 评审说明要点

- **Server 是“策略 & 状态中心”**
- **Agent 是“纯执行节点”**
- 扫描结果、进度、命中数据**全部可追溯**

------

# 二、核心类图 UML（补充策略 & 报告）

> 对应你的：
>
> - 策略
> - 任务
> - 子任务
> - 扫描结果

```
classDiagram

    %% ===== Strategy =====
    class ScanStrategy {
        +strategyId
        +name
        +scanType
        +keywords
        +createTime
    }

    class Keyword {
        +keywordId
        +value
    }

    ScanStrategy "1" o-- "*" Keyword

    %% ===== Task =====
    class ScanTask {
        +taskId
        +strategyId
        +rootPath
        +status
        +totalFileCount
        +scannedFileCount
    }

    class SubTask {
        +subTaskId
        +taskId
        +path
        +status
        +leaseOwner
        +leaseExpireTime
    }

    ScanTask "1" o-- "*" SubTask
    ScanTask "*" --> "1" ScanStrategy

    %% ===== Agent =====
    class ScanAgentConsumer {
        +start()
        +pullSubTask()
        +executeSubTask()
    }

    class DirectoryScanner {
        +preReadCount()
        +splitByDepth()
        +scanFiles()
    }

    ScanAgentConsumer --> DirectoryScanner
    ScanAgentConsumer --> SubTask

    %% ===== Result =====
    class ScanResult {
        +taskId
        +filePath
        +matchedKeyword
        +matchType
    }

    SubTask --> ScanResult

```

------

# 三、任务生命周期状态图（支持中断 / 续扫）

> 主任务

```
stateDiagram-v2
    [*] --> CREATED

    CREATED --> RUNNING: Agent 获取 taskId

    RUNNING --> PAUSED: 用户中断
    PAUSED --> RUNNING: 用户继续

    RUNNING --> FINISHED: 所有 SCAN SubTask 完成

    FINISHED --> [*]

```
> ## SubTask 生命周期（预读 & 扫描统一建模）
>
> ```
> stateDiagram-v2
>     [*] --> CREATED
> 
>     CREATED --> READY
> 
>     READY --> RUNNING_PRE_READ: type = PRE_READ
>     READY --> RUNNING_SCAN: type = SCAN
> 
>     RUNNING_PRE_READ --> FINISHED_PRE_READ
>     RUNNING_SCAN --> FINISHED_SCAN
> 
>     FINISHED_PRE_READ --> [*]
>     FINISHED_SCAN --> [*]
> ```



### 关键评审点

- **PAUSED ≠ 失败**
- PAUSED 状态下：
  - 不再下发新 subTask
  - 已 RUNNING 的 subTask 可自然结束

------

# 四、核心时序图（策略 → 任务 → 扫描 → 上报）

> 这是评审时最有杀伤力的一张图

```
sequenceDiagram
    participant User
    participant Server
    participant Agent
    participant DB

    User->>Server: 创建扫描策略
    Server->>DB: 保存策略

    User->>Server: 创建扫描任务(path + strategy)
    Server->>DB: 创建 ScanTask

    Server->>Agent: 下发 rootPath + taskId
    Agent->>Agent: 预读文件数
    Agent->>Server: 上报 totalFileCount

    loop pullSubTask
        Agent->>Server: pollSubTask(taskId)
        Server->>DB: lock subTask
        Server-->>Agent: subTask
        Agent->>Agent: 执行扫描
        Agent->>Server: 上报扫描结果
    end
```

------

# 五、深度拆分逻辑 UML（你第 7 点的核心）

```
flowchart TD
    Start[收到任务参数<br/>rootPath + depth]

    Init[初始化递归<br/>currentDepth = 0]

    Recur[递归遍历目录]

    CheckDepth{currentDepth < depth ?}

    Split[向下递归<br/>currentDepth + 1]

    StopSplit[停止递归]

    CreateSubTask[生成子目录 SubTask]
    CreateRootTask[生成 root_subTask -<br/> - 覆盖 rootPath]

    Enqueue[所有 SubTask + root_subTask<br/>统一入队]

    End[拆分完成]

    Start --> Init --> Recur --> CheckDepth
    CheckDepth -- 是 --> Split --> Recur
    CheckDepth -- 否 --> StopSplit --> CreateSubTask
    CreateSubTask --> CreateRootTask --> Enqueue --> End

```

### 设计价值

- 避免：
  - 单目录 2 亿文件 OOM
  - 子任务爆炸
- **天然适配多 Agent 并行**

------

# 六、扫描类型扩展 UML（文件名 / 内容 / 属性）

```
classDiagram
    class ScanHandler {
        <<interface>>
        +scan(file)
    }

    class FileNameScanHandler
    class FileContentScanHandler
    class FileAttrScanHandler

    ScanHandler <|.. FileNameScanHandler
    ScanHandler <|.. FileContentScanHandler
    ScanHandler <|.. FileAttrScanHandler
```

### 评审加分点

- 新增扫描类型 = 新实现类
- 不影响 Agent 主流程

------

# 七、评审级总结（可直接念）

> 本系统采用 **“策略驱动 + 任务拆分 + 子任务调度 + Agent 并行扫描”** 架构，
>  通过 **状态机 + lease 机制** 保证任务可中断、可恢复、不丢失，
>  并支持对 **超大规模目录（亿级文件）** 的稳定扫描与结果审计。