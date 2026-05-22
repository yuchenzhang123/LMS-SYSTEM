package com.bank.lms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步导出任务管理
 * 任务状态存内存，Excel 文件存本地 upload/export/ 目录
 */
@Slf4j
@Service
public class ExportTaskService {

    private final AccountExportService accountExportService;
    private final String uploadPath;

    /** taskId -> ExportTask，内存管理，重启后丢失但不影响已有文件 */
    private final Map<String, ExportTask> tasks = new ConcurrentHashMap<>();

    /** 最多保留 50 个历史任务 */
    private static final int MAX_TASKS = 50;

    public ExportTaskService(AccountExportService accountExportService,
                             @Value("${lms.file.upload-path:file:./upload}") String uploadPath) {
        this.accountExportService = accountExportService;
        this.uploadPath = uploadPath.replace("file:", "");
    }

    /** 提交异步导出，立即返回 taskId */
    public ExportTask submit(AccountExportService.ExportFilter filter) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fileName = "催收账户导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        ExportTask task = new ExportTask(taskId, fileName);
        tasks.put(taskId, task);

        // 后台线程执行导出
        Thread t = new Thread(() -> execute(taskId, filter), "export-" + taskId);
        t.setDaemon(true);
        t.start();

        // 清理超量任务，删除最旧的 COMPLETED/FAILED
        evictOldTasks();

        return task;
    }

    private void execute(String taskId, AccountExportService.ExportFilter filter) {
        ExportTask task = tasks.get(taskId);
        if (task == null) return;
        task.setStatus("RUNNING");
        try {
            byte[] data = accountExportService.export(filter);
            Path dir = Paths.get(uploadPath, "export");
            Files.createDirectories(dir);
            Path file = dir.resolve(task.getFileName());
            Files.write(file, data);
            task.setStatus("COMPLETED");
            task.setFileSize(data.length);
            log.info("异步导出完成: {} ({} bytes)", task.getFileName(), data.length);
        } catch (Exception e) {
            log.error("异步导出失败: {}", task.getFileName(), e);
            task.setStatus("FAILED");
            String msg = e.getMessage();
            task.setErrorMessage(msg != null && msg.length() > 200 ? msg.substring(0, 200) : msg);
        }
    }

    public ExportTask get(String taskId) {
        return tasks.get(taskId);
    }

    public List<ExportTask> listAll() {
        List<ExportTask> list = new ArrayList<>(tasks.values());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }

    /** 下载完成文件 */
    public byte[] download(String taskId) {
        ExportTask task = tasks.get(taskId);
        if (task == null || !"COMPLETED".equals(task.getStatus())) return null;
        try {
            return Files.readAllBytes(Paths.get(uploadPath, "export", task.getFileName()));
        } catch (IOException e) {
            log.error("读取导出文件失败: {}", task.getFileName(), e);
            return null;
        }
    }

    public boolean delete(String taskId) {
        ExportTask task = tasks.get(taskId);
        if (task == null) return false;
        try {
            Files.deleteIfExists(Paths.get(uploadPath, "export", task.getFileName()));
        } catch (IOException e) {
            log.warn("删除导出文件失败: {}", task.getFileName());
        }
        tasks.remove(taskId);
        return true;
    }

    private void evictOldTasks() {
        if (tasks.size() <= MAX_TASKS) return;
        tasks.entrySet().stream()
                .filter(e -> "COMPLETED".equals(e.getValue().getStatus())
                        || "FAILED".equals(e.getValue().getStatus()))
                .sorted(Comparator.comparing(e -> e.getValue().getCreatedAt()))
                .limit(tasks.size() - MAX_TASKS)
                .forEach(e -> {
                    try {
                        Files.deleteIfExists(Paths.get(uploadPath, "export", e.getValue().getFileName()));
                    } catch (IOException ignored) {}
                    tasks.remove(e.getKey());
                });
    }

    // -------------------------------------------------------------------------
    // ExportTask DTO（供 Controller 返回 JSON）
    // -------------------------------------------------------------------------

    public static class ExportTask {
        private String taskId;
        private String fileName;
        private String status = "PENDING";  // PENDING / RUNNING / COMPLETED / FAILED
        private long fileSize;
        private String errorMessage;
        private LocalDateTime createdAt = LocalDateTime.now();

        ExportTask() {}
        ExportTask(String taskId, String fileName) {
            this.taskId = taskId;
            this.fileName = fileName;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
