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
            log.error("文件大小超限: fileName={}, size={}MB, maxLimit={}MB",
                    file.getOriginalFilename(),
                    file.getSize() / 1024.0 / 1024.0,
                    fileUploadConfig.getMaxSize() / 1024.0 / 1024.0);
            throw new RuntimeException("文件大小超过限制，最大允许" + (fileUploadConfig.getMaxSize() / 1024 / 1024) + "MB");
        }

        // 如果 materialType 为空，默认使用 document
        String actualMaterialType = (materialType != null && !materialType.isEmpty()) ? materialType : "document";

        // 检查文件类型（如果 materialType 不为空）
        if (materialType != null && !materialType.isEmpty() && !fileUploadConfig.checkFileType(file.getOriginalFilename(), actualMaterialType)) {
            log.warn("文件类型不匹配: fileName={}, expectedType={}, actualType={}",
                    file.getOriginalFilename(), actualMaterialType, getFileExtension(file.getOriginalFilename()));
            throw new RuntimeException("文件类型与材料类型不匹配");
        }

        try {
            // 构建存储路径: uploadPath/materialType/fileName_timestamp.ext
            String dateDir = LocalDate.now().format(DATE_FORMATTER);
            String relativePath = actualMaterialType + "/" + dateDir + "/";
            String uploadBasePath = fileUploadConfig.getResolvedUploadPath();
            // 使用 File.separator 处理不同操作系统的路径分隔符
            String fullPath = uploadBasePath + File.separator + relativePath.replace("/", File.separator);

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
            String filePath = fullPath + File.separator + newFileName;
            file.transferTo(new File(filePath));

            // 构建返回URL（使用 / 作为分隔符，方便URL访问）
            String url = relativePath + newFileName;

            log.info("文件上传成功: type={}, fileName={}, size={}KB, url={}",
                    actualMaterialType, materialName, file.getSize() / 1024, url);

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
        // 检查URL是否为空
        if (url == null || url.isEmpty()) {
            log.error("文件URL为空，获取文件失败");
            throw new RuntimeException("文件URL不能为空");
        }

        // 检查是否为旧的 blob URL
        if (url.startsWith("blob:")) {
            log.warn("尝试访问临时blob URL: {}", url);
            throw new RuntimeException("该文件为临时链接，请重新上传材料");
        }

        String uploadBasePath = fileUploadConfig.getResolvedUploadPath();
        // 将 URL 中的 / 替换为 File.separator
        String relativePath = url.replace("/", File.separator);
        String fullPath = uploadBasePath + File.separator + relativePath;

        File file = new File(fullPath);

        if (!file.exists() || !file.isFile()) {
            log.error("文件不存在: url={}, fullPath={}", url, fullPath);
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
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
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
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "mkv":
                return "video/x-matroska";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "csv":
                return "text/csv";
            case "rtf":
                return "application/rtf";
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
