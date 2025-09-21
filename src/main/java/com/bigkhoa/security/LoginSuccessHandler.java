package com.bigkhoa.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Đặt cờ trong session để hiển thị thông báo mã giảm giá đúng 1 lần sau khi đăng nhập.
 */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    public static final String SHOW_DISCOUNT_ANN_ON_LOGIN = "SHOW_DISCOUNT_ANN_ON_LOGIN";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        request.getSession().setAttribute(SHOW_DISCOUNT_ANN_ON_LOGIN, Boolean.TRUE);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
