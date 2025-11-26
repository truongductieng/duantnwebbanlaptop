package com.ductieng.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CheckAdminLogin {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (InputStream is = CheckAdminLogin.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) p.load(is);
        }
        String url = p.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/laptopdb?useSSL=false&serverTimezone=UTC");
        String user = p.getProperty("spring.datasource.username", "root");
        String pass = p.getProperty("spring.datasource.password", "");

        String candidatePassword = args.length > 0 ? args[0] : "123";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String q = "SELECT username, password FROM users WHERE LOWER(username)='tadmin' LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("No tadmin user found");
                        System.exit(2);
                    }
                    String username = rs.getString(1);
                    String hash = rs.getString(2);
                    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    boolean ok = encoder.matches(candidatePassword, hash);
                    System.out.printf("User=%s OK?=%s (hash=%s)\n", username, ok, hash);
                }
            }
        }
    }
}
