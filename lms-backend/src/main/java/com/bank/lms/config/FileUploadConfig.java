package com.bank.lms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lms.file")
public class FileUploadConfig {

    /**
     * 文件上传路径
     */
    private String uploadPath = "./upload";

    /**
     * 最大文件大小（默认10MB）
     */
    private long maxSize = 10 * 1024 * 1024;

    /**
     * 文件类型映射
     */
    private Map<String, List<String>> allowedTypes = new HashMap<>();

    /**
     * 检查文件大小是否合法
     */
    public boolean checkFileSize(MultipartFile file) {
        return file != null && file.getSize() > 0 && file.getSize() <= maxSize;
    }

    /**
     * 检查文件类型是否合法
     */
    public boolean checkFileType(String fileName, String materialType) {
        if (fileName == null || materialType == null) {
            return false;
        }

        List<String> allowedExtensions = allowedTypes.get(materialType);
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(fileName).toLowerCase();
        return allowedExtensions.contains(extension);
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
}
