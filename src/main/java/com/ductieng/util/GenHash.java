package com.ductieng.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("123");
        System.out.println(hash);
    }
}
