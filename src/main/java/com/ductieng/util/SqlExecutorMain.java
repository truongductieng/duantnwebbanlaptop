package com.ductieng.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class SqlExecutorMain {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (InputStream is = SqlExecutorMain.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) p.load(is);
        }

        String url = p.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/laptopdb?useSSL=false&serverTimezone=UTC");
        String user = p.getProperty("spring.datasource.username", "root");
        String pass = p.getProperty("spring.datasource.password", "");

        Path sqlPath = Path.of(System.getProperty("user.dir"), args.length > 0 ? args[0] : "update-tadmin-password.sql");
        if (!Files.exists(sqlPath)) {
            System.err.println("SQL file not found: " + sqlPath);
            System.exit(2);
        }

        String raw = Files.readString(sqlPath);
        StringBuilder cleaned = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(raw))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                cleaned.append(line).append("\n");
            }
        }

        String[] stmts = cleaned.toString().split(";\\s*(?=\\r?\\n|$)");

        System.out.println("Connecting to DB: " + url + " as user=" + user);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String s : stmts) {
                    String t = s.trim();
                    if (t.isEmpty()) continue;
                    System.out.println("Executing: " + (t.length() > 120 ? t.substring(0, 120) + "..." : t));
                    boolean hasResult = st.execute(t);
                    if (hasResult) {
                        try (java.sql.ResultSet rs = st.getResultSet()) {
                            java.sql.ResultSetMetaData md = rs.getMetaData();
                            int cols = md.getColumnCount();
                            StringBuilder header = new StringBuilder();
                            for (int i = 1; i <= cols; i++) {
                                header.append(md.getColumnLabel(i));
                                if (i < cols) header.append(" | ");
                            }
                            System.out.println(header.toString());
                            int rowCount = 0;
                            while (rs.next() && rowCount < 200) {
                                StringBuilder row = new StringBuilder();
                                for (int i = 1; i <= cols; i++) {
                                    Object v = rs.getObject(i);
                                    row.append(v == null ? "NULL" : v.toString());
                                    if (i < cols) row.append(" | ");
                                }
                                System.out.println(row.toString());
                                rowCount++;
                            }
                            if (rowCount == 200) System.out.println("... (truncated)");
                        }
                    }
                }
                conn.commit();
                System.out.println("All statements executed.");
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}
