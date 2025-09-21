package com.bigkhoa.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}