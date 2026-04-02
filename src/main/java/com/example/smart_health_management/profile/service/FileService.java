package com.example.smart_health_management.profile.service;

import com.example.smart_health_management.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.access.url}")
    private String accessUrl;

    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException(400, "头像文件大小不能超过5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(400, "仅支持 jpg/jpeg/png/gif/webp 格式的图片");
        }

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadPath, fileName);

        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            log.error("头像文件保存失败: {}", filePath, e);
            throw new BizException(500, "头像上传失败，请稍后重试");
        }

        log.info("头像文件保存成功: {}", filePath);
        return accessUrl + fileName;
    }
}
