package com.bigkhoa.service;

import com.bigkhoa.model.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(Long id);
    User findByUsername(String username);
    User findByEmail(String email);        // <— thêm để phục vụ quên mật khẩu (nhập email)
    User save(User u);
    void deleteById(Long id);
    void updatePassword(String username, String newPassword);
}
