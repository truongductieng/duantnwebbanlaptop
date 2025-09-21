package com.bigkhoa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  @Value("${app.upload.base-dir:uploads}") private String baseDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadPath = Paths.get(baseDir).toAbsolutePath().normalize();
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath.toUri().toString());
  }
}
