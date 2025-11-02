package com.ductieng.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ductieng.service.EmailService;

import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        if (to == null || !to.contains("@")) {
            throw new IllegalArgumentException("Địa chỉ email người nhận không hợp lệ: " + to);
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
