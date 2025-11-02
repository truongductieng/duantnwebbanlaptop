package com.ductieng.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.ductieng.exception.EmailExistsException;
import com.ductieng.exception.UsernameExistsException;
import com.ductieng.model.User;
import com.ductieng.service.UserService;

import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ====== VIEW ======
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ====== REGISTER SUBMIT (AJAX JSON) ======
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> doRegister(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password
    ) {
        // Validate tối thiểu
        if (password == null || password.length() < 6) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "field", "password",
                            "message", "Mật khẩu phải ít nhất 6 ký tự"
                    ));
        }

        try {
            User u = new User();
            u.setUsername(username != null ? username.trim() : null);
            u.setEmail(email != null ? email.trim() : null);
            u.setPhone(phone != null ? phone.trim() : null);
            u.setPassword(password); // đảm bảo UserService.save() sẽ encode
            u.setRole("ROLE_USER");

            userService.save(u);
            // Trả JSON đồng nhất cho phía client (register.html đang check resp.ok)
            return ResponseEntity.ok(Map.of("message", "OK"));

        } catch (UsernameExistsException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "field", "username",
                            "message", ex.getMessage()
                    ));
        } catch (EmailExistsException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "field", "email",
                            "message", ex.getMessage()
                    ));
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Đăng ký thất bại. Vui lòng thử lại."));
        }
    }

    // ====== PUBLIC UNIQUE CHECK (AJAX) ======
    @GetMapping("/auth/check-unique")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkUniquePublic(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email
    ) {
        boolean usernameTaken = false;
        boolean emailTaken = false;

        if (username != null && !username.isBlank()) {
            User u = userService.findByUsername(username.trim());
            if (u != null) usernameTaken = true;
        }
        if (email != null && !email.isBlank()) {
            User u = userService.findByEmail(email.trim());
            if (u != null) emailTaken = true;
        }

        return ResponseEntity.ok(Map.of(
                "usernameTaken", usernameTaken,
                "emailTaken", emailTaken
        ));
    }
}
