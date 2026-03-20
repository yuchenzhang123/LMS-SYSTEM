package com.bank.lms.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 日志归档定时任务
 * 每月1号凌晨2点执行，将上个月的日志文件压缩成 ZIP 包
 */
@Slf4j
@Component
public class LogArchiveScheduler {

    private static final String LOG_BASE_PATH = "logs";
    private static final String CURRENT_LOG_PATH = LOG_BASE_PATH + "/current";
    private static final String ERROR_LOG_PATH = LOG_BASE_PATH + "/error";
    private static final String ARCHIVE_PATH = LOG_BASE_PATH + "/archives";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Value("${lms.log.archive.retention-years:3}")
    private int archiveRetentionYears;

    /**
     * 每月1号凌晨2点执行日志归档
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveLogs() {
        log.info("开始执行日志归档任务");

        try {
            // 确保归档目录存在
            Files.createDirectories(Paths.get(ARCHIVE_PATH));

            // 获取上个月
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            String monthStr = lastMonth.format(MONTH_FORMATTER);

            // 归档 INFO 日志
            archiveMonthlyLogs(CURRENT_LOG_PATH, "lms-backend-info", monthStr);

            // 归档 ERROR 日志
            archiveMonthlyLogs(ERROR_LOG_PATH, "lms-backend-error", monthStr);

            // 清理超过保留期限的归档文件
            cleanOldArchives();

            log.info("日志归档任务执行完成");

        } catch (Exception e) {
            log.error("日志归档任务执行失败", e);
        }
    }

    /**
     * 归档指定目录的月度日志
     */
    private void archiveMonthlyLogs(String logDir, String logPrefix, String monthStr) throws IOException {
        File dir = new File(logDir);
        if (!dir.exists()) {
            log.info("日志目录不存在: {}", logDir);
            return;
        }

        // 查找上个月的所有日志文件
        File[] logFiles = dir.listFiles((d, name) ->
            name.startsWith(logPrefix) &&
            name.endsWith(".log") &&
            name.contains(monthStr)
        );

        if (logFiles == null || logFiles.length == 0) {
            log.info("未找到需要归档的日志文件: {} (月份: {})", logPrefix, monthStr);
            return;
        }

        // 创建月度压缩包
        String zipFileName = ARCHIVE_PATH + "/" + logPrefix + "-" + monthStr + ".log.zip";
        File zipFile = new File(zipFileName);

        // 如果压缩包已存在，追加文件
        if (zipFile.exists()) {
            appendToExistingZip(logFiles, zipFile);
        } else {
            createNewZip(logFiles, zipFile);
        }

        // 删除已归档的日志文件
        for (File logFile : logFiles) {
            if (logFile.delete()) {
                log.info("已删除归档后的日志文件: {}", logFile.getName());
            } else {
                log.warn("无法删除日志文件: {}", logFile.getName());
            }
        }

        log.info("归档完成: {} 个文件 -> {}", logFiles.length, zipFileName);
    }

    /**
     * 创建新的压缩包
     */
    private void createNewZip(File[] files, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : files) {
                addFileToZip(file, zos);
            }
        }
    }

    /**
     * 追加到现有压缩包
     */
    private void appendToExistingZip(File[] files, File existingZip) throws IOException {
        File tempZip = new File(existingZip.getAbsolutePath() + ".tmp");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            // 复制现有条目
            copyExistingEntries(existingZip, zos);
            // 添加新文件
            for (File file : files) {
                addFileToZip(file, zos);
            }
        }

        // 替换原文件
        if (existingZip.delete()) {
            if (!tempZip.renameTo(existingZip)) {
                throw new IOException("无法重命名临时文件");
            }
        } else {
            throw new IOException("无法删除原压缩包");
        }
    }

    /**
     * 复制压缩包中的现有条目
     */
    private void copyExistingEntries(File existingZip, ZipOutputStream zos) throws IOException {
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(existingZip)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                zos.putNextEntry(new ZipEntry(entry.getName()));
                try (InputStream is = zipFile.getInputStream(entry)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 添加文件到压缩包
     */
    private void addFileToZip(File file, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        zos.closeEntry();
    }

    /**
     * 清理超过保留期限的归档文件
     */
    private void cleanOldArchives() {
        File archiveDir = new File(ARCHIVE_PATH);
        File[] zipFiles = archiveDir.listFiles((dir, name) ->
            name.startsWith("lms-backend-") && name.endsWith(".log.zip")
        );

        if (zipFiles == null || zipFiles.length == 0) {
            return;
        }

        LocalDate cutoffDate = LocalDate.now().minusYears(archiveRetentionYears);
        String cutoffMonth = cutoffDate.format(MONTH_FORMATTER);

        int deletedCount = 0;
        for (File zipFile : zipFiles) {
            try {
                String fileMonth = extractMonthFromArchiveName(zipFile.getName());
                if (fileMonth != null && fileMonth.compareTo(cutoffMonth) < 0) {
                    if (zipFile.delete()) {
                        log.info("已删除超过{}年的归档文件: {}", archiveRetentionYears, zipFile.getName());
                        deletedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("清理归档文件失败: {}", zipFile.getName(), e);
            }
        }

        if (deletedCount > 0) {
            log.info("共删除 {} 个超期归档文件", deletedCount);
        }
    }

    /**
     * 从归档文件名提取月份
     */
    private String extractMonthFromArchiveName(String fileName) {
        try {
            // 格式: lms-backend-info-2023-01.log.zip 或 lms-backend-error-2023-01.log.zip
            String[] parts = fileName.split("-");
            if (parts.length >= 4) {
                return parts[2] + "-" + parts[3].substring(0, 2);
            }
        } catch (Exception e) {
            log.warn("无法从归档名提取月份: {}", fileName);
        }
        return null;
    }
}
