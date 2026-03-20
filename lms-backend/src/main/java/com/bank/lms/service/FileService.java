package com.bank.lms.service;

import com.bank.lms.config.FileUploadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileUploadConfig fileUploadConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 上传文件
     *
     * @param file         文件
     * @param materialType 材料类型
     * @param materialName 材料名称
     * @return 文件访问URL
     */
    public Map<String, String> uploadFile(MultipartFile file, String materialType, String materialName) {
        // 检查文件大小
        if (!fileUploadConfig.checkFileSize(file)) {
            throw new RuntimeException("文件大小超过限制，最大允许" + (fileUploadConfig.getMaxSize() / 1024 / 1024) + "MB");
        }

        // 检查文件类型
        if (!fileUploadConfig.checkFileType(file.getOriginalFilename(), materialType)) {
            throw new RuntimeException("文件类型与材料类型不匹配");
        }

        try {
            // 构建存储路径: uploadPath/materialType/fileName_timestamp.ext
            String dateDir = LocalDate.now().format(DATE_FORMATTER);
            String relativePath = materialType + "/" + dateDir + "/";
            String uploadBasePath = fileUploadConfig.getUploadPath();
            String fullPath = uploadBasePath + "/" + relativePath;

            // 创建目录
            Path path = Paths.get(fullPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // 生成文件名: 原文件名_时间戳.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newFileName = (materialName != null ? materialName : "file") + "_" + timestamp + "." + extension;

            // 保存文件
            String filePath = fullPath + newFileName;
            file.transferTo(new File(filePath));

            // 构建返回URL
            String url = relativePath + newFileName;

            log.info("文件上传成功: type={}, name={}, url={}", materialType, materialName, url);

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("path", filePath);
            return result;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 根据URL获取文件
     *
     * @param url 文件URL
     * @return 文件对象
     */
    public File getFileByUrl(String url) {
        String fullPath = fileUploadConfig.getUploadPath() + "/" + url;
        File file = new File(fullPath);

        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("文件不存在: " + url);
        }

        return file;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    public String getContentType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "aac":
                return "audio/aac";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/vnd.rar";
            case "7z":
                return "application/x-7z-compressed";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";
            default:
                return "application/octet-stream";
        }
    }
}
