package com.ductieng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebsitebanlaptopApplication {
  public static void main(String[] args) {
    // 👇 Quan trọng: set system property trước khi Tomcat được tạo
    System.setProperty("org.apache.tomcat.util.http.fileupload.fileCountMax", "50");
    SpringApplication.run(WebsitebanlaptopApplication.class, args);
  }
}
