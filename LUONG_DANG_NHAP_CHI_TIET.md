# 📚 PHÂN TÍCH LUỒNG ĐĂNG NHẬP - SPRING BOOT + SPRING SECURITY

## 📖 MỤC LỤC

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Frontend - Giao diện đăng nhập](#2-frontend---giao-diện-đăng-nhập)
3. [Spring Security Configuration](#3-spring-security-configuration)
4. [UserDetailsService - Tải thông tin user](#4-userdetailsservice---tải-thông-tin-user)
5. [Authentication Flow - Luồng xác thực](#5-authentication-flow---luồng-xác-thực)
6. [Password Encoding - Mã hóa mật khẩu](#6-password-encoding---mã-hóa-mật-khẩu)
7. [Authorization - Phân quyền](#7-authorization---phân-quyền)
8. [Session Management](#8-session-management)
9. [Logout - Đăng xuất](#9-logout---đăng-xuất)
10. [OAuth2 Login với Google](#10-oauth2-login-với-google)

---

## 1. TỔNG QUAN KIẾN TRÚC

```
┌─────────────┐     HTTP POST      ┌──────────────────┐
│   Browser   │ ──────────────────> │ Spring Security  │
│ (login.html)│     username/pwd    │  Filter Chain    │
└─────────────┘                     └──────────────────┘
                                            │
                                            ↓
                                    ┌──────────────────┐
                                    │ Authentication   │
                                    │   Manager        │
                                    └──────────────────┘
                                            │
                                            ↓
                                    ┌──────────────────┐
                                    │UserDetailsService│──┐
                                    └──────────────────┘  │
                                            │             │
                                            ↓             ↓
                                    ┌──────────────┐  ┌────────────┐
                                    │  UserService │  │PasswordEnc │
                                    └──────────────┘  └────────────┘
                                            │
                                            ↓
                                    ┌──────────────┐
                                    │  Database    │
                                    │ (users table)│
                                    └──────────────┘
```

---

## 2. FRONTEND - GIAO DIỆN ĐĂNG NHẬP

### 📄 File: `login.html`

#### 2.1. Form HTML

```html
<form th:action="@{/login}" method="post" id="loginForm" novalidate>
  <!-- CSRF Token (bắt buộc với Spring Security) -->
  <input
    type="hidden"
    th:name="${_csrf.parameterName}"
    th:value="${_csrf.token}"
  />

  <!-- Username field -->
  <input
    id="username"
    name="username"
    <!--
    PHẢI
    khớp
    với
    .usernameParameter()
    --
  />
  class="form-control" required autofocus />

  <!-- Password field -->
  <input
    id="password"
    type="password"
    name="password"
    <!--
    PHẢI
    khớp
    với
    .passwordParameter()
    --
  />
  class="form-control" required />

  <!-- Remember me -->
  <input type="checkbox" id="rememberMe" name="remember-me" />
  <!-- Spring Security tự xử lý -->

  <button type="submit">Đăng nhập</button>
</form>
```

#### 2.2. Các tham số quan trọng

| Tham số                 | Mục đích            | Giá trị mặc định          |
| ----------------------- | ------------------- | ------------------------- |
| `th:action="@{/login}"` | URL xử lý đăng nhập | `/login` (POST)           |
| `name="username"`       | Tên trường username | Phải khớp config          |
| `name="password"`       | Tên trường password | Phải khớp config          |
| `_csrf` token           | Bảo mật CSRF        | Bắt buộc khi CSRF enabled |
| `name="remember-me"`    | Ghi nhớ đăng nhập   | Tùy chọn                  |

#### 2.3. Hiển thị thông báo lỗi/thành công

```html
<!-- Hiển thị khi URL có ?error -->
<div th:if="${param.error}" class="alert alert-warning">
  Sai tên đăng nhập hoặc mật khẩu.
</div>

<!-- Hiển thị khi URL có ?logout -->
<div th:if="${param.logout}" class="alert alert-success">Đã đăng xuất.</div>
```

#### 2.4. JavaScript - Prevent double submit

```javascript
const form = document.getElementById("loginForm");
const submitBtn = document.getElementById("submitBtn");

form.addEventListener("submit", () => {
  // Disable nút submit để tránh gửi nhiều lần
  submitBtn.disabled = true;
  // Hiển thị spinner loading
  submitBtn.querySelector(".spinner-border").classList.remove("d-none");
});
```

---

## 3. SPRING SECURITY CONFIGURATION

### 📄 File: `SecurityConfig.java`

#### 3.1. Class annotation

```java
@Configuration                // Đánh dấu class này là Spring Configuration
@EnableMethodSecurity        // Bật @PreAuthorize, @Secured trên method
public class SecurityConfig {
    // ...
}
```

#### 3.2. Password Encoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
    // BCrypt: thuật toán hash một chiều, mỗi lần hash sẽ có salt ngẫu nhiên
    // Ví dụ: "password123" -> "$2a$10$N9qo8uLO..."
}
```

**Tại sao dùng BCrypt?**

- ✅ Hash một chiều (không thể decode ngược)
- ✅ Có salt tự động (mỗi lần hash khác nhau)
- ✅ Có độ phức tạp điều chỉnh được (strength/rounds)
- ✅ An toàn trước rainbow table attack

#### 3.3. UserDetailsService Bean

```java
@Bean
public UserDetailsService userDetailsService(UserService userService) {
    return username -> {
        // B1: Tìm user trong database qua UserService
        User u = userService.findByUsername(username);
        if (u == null) {
            throw new UsernameNotFoundException("Không tìm thấy user: " + username);
        }

        // B2: Lấy role và chuẩn hóa
        String userRole = u.getRole();
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "ROLE_USER";  // Mặc định
        }

        // B3: Strip "ROLE_" prefix nếu có
        // (vì .roles() tự thêm "ROLE_" vào đầu)
        String role = userRole.replace("ROLE_", "");

        // B4: Trả về UserDetails object cho Spring Security
        return org.springframework.security.core.userdetails.User
            .withUsername(u.getUsername())
            .password(u.getPassword())      // Password đã hash từ DB
            .roles(role)                    // ADMIN -> ROLE_ADMIN
            .build();
    };
}
```

**Giải thích:**

- `UserDetailsService` là interface Spring Security dùng để load user
- Lambda function nhận `username` từ form login
- Trả về `UserDetails` object chứa: username, password hash, authorities/roles
- Spring Security sẽ so sánh password từ form với password hash từ DB

#### 3.4. Security Filter Chain - Cấu hình bảo mật

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // === CSRF Configuration ===
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");

    http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(requestHandler)
        .ignoringRequestMatchers("/ws-chat/**", "/api/chat/**")  // Bỏ CSRF cho WebSocket
    )

    // === URL Authorization ===
    .authorizeHttpRequests(auth -> auth
        // Public URLs - không cần đăng nhập
        .requestMatchers(
            "/css/**", "/js/**", "/images/**",
            "/", "/laptops/**", "/product/**",
            "/login", "/register",
            "/forgot-password", "/reset-password"
        ).permitAll()

        // Authenticated URLs - phải đăng nhập
        .requestMatchers("/my-orders/**")
            .hasAnyRole("USER", "CUSTOMER", "ADMIN")

        // Admin only
        .requestMatchers("/admin/**").hasRole("ADMIN")

        // Mọi request khác phải authenticated
        .anyRequest().authenticated()
    )

    // === Form Login Configuration ===
    .formLogin(form -> form
        .loginPage("/login")                    // URL trang login
        .usernameParameter("username")          // Tên field username
        .passwordParameter("password")          // Tên field password

        // Success Handler - xử lý sau khi login thành công
        .successHandler((request, response, authentication) -> {
            // Set session attribute
            request.getSession()
                .setAttribute("SHOW_DISCOUNT_ANN_ON_LOGIN", Boolean.TRUE);

            // Redirect dựa theo role
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            response.sendRedirect(
                isAdmin ? "/admin/dashboard" : "/laptops"
            );
        })

        .failureUrl("/login?error")             // Redirect khi login fail
        .permitAll()
    )

    // === OAuth2 Login (Google) ===
    .oauth2Login(oauth2 -> oauth2
        .loginPage("/login")
        .defaultSuccessUrl("/laptops", true)
    )

    // === Logout Configuration ===
    .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?logout")
        .invalidateHttpSession(true)            // Xóa session
        .clearAuthentication(true)              // Xóa authentication
        .deleteCookies("JSESSIONID")            // Xóa cookie session
        .permitAll()
    );

    return http.build();
}
```

---

## 4. USERDETAILSSERVICE - TẢI THÔNG TIN USER

### 4.1. Interface UserService

```java
public interface UserService {
    User findByUsername(String username);
    User save(User u);
    // ... các method khác
}
```

### 4.2. Implementation UserServiceImpl

```java
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        // Tìm user trong database
        return userRepo.findByUsername(username);
    }

    @Override
    public User save(User u) {
        // ... validation ...

        // Mã hóa password trước khi lưu
        if (u.getPassword() != null && !u.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(u.getPassword()));
        }

        return userRepo.save(u);
    }
}
```

### 4.3. UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
}
```

### 4.4. User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;  // Password đã hash (BCrypt)

    @Column(nullable = false)
    private String role;      // ROLE_USER, ROLE_ADMIN, ROLE_CUSTOMER

    // ... getters/setters ...
}
```

---

## 5. AUTHENTICATION FLOW - LUỒNG XÁC THỰC

### 5.1. Luồng xử lý từng bước

```
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 1: User nhập username + password → Submit form         │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 2: Browser gửi POST /login với CSRF token              │
│  - Content-Type: application/x-www-form-urlencoded           │
│  - Body: username=admin&password=123&_csrf=xxx               │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 3: Spring Security Filter Chain nhận request           │
│  - UsernamePasswordAuthenticationFilter                      │
│  - Tạo UsernamePasswordAuthenticationToken (chưa xác thực)   │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 4: AuthenticationManager xử lý                          │
│  - Gọi UserDetailsService.loadUserByUsername(username)       │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 5: UserDetailsService load user từ database            │
│  - UserService.findByUsername(username)                      │
│  - UserRepository.findByUsername(username)                   │
│  - Query: SELECT * FROM users WHERE username = ?             │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 6: So sánh password                                     │
│  - Password từ form: "123" (plain text)                      │
│  - Password từ DB: "$2a$10$..." (BCrypt hash)                │
│  - PasswordEncoder.matches("123", "$2a$10$...")              │
│  - BCrypt hash lại "123" và compare với hash từ DB           │
└──────────────────────────────────────────────────────────────┘
                            ↓
                  ┌─────────┴─────────┐
                  │                   │
            ✅ MATCH              ❌ NO MATCH
                  │                   │
                  ↓                   ↓
      ┌───────────────────┐   ┌──────────────────┐
      │ BƯỚC 7a: SUCCESS  │   │ BƯỚC 7b: FAILURE │
      │ - Tạo Session     │   │ - Redirect        │
      │ - Set cookie      │   │   /login?error    │
      │ - SecurityContext │   │ - AuthenticationEx│
      └───────────────────┘   └──────────────────┘
                  │
                  ↓
      ┌───────────────────────────────────┐
      │ BƯỚC 8: successHandler chạy       │
      │ - Set session attributes          │
      │ - Check role để redirect          │
      │   • ADMIN → /admin/dashboard      │
      │   • USER → /laptops               │
      └───────────────────────────────────┘
```

### 5.2. Code chi tiết từng bước

#### BƯỚC 4-6: Authentication Provider

```java
// Spring Security tự động wire DaoAuthenticationProvider
public class DaoAuthenticationProvider {

    public Authentication authenticate(Authentication auth) {
        String username = auth.getName();
        String password = (String) auth.getCredentials();

        // Load user từ UserDetailsService
        UserDetails user = userDetailsService.loadUserByUsername(username);

        // So sánh password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Sai mật khẩu");
        }

        // Tạo authenticated token
        return new UsernamePasswordAuthenticationToken(
            user, password, user.getAuthorities()
        );
    }
}
```

#### BƯỚC 7: Success Handler

```java
.successHandler((request, response, authentication) -> {
    // 1. Lấy session
    HttpSession session = request.getSession();

    // 2. Set custom attributes
    session.setAttribute("SHOW_DISCOUNT_ANN_ON_LOGIN", Boolean.TRUE);

    // 3. Lấy thông tin user đã authenticated
    String username = authentication.getName();
    Collection<? extends GrantedAuthority> authorities =
        authentication.getAuthorities();

    // 4. Check role
    boolean isAdmin = authorities.stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    // 5. Redirect theo role
    String redirectUrl = isAdmin ? "/admin/dashboard" : "/laptops";
    response.sendRedirect(redirectUrl);
})
```

---

## 6. PASSWORD ENCODING - MÃ HÓA MẬT KHẨU

### 6.1. Khi đăng ký user mới

```java
@Override
public User save(User u) {
    // Kiểm tra password có được nhập không
    if (u.getPassword() != null && !u.getPassword().isBlank()) {
        // Mã hóa password trước khi lưu DB
        String encodedPassword = passwordEncoder.encode(u.getPassword());
        u.setPassword(encodedPassword);

        // Ví dụ:
        // Input: "mypassword123"
        // Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
    }

    return userRepo.save(u);
}
```

### 6.2. Khi login - so sánh password

```java
// Spring Security tự động gọi
boolean matches = passwordEncoder.matches(
    "mypassword123",                                          // Raw password từ form
    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"  // Hash từ DB
);

// matches = true → Đăng nhập thành công
// matches = false → Sai mật khẩu
```

### 6.3. BCrypt Hash Structure

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│  │ │  │                                                    │
│  │ │  └─── Salt (22 chars, random)                       │
│  │ └────── Cost factor (10 = 2^10 rounds = 1024 rounds)  │
│  └──────── BCrypt version                                │
└─────────── Hash result (31 chars)                        │
```

---

## 7. AUTHORIZATION - PHÂN QUYỀN

### 7.1. URL-based Authorization (trong SecurityConfig)

```java
.authorizeHttpRequests(auth -> auth
    // Public - ai cũng truy cập được
    .requestMatchers("/", "/login", "/register").permitAll()

    // Authenticated - phải đăng nhập
    .requestMatchers("/profile/**").authenticated()

    // Role-based - phải có role cụ thể
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/my-orders/**").hasAnyRole("USER", "CUSTOMER", "ADMIN")

    // Default - mọi request khác phải authenticated
    .anyRequest().authenticated()
)
```

### 7.2. Method-level Authorization (với @PreAuthorize)

```java
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")  // ← Cả class phải là ADMIN
public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard() {
        // Chỉ ADMIN mới vào được
        return "admin/dashboard";
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")  // ← Override class-level
    public String listUsers() {
        // ADMIN hoặc SUPER_ADMIN đều vào được
        return "admin/users";
    }
}
```

### 7.3. Lấy thông tin user đã login trong Controller

```java
@GetMapping("/profile")
public String profile(Model model, Principal principal) {
    // Cách 1: Dùng Principal
    String username = principal.getName();
    User user = userService.findByUsername(username);

    model.addAttribute("user", user);
    return "profile";
}

@GetMapping("/orders")
public String orders(Authentication authentication) {
    // Cách 2: Dùng Authentication
    String username = authentication.getName();
    Collection<? extends GrantedAuthority> roles =
        authentication.getAuthorities();

    // ...
}

@GetMapping("/settings")
public String settings(@AuthenticationPrincipal UserDetails userDetails) {
    // Cách 3: Dùng @AuthenticationPrincipal
    String username = userDetails.getUsername();
    // ...
}
```

### 7.4. Thymeleaf - Hiển thị theo role

```html
<!-- Chỉ hiển thị khi user đã đăng nhập -->
<div sec:authorize="isAuthenticated()">
  Xin chào, <span sec:authentication="name">User</span>
</div>

<!-- Chỉ hiển thị khi chưa đăng nhập -->
<div sec:authorize="!isAuthenticated()">
  <a href="/login">Đăng nhập</a>
</div>

<!-- Chỉ hiển thị cho ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
  <a href="/admin/dashboard">Quản trị</a>
</div>

<!-- Hiển thị cho USER hoặc ADMIN -->
<div sec:authorize="hasAnyRole('USER', 'ADMIN')">
  <a href="/my-orders">Đơn hàng của tôi</a>
</div>
```

---

## 8. SESSION MANAGEMENT

### 8.1. Session được tạo khi nào?

```java
// Sau khi login thành công, Spring Security:
HttpSession session = request.getSession(true);  // Tạo session mới

// Lưu SecurityContext vào session
SecurityContext context = SecurityContextHolder.getContext();
session.setAttribute("SPRING_SECURITY_CONTEXT", context);

// Set cookie JSESSIONID
// JSESSIONID=ABC123XYZ (HttpOnly, Secure nếu HTTPS)
```

### 8.2. Cấu hình Session (nếu cần custom)

```java
http.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .maximumSessions(1)                    // Chỉ cho 1 session/user
    .maxSessionsPreventsLogin(true)        // Block login mới nếu đã có session
    .expiredUrl("/login?expired")          // Redirect khi session hết hạn
);
```

### 8.3. Remember Me

```java
http.rememberMe(remember -> remember
    .key("uniqueAndSecret")                // Secret key
    .tokenValiditySeconds(86400 * 7)       // 7 ngày
    .rememberMeParameter("remember-me")    // Tên checkbox
    .rememberMeCookieName("remember-me-cookie")
);
```

Khi user check "Nhớ đăng nhập":

- Browser lưu cookie `remember-me-cookie` (expire sau 7 ngày)
- Lần sau vào trang, Spring Security tự động login từ cookie

---

## 9. LOGOUT - ĐĂNG XUẤT

### 9.1. Cấu hình Logout

```java
.logout(logout -> logout
    .logoutUrl("/logout")                  // URL POST để logout
    .logoutSuccessUrl("/login?logout")     // Redirect sau logout
    .invalidateHttpSession(true)           // Xóa session
    .clearAuthentication(true)             // Xóa authentication
    .deleteCookies("JSESSIONID", "remember-me-cookie")  // Xóa cookies
    .permitAll()
)
```

### 9.2. Logout button trong HTML

```html
<form th:action="@{/logout}" method="post">
  <input
    type="hidden"
    th:name="${_csrf.parameterName}"
    th:value="${_csrf.token}"
  />
  <button type="submit" class="btn btn-danger">Đăng xuất</button>
</form>
```

### 9.3. Luồng Logout

```
1. User click "Đăng xuất"
   ↓
2. Form POST /logout với CSRF token
   ↓
3. LogoutFilter nhận request
   ↓
4. SecurityContextLogoutHandler xử lý:
   - session.invalidate()           → Xóa session
   - SecurityContextHolder.clearContext()  → Xóa context
   - Cookie.setMaxAge(0)            → Xóa cookies
   ↓
5. LogoutSuccessHandler redirect
   → /login?logout
```

---

## 10. OAUTH2 LOGIN VỚI GOOGLE

### 10.1. Cấu hình application.properties

```properties
# OAuth2 Google Client
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
```

### 10.2. Cấu hình trong SecurityConfig

```java
.oauth2Login(oauth2 -> oauth2
    .loginPage("/login")                   // Trang login custom
    .defaultSuccessUrl("/laptops", true)   // Redirect sau OAuth2 login
)
```

### 10.3. Button login Google trong HTML

```html
<a href="/oauth2/authorization/google" class="btn btn-outline-danger">
  <i class="fab fa-google me-2"></i>
  Đăng nhập với Google
</a>
```

### 10.4. Luồng OAuth2 Login

```
1. User click "Đăng nhập với Google"
   ↓
2. Redirect → https://accounts.google.com/o/oauth2/auth?...
   (User đăng nhập Google)
   ↓
3. Google redirect về: /login/oauth2/code/google?code=ABC123
   ↓
4. Spring Security OAuth2LoginAuthenticationFilter:
   - Đổi code → access_token
   - Gọi Google API lấy user info
   ↓
5. Tạo OAuth2User với info từ Google
   - email, name, picture
   ↓
6. Kiểm tra user đã tồn tại chưa?
   - Có → Login
   - Chưa → Tạo user mới → Login
   ↓
7. Redirect → /laptops
```

---

## 📌 TÓM TẮT QUAN TRỌNG

### ✅ Checklist khi implement Login

1. **Frontend (login.html)**

   - [ ] Form POST đến `/login`
   - [ ] Field `name="username"` và `name="password"` khớp config
   - [ ] CSRF token hidden field
   - [ ] Hiển thị lỗi với `th:if="${param.error}"`

2. **Backend - Entity & Repository**

   - [ ] User entity với `username`, `password`, `role`
   - [ ] UserRepository có method `findByUsername()`
   - [ ] Password được hash bằng BCrypt trước khi lưu DB

3. **Backend - Security Config**

   - [ ] Bean `PasswordEncoder` (BCrypt)
   - [ ] Bean `UserDetailsService` load user từ DB
   - [ ] `SecurityFilterChain` config:
     - Form login với username/password parameter
     - Success handler redirect theo role
     - Failure URL: `/login?error`
   - [ ] URL authorization rules
   - [ ] CSRF enabled (mặc định)

4. **Testing**
   - [ ] Login thành công → redirect đúng URL theo role
   - [ ] Login sai password → hiện lỗi
   - [ ] Login user không tồn tại → hiện lỗi
   - [ ] Session được tạo sau login
   - [ ] Logout xóa session và redirect

---

## 🎓 BÀI TẬP THỰC HÀNH

### Bài 1: Thêm field "email" để login

- Cho phép user login bằng email thay vì username
- Hint: Custom `UserDetailsService` để check cả username và email

### Bài 2: Lock account sau 5 lần đăng nhập sai

- Thêm field `failedLoginAttempts`, `locked` vào User entity
- Custom `AuthenticationFailureHandler`

### Bài 3: Redirect về trang trước đó sau login

- Hint: Dùng `SavedRequestAwareAuthenticationSuccessHandler`

### Bài 4: Thêm captcha cho login form

- Google reCAPTCHA v2
- Verify token phía server

---

## 📚 TÀI LIỆU THAM KHẢO

1. **Spring Security Official Docs**

   - https://docs.spring.io/spring-security/reference/

2. **Baeldung - Spring Security**

   - https://www.baeldung.com/spring-security-login

3. **BCrypt Password Encoder**

   - https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt

4. **OAuth2 with Spring Boot**
   - https://spring.io/guides/tutorials/spring-boot-oauth2/

---

## ❓ CÂUI HỎI THƯỜNG GẶP (FAQ)

### Q1: Tại sao cần CSRF token?

**A:** CSRF (Cross-Site Request Forgery) attack xảy ra khi website độc hại lợi dụng session của user đã login để gửi request giả mạo. CSRF token đảm bảo request thực sự từ form của bạn.

### Q2: BCrypt hash mỗi lần khác nhau, vậy làm sao so sánh?

**A:** BCrypt hash chứa salt ngẫu nhiên. Khi so sánh, BCrypt sẽ:

1. Extract salt từ hash đã lưu
2. Hash lại password input với salt đó
3. Compare 2 hash

### Q3: Session timeout là bao lâu?

**A:** Mặc định 30 phút. Config trong `application.properties`:

```properties
server.servlet.session.timeout=30m
```

### Q4: Làm sao redirect về trang trước đó sau login?

**A:** Dùng `SavedRequestAwareAuthenticationSuccessHandler`:

```java
.successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
```

### Q5: Có thể dùng cả username VÀ email để login không?

**A:** Có, custom `UserDetailsService`:

```java
return username -> {
    User u = userService.findByUsernameOrEmail(username);
    // ...
}
```

---

**Chúc bạn học tốt! 🚀**
