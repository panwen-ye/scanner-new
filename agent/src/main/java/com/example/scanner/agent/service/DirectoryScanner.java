package com.example.scanner.agent.service;


import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * DirectoryScanner
 * <p>
 * 核心设计思想：
 * 1. 深度优先递归目录
 * 2. 统计「当前目录树」下文件总数（递归统计）
 * 3. 当文件数 >= threshold：
 * - 停止继续下探
 * - 将当前目录的【一级子目录】作为子任务提交
 * 4. 不使用 Files.walk()，避免一次性遍历海量文件
 * 5. 支持 checkpoint 回调
 */
public class DirectoryScanner {

    private DirectoryScanner() {
    }

    /**
     * 对外唯一入口
     *
     * @param rootPath        扫描起始目录
     * @param threshold       文件阈值（超过即拆分）
     * @param subTaskConsumer 子任务提交回调
     * @param checkpointHook  checkpoint 回调
     */
    public static void scanWithRecursiveSplit(
            String rootPath,
            long threshold,
            AtomicLong counter,
            Consumer<String> subTaskConsumer,
            Consumer<String> checkpointHook
    ) throws IOException {
        Path root = Paths.get(rootPath);
        if (!Files.isDirectory(root)) {
            return  ;
        }

        // 显式栈，避免深递归导致 StackOverflow
        Deque<Path> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Path currentDir = stack.pop();
            // checkpoint：当前处理目录
            checkpointHook.accept(currentDir.toString());
            long fileCount = countFilesRecursively(currentDir, threshold);
            if (fileCount >= threshold) {
                // 达到阈值，拆分当前目录
                splitToSubTasks(currentDir, subTaskConsumer);
                // 2) 处理当前目录下的文件，避免丢失
                List<Path> files = listFiles(currentDir);
                if (!files.isEmpty()) {
                    // 你可以把文件作为一个独立子任务，也可以直接处理
                    counter.addAndGet(files.size());
                }
                continue;
            }

            // 文件数未达到阈值，继续向下递归
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
                for (Path p : stream) {
                    if (Files.isDirectory(p)) {
                        stack.push(p);
                    } else {
                        counter.incrementAndGet();
                    }
                }
            }
        }
     }

    private static List<Path> listFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) {
                    files.add(p);
                }
            }
        }
        return files;
    }


    /**
     * 统计目录树下文件总数
     * <p>
     * ⚠️ 一旦达到 threshold 立即返回（极其关键的优化）
     */
    private static long countFilesRecursively(Path dir, long threshold) throws IOException {
        FileCounter counter = new FileCounter(threshold);

        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                counter.increment();
                if (counter.reachedLimit()) {
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // 权限丢失 / 网络异常，直接抛出
                throw new RuntimeException("Access denied: " + file, exc);
            }
        });

        return counter.getCount();
    }

    /**
     * 将当前目录的一级子目录拆分为子任务
     */
    private static void splitToSubTasks(Path dir, Consumer<String> subTaskConsumer) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    subTaskConsumer.accept(p.toString());
                }
            }
        }
    }

    /**
     * 内部文件计数器（避免 AtomicLong 性能损耗）
     */
    private static class FileCounter {
        private final long limit;
        private long count = 0;

        FileCounter(long limit) {
            this.limit = limit;
        }

        void increment() {
            count++;
        }

        boolean reachedLimit() {
            return count >= limit;
        }

        long getCount() {
            return count;
        }
    }
}
