package com.example.scanner.server.dao;

import com.example.scanner.server.domain.ScanTask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskQueueDaoImpl implements TaskQueueDao {

    private final JdbcTemplate jdbcTemplate;

    public TaskQueueDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * MySQL 5.7 任务抢占核心逻辑
     */
    @Override
    @Transactional
    public Optional<ScanTask> pollTask(String agentId,
                                       int leaseSeconds,
                                       int maxConcurrentTasks) {

        // 1️⃣ Agent 并发保护
        Integer runningCount = jdbcTemplate.queryForObject(
                " SELECT COUNT(*) FROM scan_task_queue WHERE " +
                "agent_id = ? AND status = 'RUNNING' AND lease_expire_time > NOW() ", Integer.class, agentId);

        if (runningCount != null && runningCount >= maxConcurrentTasks) {
            return Optional.empty();
        }

        // 2️⃣ 选一个 NEW 任务（不加锁）
        List<Long> ids = jdbcTemplate.query(" SELECT task_id FROM scan_task_queue " +
                "WHERE status = 'NEW' " +
                "ORDER BY priority ASC " +
                "LIMIT 1 ", (rs, i) -> rs.getLong(1));

        if (ids.isEmpty()) {
            return Optional.empty();
        }

        long taskId = ids.get(0);

        LocalDateTime leaseExpireTime =
                LocalDateTime.now().plusSeconds(leaseSeconds);

        // 3️⃣ 原子抢占（关键）
        int updated = jdbcTemplate.update(" UPDATE scan_task_queue SET status = 'RUNNING', " +
                        "agent_id = ?,  lease_expire_time = ?, updated_time = NOW() WHERE task_id = ? AND status = 'NEW' ", agentId,
                Timestamp.valueOf(leaseExpireTime),
                taskId);

        // 抢占失败（被别的 Agent 抢走）
        if (updated == 0) {
            return Optional.empty();
        }

        // 4️⃣ 查询任务详情
        ScanTask task = jdbcTemplate.queryForObject("  SELECT * FROM scan_task_queue WHERE task_id = ? ", scanTaskRowMapper(), taskId);

        return Optional.ofNullable(task);
    }

    @Override
    public void markCompleted(long taskId) {
        jdbcTemplate.update("  UPDATE scan_task_queue SET status = 'COMPLETED', updated_time = NOW()  WHERE task_id = ? ", taskId);
    }

    @Override
    public void markFailed(long taskId, String errorMessage) {
        jdbcTemplate.update("  UPDATE scan_task_queue SET status = 'FAILED', retry_count = retry_count + 1, " +
                "error_message = ?, updated_time = NOW() WHERE task_id = ? ", errorMessage, taskId);
    }

    @Override
    public void updateCheckpoint(long taskId, String checkpoint) {
        jdbcTemplate.update(" UPDATE scan_task_queue SET checkpoint = ?, updated_time = NOW() WHERE task_id = ? ", checkpoint, taskId);
    }

    /**
     * lease 超时回收（Server 定时任务调用）
     */
    @Override
    public int recoverExpiredTasks() {
        return jdbcTemplate.update(" UPDATE scan_task_queue SET status = 'NEW', " +
                        "agent_id = NULL, lease_expire_time = NULL, " +
                        "updated_time = NOW() WHERE status = 'RUNNING' AND lease_expire_time < NOW() ");
    }

    @Override
    public void insertTask(ScanTask task) {
        jdbcTemplate.update("  INSERT INTO scan_task_queue (parent_task_id," +
         "path, shard_id, status, priority, file_threshold, created_time, updated_time" +
        ") VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())"  ,
                task.getParentTaskId(),
                task.getPath(),
                task.getShardId(),
                task.getStatus(),      // 通常为 NEW
                task.getPriority(),
                task.getFileThreshold()
        );
    }

    private RowMapper<ScanTask> scanTaskRowMapper() {
        return (rs, rowNum) -> {
            ScanTask t = new ScanTask();
            t.setTaskId(rs.getLong("task_id"));
            t.setParentTaskId(rs.getLong("parent_task_id"));
            t.setPath(rs.getString("path"));
            t.setShardId(rs.getInt("shard_id"));
            t.setStatus(rs.getString("status"));
            t.setPriority(rs.getInt("priority"));
            t.setAgentId(rs.getString("agent_id"));
            t.setCheckpoint(rs.getString("checkpoint"));
            Timestamp ts = rs.getTimestamp("lease_expire_time");
            if (ts != null) {
                t.setLeaseExpireTime(ts.toLocalDateTime());
            }
            return t;
        };
    }

}

