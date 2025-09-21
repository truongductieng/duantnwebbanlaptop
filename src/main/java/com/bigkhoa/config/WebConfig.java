package com.bigkhoa.config;

import com.bigkhoa.web.AnnouncementOncePerLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private AnnouncementOncePerLoginInterceptor annInterceptor;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Map URL /uploads/** -> thư mục "uploads" ở project root (ngoài classpath)
    Path uploadDir = Paths.get("uploads");
    String uploadPath = uploadDir.toFile().getAbsolutePath();
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadPath + "/");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Interceptor để hiển thị thông báo mã giảm giá 1 lần sau khi đăng nhập
    registry.addInterceptor(annInterceptor).addPathPatterns("/**");
  }
}
