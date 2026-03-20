package com.bank.lms.common;

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
 * 每月1日凌晨2点执行，将超过30天的日志打包成月份压缩包
 */
@Slf4j
@Component
public class LogArchiveTask {

    private static final String LOG_BASE_PATH = "logs";
    private static final String CURRENT_LOG_PATH = LOG_BASE_PATH + "/current";
    private static final String ARCHIVE_PATH = LOG_BASE_PATH + "/archive";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${lms.log.archive.retention-days:30}")
    private int retentionDays;

    @Value("${lms.log.archive.archive-retention-years:3}")
    private int archiveRetentionYears;

    /**
     * 每月1日凌晨2点执行日志归档
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void archiveLogs() {
        log.info("开始执行日志归档任务...");
        
        try {
            // 确保目录存在
            createDirectories();
            
            // 获取保留期限前的日期
            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
            
            // 归档超过30天的日志文件
            archiveOldLogs(cutoffDate);
            
            // 清理超过3年的归档文件
            cleanOldArchives();
            
            log.info("日志归档任务执行完成");
        } catch (Exception e) {
            log.error("日志归档任务执行失败", e);
        }
    }

    /**
     * 创建必要的目录
     */
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(CURRENT_LOG_PATH));
        Files.createDirectories(Paths.get(ARCHIVE_PATH));
    }

    /**
     * 归档超过指定日期的日志文件
     */
    private void archiveOldLogs(LocalDate cutoffDate) {
        File currentDir = new File(CURRENT_LOG_PATH);
        File[] logFiles = currentDir.listFiles((dir, name) -> 
            name.startsWith("lms-backend.") && name.endsWith(".log")
        );
        
        if (logFiles == null || logFiles.length == 0) {
            log.info("没有需要归档的日志文件");
            return;
        }

        // 按月份分组文件
        for (File logFile : logFiles) {
            try {
                LocalDate fileDate = extractDateFromFileName(logFile.getName());
                if (fileDate != null && fileDate.isBefore(cutoffDate)) {
                    String month = fileDate.format(MONTH_FORMATTER);
                    archiveFileToMonth(logFile, month);
                }
            } catch (Exception e) {
                log.error("归档文件失败: {}", logFile.getName(), e);
            }
        }
    }

    /**
     * 从文件名提取日期
     */
    private LocalDate extractDateFromFileName(String fileName) {
        try {
            // 格式: lms-backend.2024-01-15.0.log
            String[] parts = fileName.split("\\.");
            if (parts.length >= 3) {
                return LocalDate.parse(parts[1], DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("无法从文件名提取日期: {}", fileName);
        }
        return null;
    }

    /**
     * 将文件归档到指定月份的压缩包
     */
    private void archiveFileToMonth(File logFile, String month) throws IOException {
        String zipFileName = ARCHIVE_PATH + "/lms-backend-" + month + ".log.zip";
        File zipFile = new File(zipFileName);

        // 如果压缩包已存在，先解压再重新压缩
        if (zipFile.exists()) {
            appendToExistingZip(logFile, zipFile);
        } else {
            createNewZip(logFile, zipFile);
        }

        // 删除原文件
        if (logFile.delete()) {
            log.info("已归档并删除文件: {}", logFile.getName());
        } else {
            log.warn("无法删除文件: {}", logFile.getName());
        }
    }

    /**
     * 创建新的压缩包
     */
    private void createNewZip(File logFile, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            addFileToZip(logFile, zos);
        }
    }

    /**
     * 追加到现有压缩包
     */
    private void appendToExistingZip(File logFile, File existingZip) throws IOException {
        File tempZip = new File(existingZip.getAbsolutePath() + ".tmp");
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            // 复制现有条目
            copyExistingEntries(existingZip, zos);
            // 添加新文件
            addFileToZip(logFile, zos);
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
     * 清理超过3年的归档文件
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

        for (File zipFile : zipFiles) {
            try {
                String fileMonth = extractMonthFromArchiveName(zipFile.getName());
                if (fileMonth != null && fileMonth.compareTo(cutoffMonth) < 0) {
                    if (zipFile.delete()) {
                        log.info("已删除超过{}年的归档文件: {}", archiveRetentionYears, zipFile.getName());
                    }
                }
            } catch (Exception e) {
                log.error("清理归档文件失败: {}", zipFile.getName(), e);
            }
        }
    }

    /**
     * 从归档文件名提取月份
     */
    private String extractMonthFromArchiveName(String fileName) {
        try {
            // 格式: lms-backend-2024-01.log.zip
            String[] parts = fileName.split("-");
            if (parts.length >= 3) {
                return parts[2].substring(0, 7); // 提取 yyyy-MM
            }
        } catch (Exception e) {
            log.warn("无法从归档名提取月份: {}", fileName);
        }
        return null;
    }
}
