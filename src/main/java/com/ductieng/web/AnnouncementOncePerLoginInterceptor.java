package com.ductieng.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ductieng.security.LoginSuccessHandler;

@Component
public class AnnouncementOncePerLoginInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    HttpSession session = req.getSession(false);
    if (session != null && Boolean.TRUE.equals(session.getAttribute(LoginSuccessHandler.SHOW_DISCOUNT_ANN_ON_LOGIN))) {
      req.setAttribute("showDiscountAnn", true);
      session.removeAttribute(LoginSuccessHandler.SHOW_DISCOUNT_ANN_ON_LOGIN);
    }
    return true;
  }
}
