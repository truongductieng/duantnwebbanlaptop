package com.bigkhoa.service.impl;

import com.bigkhoa.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    public EmailServiceImpl(JavaMailSender mailSender) { this.mailSender = mailSender; }

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        if (to == null || !to.contains("@")) {
            throw new IllegalArgumentException("Địa chỉ email người nhận không hợp lệ: " + to);
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        // msg.setFrom("no-reply@your-domain.com"); // nếu cần
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
