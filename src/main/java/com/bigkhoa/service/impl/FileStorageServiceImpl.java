package com.bigkhoa.service.impl;

import com.bigkhoa.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

  private final Path baseDir;

  public FileStorageServiceImpl(@Value("${app.upload.base-dir:uploads}") String baseDirStr) throws IOException {
    this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
    Files.createDirectories(this.baseDir.resolve("ann"));
  }

  @Override
  public String saveAnnouncementImage(MultipartFile file) throws IOException {
    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
    if (ext == null) ext = "png";
    String name = UUID.randomUUID() + "." + ext.toLowerCase();
    Path target = baseDir.resolve("ann").resolve(name);
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    return "/uploads/ann/" + name;
  }

  @Override
  public void deleteIfExists(String url) {
    try {
      if (url == null || !url.startsWith("/uploads/")) return;
      Path p = baseDir.resolve(url.replace("/uploads/", ""));
      Files.deleteIfExists(p);
    } catch (Exception ignored) {}
  }
}
