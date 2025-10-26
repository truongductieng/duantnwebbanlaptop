package com.bigkhoa.config;

import com.bigkhoa.model.User;
import com.bigkhoa.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity // cho phép @PreAuthorize ở controller
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(UserService userService) {
                return username -> {
                        User u = userService.findByUsername(username);
                        if (u == null) {
                                throw new UsernameNotFoundException("Không tìm thấy user: " + username);
                        }
                        System.out.println("Đang xử lý đăng nhập cho user: " + username);
                        System.out.println("Role từ DB: " + u.getRole());
                        // Xử lý role
                        String roleValue = u.getRole();
                        if (roleValue == null || roleValue.trim().isEmpty()) {
                                roleValue = "ROLE_USER";
                        }

                        String role;
                        if (roleValue.startsWith("ROLE_")) {
                                role = roleValue.substring(5); // Bỏ "ROLE_"
                        } else {
                                role = roleValue;
                        }
                        System.out.println("Role sau khi xử lý: " + role);
                        return org.springframework.security.core.userdetails.User
                                        .withUsername(u.getUsername())
                                        .password(u.getPassword())
                                        .roles(role)
                                        .build();
                };
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                // Cho phép Spring 6 đọc CSRF token từ tham số _csrf (hidden field) hoặc header
                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName("_csrf");

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(requestHandler)
                                                // BỎ CSRF cho WebSocket handshake + REST chat (nếu cần)
                                                .ignoringRequestMatchers(
                                                                "/ws-chat/**",
                                                                "/api/chat/**"))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                                                "/", "/laptops/**",
                                                                "/product/**",
                                                                "/login", "/register",
                                                                "/auth/**", // <--- mở quyền cho /auth/check-unique
                                                                "/error",
                                                                "/forgot-password", "/reset-password",
                                                                "/oauth2/**",
                                                                "/ws-chat/**",
                                                                "/contact" // <--- thêm trang liên hệ
                                                ).permitAll()

                                                // Các trang yêu cầu đăng nhập USER
                                                .requestMatchers("/chat", "/cart/**", "/profile/**")
                                                .hasAnyRole("USER", "CUSTOMER", "ADMIN", "STAFF")

                                                // API chat phải đăng nhập
                                                .requestMatchers("/api/chat/partners").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/api/chat/**")
                                                .hasAnyRole("USER", "CUSTOMER", "ADMIN", "STAFF")

                                                .requestMatchers("/my-orders/**")
                                                .hasAnyRole("USER", "CUSTOMER", "ADMIN", "STAFF")
                                                .requestMatchers("/admin/dashboard").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/statistics/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/reviews/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/discounts/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/api/reports/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/chat").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/admin/chat/**").hasAnyRole("ADMIN", "STAFF")
                                                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler((request, response, authentication) -> {
                                                        // Cho popup khuyến mãi chỉ hiện 1 lần sau đăng nhập
                                                        request.getSession().setAttribute("SHOW_DISCOUNT_ANN_ON_LOGIN",
                                                                        Boolean.TRUE);

                                                        boolean isAdmin = authentication.getAuthorities().stream()
                                                                        .anyMatch(a -> a.getAuthority()
                                                                                        .equals("ROLE_ADMIN"));
                                                        boolean isStaff = authentication.getAuthorities().stream()
                                                                        .anyMatch(a -> a.getAuthority()
                                                                                        .equals("ROLE_STAFF"));
                                                        response.sendRedirect(
                                                                        (isAdmin || isStaff) ? "/admin/dashboard"
                                                                                        : "/laptops");
                                                })
                                                .failureUrl("/login?error")
                                                .permitAll())

                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/laptops", true))

                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                return http.build();
        }
}
