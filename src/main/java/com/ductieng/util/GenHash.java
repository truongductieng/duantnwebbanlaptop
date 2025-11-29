package com.ductieng.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

<<<<<<< HEAD
/**
 * Small utility to generate a BCrypt hash for a password.
 * Run with: mvn -DskipTests compile exec:java -Dexec.mainClass=com.ductieng.util.GenHash -Dexec.args="123"
 */
public class GenHash {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Usage: GenHash <password>");
            System.exit(1);
        }
        String pwd = args[0];
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode(pwd);
=======
public class GenHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("123");
>>>>>>> fac37272204c35f27c71a5c92ea772ec1e667fe9
        System.out.println(hash);
    }
}
