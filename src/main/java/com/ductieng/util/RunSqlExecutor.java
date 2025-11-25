package com.ductieng.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class RunSqlExecutor {
    public static void main(String[] args) throws Exception {
        // Load DB config from application.properties on classpath
        Properties p = new Properties();
        try (InputStream is = RunSqlExecutor.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                p.load(is);
            } else {
                System.err.println("application.properties not found on classpath; falling back to defaults");
            }
        }

        String url = p.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/lapshop?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true");
        String user = p.getProperty("spring.datasource.username", "root");
        String pass = p.getProperty("spring.datasource.password", "");

        // SQL file path (project root)
        String cwd = System.getProperty("user.dir");
        Path sqlPath = Path.of(cwd, "create-admin-tadmin.sql");
        if (args.length > 0) {
            sqlPath = Path.of(args[0]);
        }

        if (!Files.exists(sqlPath)) {
            System.err.println("SQL file not found: " + sqlPath);
            System.exit(2);
        }

        String raw = Files.readString(sqlPath);
        // Remove SQL comment lines that start with --
        StringBuilder cleaned = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(raw))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                cleaned.append(line).append("\n");
            }
        }
        String sql = cleaned.toString();

        // Split statements by semicolon
        String[] stmts = sql.split(";\\s*(?=\\r?\\n|$)");

        System.out.println("Connecting to DB: " + url + " as user=" + user);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                int executed = 0;
                for (String s : stmts) {
                    String t = s.trim();
                    if (t.isEmpty()) continue;
                    // skip comments lines starting with --
                    if (t.startsWith("--")) continue;
                    System.out.println("Executing: " + (t.length() > 80 ? t.substring(0, 80) + "..." : t));
                    boolean hasResult = st.execute(t);
                    // Nếu là SELECT (trả về ResultSet) -> in kết quả
                    if (hasResult) {
                        try (java.sql.ResultSet rs = st.getResultSet()) {
                            java.sql.ResultSetMetaData md = rs.getMetaData();
                            int cols = md.getColumnCount();
                            // In header
                            StringBuilder header = new StringBuilder();
                            for (int i = 1; i <= cols; i++) {
                                header.append(md.getColumnLabel(i));
                                if (i < cols) header.append(" | ");
                            }
                            System.out.println(header.toString());
                            // In rows (giới hạn 100 hàng để tránh spam)
                            int rowCount = 0;
                            while (rs.next() && rowCount < 100) {
                                StringBuilder row = new StringBuilder();
                                for (int i = 1; i <= cols; i++) {
                                    Object v = rs.getObject(i);
                                    row.append(v == null ? "NULL" : v.toString());
                                    if (i < cols) row.append(" | ");
                                }
                                System.out.println(row.toString());
                                rowCount++;
                            }
                            if (rowCount == 100) System.out.println("... (truncated)");
                        }
                    }
                    executed++;
                }
                conn.commit();
                System.out.println("Executed statements: " + executed);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
}
