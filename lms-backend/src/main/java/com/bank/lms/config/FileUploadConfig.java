package com.bank.lms.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
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
     * 文件上传路径（使用 file: 协议）
     */
    private String uploadPath = "file:./upload";

    /**
     * 最大文件大小（默认10MB）
     */
    private long maxSize = 10 * 1024 * 1024;

    /**
     * 文件类型映射
     */
    private Map<String, List<String>> allowedTypes = new HashMap<>();

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 解析后的绝对路径
     */
    private String resolvedUploadPath;

    /**
     * 初始化后解析路径
     */
    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource(uploadPath);
            if (resource.exists()) {
                resolvedUploadPath = resource.getFile().getAbsolutePath();
            } else {
                resolvedUploadPath = resource.getFile().getAbsolutePath();
            }

            // 自动创建上传目录
            File uploadDir = new File(resolvedUploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
        } catch (IOException e) {
            resolvedUploadPath = "./upload";
        }
    }

    /**
     * 获取上传路径的绝对路径
     */
    public String getResolvedUploadPath() {
        return resolvedUploadPath;
    }

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
