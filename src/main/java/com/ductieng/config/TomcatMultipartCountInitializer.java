package com.ductieng.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletContext;

@Configuration
public class TomcatMultipartCountInitializer {

    private static final String FILE_COUNT_MAX_ATTR =
            "org.apache.tomcat.util.http.fileupload.fileCountMax";

    @Bean
    public ServletContextInitializer tomcatFileCountMaxInitializer() {
        return (ServletContext servletContext) -> {
            // Cho phép tối đa 50 file trong 1 request (dư sức cho 5 ảnh)
            servletContext.setAttribute(FILE_COUNT_MAX_ATTR, Long.valueOf(50L));
        };
    }
}
