package com.ductieng.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {

    private final Path root = Paths.get("uploads");

    public StorageService() throws IOException {
        if (!Files.exists(root)) Files.createDirectories(root);
    }

    /** Lưu file vào /uploads, trả về tên file (để lưu DB) */
    public String save(MultipartFile mf) throws IOException {
        if (mf == null || mf.isEmpty()) return null;
        String ext = "";
        String original = mf.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String cleanExt = StringUtils.cleanPath(ext);
        String name = UUID.randomUUID().toString().replace("-", "") + cleanExt;
        Path target = root.resolve(name);
        Files.copy(mf.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    public void deleteQuietly(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Files.deleteIfExists(root.resolve(filename));
        } catch (Exception ignore) {}
    }

    /** Tải từ /uploads/{filename} */
    public Resource loadAsResource(String filename) {
        if (filename == null || filename.isBlank()) return null;
        Path p = root.resolve(filename);
        return Files.exists(p) ? new FileSystemResource(p) : null;
    }

    /** Tải từ classpath (vd: placeholder) */
    public Resource classpath(String classpathLocation) {
        try {
            ClassPathResource cp = new ClassPathResource(classpathLocation);
            return cp.exists() ? cp : null;
        } catch (Exception e) {
            return null;
        }
    }
}
