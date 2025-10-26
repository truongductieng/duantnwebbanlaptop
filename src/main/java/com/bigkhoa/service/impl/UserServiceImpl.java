package com.bigkhoa.service.impl;

import com.bigkhoa.exception.EmailExistsException;
import com.bigkhoa.exception.UsernameExistsException;
import com.bigkhoa.model.User;
import com.bigkhoa.repository.OrderRepository;
import com.bigkhoa.repository.PasswordResetTokenRepository;
import com.bigkhoa.repository.UserRepository;
import com.bigkhoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepo;
    private final OrderRepository orderRepo;

    @Autowired
    public UserServiceImpl(UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository tokenRepo,
            OrderRepository orderRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepo = tokenRepo;
        this.orderRepo = orderRepo;
    }

    @Override
    public List<User> findAll() {
        return userRepo.findAll();
    }

    @Override
    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User id=" + id));
    }

    @Override
    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        if (email == null)
            return null;
        return userRepo.findByEmailIgnoreCase(email.trim()).orElse(null);
    }

    @Override
    public User save(User u) {
        // Chuẩn hoá chuỗi
        if (u.getUsername() != null)
            u.setUsername(u.getUsername().trim());
        if (u.getEmail() != null)
            u.setEmail(u.getEmail().trim().toLowerCase());

        // --- KIỂM TRA TRÙNG USERNAME/EMAIL ---
        if (u.getId() == null) {
            if (userRepo.existsByUsernameIgnoreCase(u.getUsername())) {
                throw new UsernameExistsException("Tên đăng nhập đã được sử dụng, vui lòng chọn tên khác");
            }
            if (u.getEmail() != null && userRepo.existsByEmailIgnoreCase(u.getEmail())) {
                throw new EmailExistsException("Email này đã có người sử dụng, vui lòng đổi email khác");
            }
        } else {
            // Khi cập nhật: nếu thay đổi username/email thì cũng phải check trùng
            User current = userRepo.findById(u.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User id=" + u.getId()));

            if (!current.getUsername().equalsIgnoreCase(u.getUsername())
                    && userRepo.existsByUsernameIgnoreCase(u.getUsername())) {
                throw new UsernameExistsException("Tên đăng nhập đã được sử dụng, vui lòng chọn tên khác");
            }
            if (u.getEmail() != null
                    && !u.getEmail().equalsIgnoreCase(current.getEmail())
                    && userRepo.existsByEmailIgnoreCase(u.getEmail())) {
                throw new EmailExistsException("Email này đã có người sử dụng, vui lòng đổi email khác");
            }
        }

        // Xử lý role
        if (u.getRole() == null || u.getRole().trim().isEmpty()) {
            u.setRole("ROLE_USER");
        } else if (!u.getRole().startsWith("ROLE_")) {
            // Thêm prefix ROLE_ nếu chưa có
            u.setRole("ROLE_" + u.getRole());
        }

        // Xử lý mật khẩu
        if (u.getPassword() != null && !u.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        } else if (u.getId() != null) {
            // Khi edit mà không nhập password -> giữ nguyên
            String existing = userRepo.findById(u.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User id=" + u.getId()))
                    .getPassword();
            u.setPassword(existing);
        } else {
            throw new IllegalArgumentException("Password không được để trống");
        }

        return userRepo.save(u);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        tokenRepo.deleteByUserId(id);
        orderRepo.deleteByCustomerId(id);
        userRepo.deleteById(id);
    }

    @Override
    public void updatePassword(String username, String newPassword) {
        User u = userRepo.findByUsername(username);
        if (u != null) {
            u.setPassword(passwordEncoder.encode(newPassword));
            userRepo.save(u);
        }
    }
}
