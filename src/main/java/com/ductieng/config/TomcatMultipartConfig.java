package com.ductieng.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tăng giới hạn số file upload trong 1 request cho Tomcat (fileCountMax).
 * Lý do: form admin cho upload nhiều ảnh (5), Tomcat mặc định có thể đang để 1 → văng FileCountLimitExceededException.
 */
@Configuration
public class TomcatMultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMaxFileCountCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            // Cho phép tối đa 10 file/1 request (dư cho 5 ảnh của mình)
            context.getServletContext().setAttribute(
                "org.apache.tomcat.util.http.fileupload.fileCountMax",
                Long.valueOf(10)
            );
        });
    }
}
