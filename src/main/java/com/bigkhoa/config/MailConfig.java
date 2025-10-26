package com.bigkhoa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(Environment env) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(env.getProperty("spring.mail.host", "smtp.gmail.com"));
        sender.setPort(Integer.parseInt(env.getProperty("spring.mail.port", "587")));
        sender.setUsername(env.getProperty("spring.mail.username"));
        sender.setPassword(env.getProperty("spring.mail.password"));

        Properties props = sender.getJavaMailProperties();
        // Ensure STARTTLS and auth are explicitly set
        props.put("mail.transport.protocol", env.getProperty("spring.mail.protocol", "smtp"));
        props.put("mail.smtp.auth", env.getProperty("spring.mail.properties.mail.smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable",
                env.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "true"));
        props.put("mail.smtp.starttls.required",
                env.getProperty("spring.mail.properties.mail.smtp.starttls.required", "true"));
        props.put("mail.smtp.ssl.trust", env.getProperty("spring.mail.host", "smtp.gmail.com"));
        props.put("mail.debug", env.getProperty("spring.mail.properties.mail.debug", "true"));

        // Optional timeouts
        props.putIfAbsent("mail.smtp.connectiontimeout",
                env.getProperty("spring.mail.properties.mail.smtp.connectiontimeout", "5000"));
        props.putIfAbsent("mail.smtp.timeout", env.getProperty("spring.mail.properties.mail.smtp.timeout", "5000"));
        props.putIfAbsent("mail.smtp.writetimeout",
                env.getProperty("spring.mail.properties.mail.smtp.writetimeout", "5000"));

        return sender;
    }
}
