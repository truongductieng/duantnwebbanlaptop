package com.bigkhoa.service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
  String saveAnnouncementImage(MultipartFile file) throws IOException; // trả về /uploads/ann/xxx.jpg
  void deleteIfExists(String url);
}
