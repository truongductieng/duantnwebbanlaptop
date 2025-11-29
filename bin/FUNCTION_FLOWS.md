# TÀI LIỆU MÔ TẢ LUỒNG CHỨC NĂNG CHI TIẾT

**Dự án**: Website Bán Laptop  
**Ngày tạo**: 24/11/2025  
**Công nghệ**: Spring Boot 3.x, Thymeleaf, MySQL, WebSocket

---

## MỤC LỤC

1. [Authentication & Authorization](#1-authentication--authorization)
2. [Quản lý Giỏ hàng & Đặt hàng](#2-quản-lý-giỏ-hàng--đặt-hàng)
3. [Thanh toán VNPay](#3-thanh-toán-vnpay)
4. [Quản lý Đơn hàng (Admin)](#4-quản-lý-đơn-hàng-admin)
5. [Trả hàng (Returns)](#5-trả-hàng-returns)
6. [Chat realtime (WebSocket)](#6-chat-realtime-websocket)
7. [Mã giảm giá (Discount)](#7-mã-giảm-giá-discount)
8. [Thống kê & Dashboard](#8-thống-kê--dashboard)
9. [Đổi mật khẩu & Quên mật khẩu](#9-đổi-mật-khẩu--quên-mật-khẩu)

---

## 1. AUTHENTICATION & AUTHORIZATION

### 1.1. Đăng ký (Registration)

**Endpoint**: `POST /register`  
**Controller**: `AuthController.doRegister()`  
**Template**: `register.html`

**Luồng xử lý**:

```
┌─────────────┐
│   Client    │
│ (register   │
│   .html)    │
└──────┬──────┘
       │ 1. User điền form
       │    - username, email, phone, password
       │
       │ 2. Client-side validation (JS)
       │    - Check password length >= 6
       │    - Check phone format (10-15 digits)
       │    - AJAX call /auth/check-unique để kiểm tra trùng
       │      username/email realtime khi gõ
       │
       ▼
┌──────────────────┐
│ POST /register   │ (AJAX)
└──────┬───────────┘
       │
       ▼
┌────────────────────────────────────┐
│ AuthController.doRegister()        │
│                                    │
│ 1. Validate password >= 6 chars    │
│ 2. Create User object              │
│    - set username, email, phone    │
│    - set role = "ROLE_USER"        │
│ 3. Call userService.save(user)     │
└──────┬─────────────────────────────┘
       │
       ▼
┌────────────────────────────────────┐
│ UserServiceImpl.save()             │
│                                    │
│ 1. Chuẩn hóa: trim, lowercase email│
│ 2. Kiểm tra trùng username/email:  │
│    - userRepo.existsByUsername()   │
│    - userRepo.existsByEmail()      │
│    → Throw exception nếu trùng     │
│ 3. Mã hóa password (BCrypt):       │
│    - passwordEncoder.encode()      │
│ 4. Set default role "ROLE_USER"    │
│ 5. Save vào database               │
└──────┬─────────────────────────────┘
       │
       ▼
   Response
   ┌─────────────────────────┐
   │ 200 OK: {message:"OK"}  │
   │ 409 CONFLICT: trùng user│
   │ 409 CONFLICT: trùng email│
   │ 400 BAD REQUEST: lỗi khác│
   └─────────────────────────┘
```

**Validation**:

- **Client-side** (register.html JavaScript):
  - Password tối thiểu 6 ký tự
  - Phone format: 10-15 chữ số
  - Check unique qua AJAX `/auth/check-unique`
- **Server-side** (UserServiceImpl):
  - Username unique (case-insensitive)
  - Email unique (case-insensitive)
  - Password encode BCrypt

**Exceptions**:

- `UsernameExistsException` → 409 với field="username"
- `EmailExistsException` → 409 với field="email"

---

### 1.2. Đăng nhập (Login)

**Endpoint**: `POST /login` (Spring Security auto-handle)  
**Controller**: Spring Security `formLogin`  
**Template**: `login.html`

**Luồng xử lý**:

```
┌─────────────┐
│   Client    │
│ (login.html)│
└──────┬──────┘
       │ 1. User điền form
       │    - username (hoặc email)
       │    - password
       │    - CSRF token (hidden)
       │
       ▼
┌──────────────────┐
│ POST /login      │
└──────┬───────────┘
       │
       ▼
┌────────────────────────────────────────┐
│ Spring Security Filter Chain           │
│                                        │
│ 1. Extract username & password         │
│ 2. Call UserDetailsService             │
└──────┬─────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────┐
│ SecurityConfig.userDetailsService()    │
│                                        │
│ 1. Call userService.findByUsername()   │
│ 2. If null → UsernameNotFoundException │
│ 3. Extract role, default "ROLE_USER"   │
│ 4. Build UserDetails with:             │
│    - username                           │
│    - encoded password                   │
│    - roles                              │
└──────┬─────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────┐
│ Password Matching                      │
│                                        │
│ BCrypt compare:                        │
│   passwordEncoder.matches(             │
│     rawPassword,                       │
│     storedEncodedPassword)             │
└──────┬─────────────────────────────────┘
       │
       ├─ Match ──────────────────────────┐
       │                                  │
       ▼                                  ▼
   SUCCESS                            FAILURE
   ┌─────────────────────┐         ┌──────────────────────┐
   │ LoginSuccessHandler │         │ Redirect:            │
   │                     │         │ /login?error         │
   │ 1. Set session attr:│         └──────────────────────┘
   │    SHOW_DISCOUNT_   │
   │    ANN_ON_LOGIN     │
   │ 2. Check role:      │
   │    - ADMIN →        │
   │      /admin/        │
   │      dashboard      │
   │    - USER →         │
   │      /laptops       │
   └─────────────────────┘
```

**Security Config** (`SecurityConfig.java`):

- **UserDetailsService**: Load user từ `UserService.findByUsername()`
- **PasswordEncoder**: BCryptPasswordEncoder
- **Success Handler**:
  - Set session attribute cho popup khuyến mãi
  - Redirect theo role (ADMIN → dashboard, USER → laptops)
- **Failure**: Redirect `/login?error`

**OAuth2 Login** (Google):

- Endpoint: `/oauth2/authorization/google`
- Config: `oauth2Login().loginPage("/login").defaultSuccessUrl("/laptops")`
- Sau khi Google auth thành công → redirect `/laptops`

---

### 1.3. Phân quyền (Authorization)

**Roles**:

- `ROLE_ADMIN` - Quản trị viên
- `ROLE_USER` / `ROLE_CUSTOMER` - Khách hàng

**Protected Routes**:

```java
// SecurityConfig.java - authorizeHttpRequests()

PUBLIC (permitAll):
- /, /laptops/**, /product/**
- /login, /register, /auth/**
- /forgot-password, /reset-password
- /oauth2/**, /error, /contact
- /css/**, /js/**, /images/**
- /ws-chat/** (WebSocket handshake)

AUTHENTICATED (anyRequest().authenticated()):
- /checkout, /cart
- /profile, /change-password

ROLE_ADMIN (@PreAuthorize("hasRole('ADMIN')")):
- /admin/** (tất cả admin routes)

ROLE_USER/CUSTOMER:
- /my-orders/**, /profile/**
- /chat (customer chat view)

ADMIN-ONLY API:
- /api/chat/partners (lấy danh sách chat partners)
```

**Method-level Security**:

```java
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
public class AdminController { ... }
```

---

## 2. QUẢN LÝ GIỎ HÀNG & ĐẶT HÀNG

### 2.1. Thêm vào giỏ hàng

**Endpoint**: `POST /cart/add`  
**Controller**: `CartController.addToCart()`

**Luồng**:

```
Product Page
    │
    │ Click "Thêm vào giỏ"
    │ (productId, quantity)
    ▼
POST /cart/add
    │
    ▼
CartController.addToCart()
    │
    ├─ Kiểm tra authenticated?
    │  └─ Nếu chưa login → redirect /login
    │
    ├─ Validate quantity > 0
    │
    ├─ Call cartService.addToCart(id, qty, auth)
    │
    ▼
CartService (session-based)
    │
    ├─ Lấy cart từ session
    ├─ Load Laptop từ DB
    ├─ Kiểm tra tồn kho >= quantity
    │  └─ Nếu không đủ → throw exception
    ├─ Tìm item trong cart:
    │  - Nếu đã có → cộng dồn quantity
    │  - Nếu chưa → tạo CartItem mới
    ├─ Save cart vào session
    │
    ▼
Redirect /cart (hiển thị giỏ)
```

**Session Cart Structure**:

```java
List<CartItem> cart = session.getAttribute("cart");
// CartItem { Laptop laptop, int quantity }
```

---

### 2.2. Checkout (Trang thanh toán)

**Endpoint**: `GET /checkout`  
**Controller**: `CheckoutController.showCheckoutForm()`

**Luồng**:

```
Cart Page
    │ Click "Thanh toán"
    ▼
GET /checkout
    │
    ▼
CheckoutController.showCheckoutForm()
    │
    ├─ Load cart items từ session
    ├─ Tính totalPrice (sum của price × quantity)
    ├─ Lấy discount info từ session:
    │  - discountCode
    │  - discountPercent
    │  - discountAmount
    ├─ Tính totalAfterDiscount = totalPrice - discountAmount
    ├─ Pre-populate CheckoutForm với thông tin discount
    │
    ▼
Render checkout.html
    │
    └─ Form fields:
       - fullName, email, phone, address
       - paymentMethod (COD / VNPAY)
       - Hidden: discountCode, discountPercent, discountAmount
```

---

### 2.3. Xử lý đặt hàng

**Endpoint**: `POST /checkout`  
**Controller**: `CheckoutController.processCheckout()`

**Luồng**:

```
checkout.html
    │ Submit form
    ▼
POST /checkout
    │
    ▼
CheckoutController.processCheckout()
    │
    ├─ 1. Load User từ Authentication
    │    (hỗ trợ cả local login & OAuth2)
    │
    ├─ 2. Security: Lấy discount từ SESSION
    │    (không tin hidden fields từ client)
    │    - discountCode, discountPercent
    │
    ├─ 3. Tính lại discountAmount từ percent
    │    discountAmount = totalPrice × percent / 100
    │
    ├─ 4. Gán vào CheckoutForm:
    │    - discountCode, discountPercent
    │    - discountAmount, totalAfterDiscount
    │
    ├─ 5. Call orderService.createOrder(user, items, form)
    │
    ▼
OrderService.createOrder()
    │
    ├─ 1. KIỂM TRA TỒN KHO:
    │    For each item:
    │      if (laptop.quantity < item.quantity)
    │        → throw IllegalStateException
    │
    ├─ 2. Tạo Order object:
    │    - customer = user
    │    - status = PENDING
    │    - recipientName, email, phone, address
    │    - paymentMethod (COD / VNPAY)
    │
    ├─ 3. Tính subtotal (totalBeforeDiscount)
    │    For each CartItem:
    │      - Tạo OrderItem
    │      - unitPrice = laptop.price
    │      - subtotal += unitPrice × quantity
    │
    ├─ 4. Áp dụng discount:
    │    - Lưu discountCode, discountPercent, discountAmount
    │    - Validate: discountAmount >= 0 && <= subtotal
    │    - total = subtotal - discountAmount (>= 0)
    │
    ├─ 5. TRỪ TỒN KHO:
    │    For each item:
    │      laptop.quantity -= item.quantity
    │      laptopRepo.save(laptop)
    │
    ├─ 6. Save Order vào DB
    │
    ▼
Gửi email xác nhận
    │
    └─ gmailService.sendOrderConfirmationEmail(order)
       - Email: thông tin đơn hàng, trạng thái PENDING
```

**Phân luồng theo Payment Method**:

```
if (paymentMethod == "VNPAY"):
    ├─ Tạo VNPay payment URL
    │  - vnpService.createPayment(total, fullName, orderId, ipAddress)
    ├─ Clear cart & session discount
    └─ Redirect to VNPay gateway

else (COD):
    ├─ Clear cart & session discount
    ├─ Flash message: "Đặt hàng thành công!"
    └─ Redirect /confirmation/{orderId}
```

---

## 3. THANH TOÁN VNPAY

### 3.1. Tạo Payment URL

**Service**: `VNPayService.createPayment()`

**Luồng**:

```
CheckoutController
    │ if (paymentMethod == "VNPAY")
    ▼
vnpService.createPayment(amount, fullName, orderId, ipAddress)
    │
    ├─ Build VNPay params:
    │  - vnp_Version = "2.1.0"
    │  - vnp_Command = "pay"
    │  - vnp_TmnCode = Config.vnp_TmnCode
    │  - vnp_Amount = amount × 100 (VNPay yêu cầu đơn vị VND × 100)
    │  - vnp_TxnRef = orderId.toString()
    │  - vnp_OrderInfo = "Thanh toan don hang #[orderId] cho [fullName]"
    │  - vnp_ReturnUrl = Config.vnp_ReturnUrl (http://localhost:8080/payment)
    │  - vnp_Locale = "vn"
    │  - vnp_IpAddr = ipAddress
    │  - vnp_CreateDate = yyyyMMddHHmmss
    │  - vnp_ExpireDate = createDate + 15 minutes
    │
    ├─ Sort params by key (alphabetically)
    │
    ├─ Build hashData & queryString:
    │  - URL encode each param
    │  - Concatenate: key1=value1&key2=value2&...
    │
    ├─ Generate secure hash:
    │  - vnp_SecureHash = HMAC_SHA512(secretKey, hashData)
    │
    ├─ Build final payment URL:
    │  - https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
    │    ?[queryString]&vnp_SecureHash=[hash]
    │
    └─ Return payment URL
```

**Config** (`Config.java`):

```java
vnp_TmnCode = "TCOSJRCY"
secretKey = "N4ZB7OYQOD48WVSEITD2JOFX2X6F2IB4"
vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
vnp_ReturnUrl = "http://localhost:8080/payment"
```

---

### 3.2. Xử lý VNPay Callback

**Endpoint**: `GET /payment`  
**Controller**: `CheckoutController.handleVNPayReturn()`

**Luồng**:

```
VNPay Gateway
    │ User thanh toán xong
    │ VNPay redirect về: /payment?vnp_ResponseCode=00&vnp_TxnRef=108&...
    ▼
GET /payment
    │
    ▼
CheckoutController.handleVNPayReturn(params)
    │
    ├─ 1. Validate params:
    │    - Kiểm tra vnp_TxnRef (orderId) có tồn tại?
    │    - Parse orderId từ string → Long
    │
    ├─ 2. Check vnp_ResponseCode:
    │
    ├─ IF responseCode == "00" (SUCCESS):
    │  │
    │  ├─ Update order status: PENDING → CONFIRMED
    │  │  - orderService.updateStatus(orderId, CONFIRMED)
    │  │
    │  ├─ Gửi email xác nhận thanh toán thành công
    │  │  - gmailService.sendOrderConfirmationEmail(order)
    │  │
    │  ├─ Flash message: "Thanh toán VNPay thành công"
    │  │
    │  └─ Redirect /confirmation/{orderId}
    │
    └─ ELSE (FAILURE / CANCELED):
       │
       ├─ Giữ order status = PENDING
       │  (cho phép thanh toán lại)
       │
       ├─ Log warning với responseCode
       │
       ├─ Flash error: "Thanh toán thất bại/hủy"
       │
       └─ Redirect /profile/order/{orderId}
```

**VNPay Response Codes**:

- `00` - Giao dịch thành công
- `24` - Khách hàng hủy giao dịch
- `71` - Website chưa được phê duyệt (Return URL chưa whitelist)
- Khác - Lỗi giao dịch

---

### 3.3. Thanh toán lại (Retry Payment)

**Endpoint**: `POST /profile/order/{id}/retry-payment`  
**Controller**: `ProfileController.retryPayment()`

**Điều kiện**:

- Order status = PENDING hoặc CANCELED
- Payment method = VNPAY
- User là chủ đơn hàng

**Luồng**:

```
Order Detail Page
    │ Nếu order.status = PENDING && paymentMethod = VNPAY
    │ Hiện nút "Thanh toán lại"
    ▼
POST /profile/order/{id}/retry-payment
    │
    ▼
ProfileController.retryPayment()
    │
    ├─ Validate ownership: order.customer == currentUser
    │
    ├─ Validate status: PENDING hoặc CANCELED
    │
    ├─ Validate payment method: VNPAY only
    │
    ├─ Tạo VNPay payment URL mới:
    │  - vnpService.createPayment(order.total, order.recipientName, orderId)
    │
    ├─ Nếu status = CANCELED:
    │  - Update status: CANCELED → PENDING
    │
    └─ Redirect to VNPay payment URL
```

---

## 4. QUẢN LÝ ĐƠN HÀNG (ADMIN)

### 4.1. Danh sách đơn hàng

**Endpoint**: `GET /admin/orders`  
**Controller**: `AdminController.listOrders()`

**Luồng**:

```
Admin Dashboard
    │ Click "Đơn hàng"
    ▼
GET /admin/orders?status={status}
    │
    ▼
AdminController.listOrders(status)
    │
    ├─ If status == null:
    │  - Load tất cả orders
    │  - orderService.findAll()
    │
    ├─ Else:
    │  - Filter by status
    │  - orderService.getByStatus(status)
    │
    ├─ Sort by createdAt DESC (mới nhất trước)
    │
    └─ Render admin/orders.html
       - Table: ID, Khách, Tổng tiền, Trạng thái, Ngày tạo
       - Actions: Xem chi tiết, Cập nhật trạng thái, Hủy
```

**Filter Options**:

- Tất cả
- PENDING (Chờ xử lý)
- CONFIRMED (Đã xác nhận)
- SHIPPING (Đang giao)
- DELIVERED (Đã giao)
- CANCELED (Đã hủy)

---

### 4.2. Chi tiết đơn hàng

**Endpoint**: `GET /admin/orders/{id}`  
**Controller**: `AdminController.orderDetail()`

**Hiển thị**:

- Thông tin khách hàng
- Thông tin người nhận
- Danh sách sản phẩm (OrderItems)
- Trạng thái đơn hàng
- Discount info (nếu có)
- Timeline trạng thái
- Form cập nhật trạng thái

---

### 4.3. Cập nhật trạng thái

**Endpoint**: `POST /admin/orders/{id}/status`  
**Controller**: `AdminController.updateOrderStatus()`

**Luồng**:

```
Admin Order Detail
    │ Select new status
    │ Click "Cập nhật"
    ▼
POST /admin/orders/{id}/status
    │ status = newStatus
    ▼
AdminController.updateOrderStatus()
    │
    ├─ Parse OrderStatus from string
    │
    ├─ Call orderService.updateStatus(id, newStatus)
    │
    ▼
OrderService.updateStatus()
    │
    ├─ Load order by ID
    │
    ├─ Update order.status = newStatus
    │
    ├─ If newStatus == DELIVERED:
    │  - Set deliveredAt = LocalDateTime.now()
    │
    └─ Save order

    ▼
Redirect /admin/orders/{id}
```

**Status Transitions**:

```
PENDING → CONFIRMED → SHIPPING → DELIVERED
   ↓
CANCELED (chỉ từ PENDING)
```

---

### 4.4. Hủy đơn hàng (Admin)

**Endpoint**: `POST /admin/orders/{id}/cancel`  
**Controller**: `AdminController.cancelOrderByAdmin()`

**Luồng**:

```
Admin Order Detail
    │ Click "Hủy đơn"
    │ Nhập lý do (optional)
    ▼
POST /admin/orders/{id}/cancel
    │ reason = cancelReason
    ▼
AdminController.cancelOrderByAdmin()
    │
    ├─ Call orderService.cancelOrderByAdmin(id, reason)
    │
    ▼
OrderService.cancelOrderByAdmin()
    │
    ├─ Load order
    │
    ├─ Validate: chỉ hủy khi status = PENDING
    │  (đã CONFIRMED trở đi không cho hủy)
    │
    ├─ If already CANCELED → return (idempotent)
    │
    ├─ Update order:
    │  - status = CANCELED
    │  - canceledAt = LocalDateTime.now()
    │  - cancelReason = reason
    │  - canceledBy = "ADMIN"
    │
    ├─ HOÀN KHO:
    │  For each OrderItem:
    │    laptop.quantity += item.quantity
    │    laptopRepo.save(laptop)
    │
    └─ Save order

    ▼
Redirect /admin/orders/{id}
```

**Lưu ý**:

- Admin chỉ hủy được đơn PENDING
- Đã CONFIRMED trở đi không cho hủy (đảm bảo tính nhất quán)
- Khi hủy → hoàn lại số lượng tồn kho

---

## 5. TRẢ HÀNG (RETURNS)

### 5.1. Khách yêu cầu trả hàng

**Endpoint**: `POST /api/returns`  
**Controller**: `ReturnRequestController.createReturnRequest()`

**Điều kiện**:

- Order status = DELIVERED
- Trong vòng 7 ngày kể từ ngày giao (deliveredAt)
- Chưa có return request cho đơn này

**Luồng**:

```
Order Detail Page (Customer)
    │ Nếu order.status = DELIVERED
    │ && (now - deliveredAt) <= 7 days
    │ && chưa có return request
    │ → Hiện nút "Yêu cầu trả hàng"
    ▼
POST /api/returns
    │ Body: { orderId, reason, images[] }
    ▼
ReturnRequestController.createReturnRequest()
    │
    ├─ 1. Load order by ID
    │
    ├─ 2. Validate ownership: order.customer == currentUser
    │
    ├─ 3. Validate status: order.status == DELIVERED
    │
    ├─ 4. Validate timeframe:
    │    days = ChronoUnit.DAYS.between(deliveredAt, now)
    │    if (days > 7) → throw exception
    │
    ├─ 5. Check existing return:
    │    if (returnRepo.existsByOrderId(orderId))
    │      → throw exception
    │
    ├─ 6. Upload images (nếu có):
    │    For each image:
    │      - Save to /uploads/returns/
    │      - Generate filename: return_{orderId}_{timestamp}_{idx}.ext
    │
    ├─ 7. Create ReturnRequest:
    │    - order = order
    │    - reason = reason
    │    - status = PENDING
    │    - imageUrls = list of uploaded image paths
    │    - requestedAt = LocalDateTime.now()
    │
    └─ 8. Save return request

    ▼
Response: { id, status, message }
```

---

### 5.2. Admin xử lý trả hàng

**Endpoints**:

- `GET /admin/returns` - Danh sách yêu cầu trả hàng
- `GET /admin/returns/{id}` - Chi tiết yêu cầu
- `POST /admin/returns/{id}/approve` - Duyệt trả hàng
- `POST /admin/returns/{id}/reject` - Từ chối

**Luồng duyệt**:

```
Admin Returns List
    │ Click "Chi tiết"
    ▼
GET /admin/returns/{id}
    │
    └─ Hiển thị:
       - Thông tin đơn hàng
       - Lý do trả
       - Ảnh minh chứng
       - Trạng thái hiện tại
       - Form duyệt/từ chối

Admin quyết định
    │
    ├─ APPROVE ──────────────┐
    │                        │
    ▼                        │
POST /admin/returns/{id}/approve
    │                        │
    ├─ Update return.status = APPROVED
    ├─ Update return.processedAt = now
    ├─ Update return.processedBy = admin.username
    ├─ refundAmount = order.total
    ├─ Save return
    │
    └─ TODO: Tạo refund transaction

    │
    └─ REJECT ──────────────┐
                            │
                            ▼
POST /admin/returns/{id}/reject
    │ rejectionReason = reason
    │
    ├─ Update return.status = REJECTED
    ├─ Update return.processedAt = now
    ├─ Update return.processedBy = admin.username
    ├─ Update return.rejectionReason = reason
    └─ Save return
```

**Return Status**:

- `PENDING` - Chờ xử lý
- `APPROVED` - Đã duyệt
- `REJECTED` - Từ chối
- `REFUNDED` - Đã hoàn tiền (future)

---

## 6. CHAT REALTIME (WEBSOCKET)

### 6.1. Kiến trúc

**Technology**: STOMP over WebSocket  
**Broker**: Simple in-memory broker  
**Destinations**:

- `/app/*` - Client gửi message đến server
- `/topic/*` - Server broadcast to subscribers
- `/user/queue/*` - Server gửi private message đến user cụ thể

**Config** (`WebSocketConfig.java`):

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
```

---

### 6.2. Luồng Chat (Customer ↔ Admin)

**Customer View**: `/chat`  
**Admin View**: `/admin/chat`

**Kết nối WebSocket**:

```javascript
// Client (chat.html / admin/chat.html)
const socket = new SockJS("/ws-chat");
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
  console.log("Connected: " + frame);

  // Subscribe to private queue
  stompClient.subscribe("/user/queue/messages", function (msg) {
    displayMessage(JSON.parse(msg.body));
  });
});
```

**Gửi message**:

```
Customer hoặc Admin
    │ Gõ tin nhắn
    │ Click "Gửi"
    ▼
JavaScript gửi qua STOMP:
    stompClient.send("/app/chat.send", {}, JSON.stringify({
        from: currentUsername,
        to: recipientUsername,
        content: messageText
    }));
    │
    ▼
Server nhận tại:
    @MessageMapping("/chat.send")
    @SendToUser("/queue/messages")
    public ChatMessage handleMessage(ChatMessage msg, Principal principal)
    │
    ├─ Validate sender: principal.getName() == msg.from
    │
    ├─ Save message vào database:
    │  - ChatMessage entity
    │  - from, to, content, timestamp
    │  - chatMessageRepo.save(msg)
    │
    ├─ Gửi đến người nhận:
    │  - messagingTemplate.convertAndSendToUser(
    │      msg.to, "/queue/messages", msg)
    │
    └─ Return message to sender (via @SendToUser)

    ▼
Recipients nhận message
    │ stompClient.subscribe('/user/queue/messages')
    │ → Callback: displayMessage(msg)
    └─ Hiển thị tin nhắn trong chat UI
```

---

### 6.3. Admin: Danh sách Chat Partners

**Endpoint**: `GET /api/chat/partners`  
**Controller**: `ChatRestController.getChatPartners()`

**Luồng**:

```
Admin Chat Page
    │ Load danh sách users đã chat
    ▼
GET /api/chat/partners
    │ Require: ROLE_ADMIN
    ▼
ChatRestController.getChatPartners()
    │
    ├─ Query database:
    │  SELECT DISTINCT from_user FROM chat_messages
    │  WHERE to_user = 'admin'
    │  UNION
    │  SELECT DISTINCT to_user FROM chat_messages
    │  WHERE from_user = 'admin'
    │
    ├─ Load User info for each partner:
    │  - userService.findByUsername(username)
    │
    └─ Return List<User> (JSON)

    ▼
Admin UI hiển thị:
    - Avatar, username của từng partner
    - Click vào partner → load chat history
```

---

### 6.4. Load Chat History

**Endpoint**: `GET /api/chat/history?with={username}`  
**Controller**: `ChatRestController.getChatHistory()`

**Luồng**:

```
Chat Page (Customer or Admin)
    │ Mở chat với user X
    ▼
GET /api/chat/history?with=userX
    │
    ▼
ChatRestController.getChatHistory(with, principal)
    │
    ├─ currentUser = principal.getName()
    │
    ├─ Query messages:
    │  FROM chat_messages
    │  WHERE (from_user = currentUser AND to_user = with)
    │     OR (from_user = with AND to_user = currentUser)
    │  ORDER BY timestamp ASC
    │
    └─ Return List<ChatMessage> (JSON)

    ▼
Client render chat history
```

---

## 7. MÃ GIẢM GIÁ (DISCOUNT)

### 7.1. Tạo mã giảm giá (Admin)

**Endpoint**: `POST /admin/discounts/save`  
**Controller**: `AdminMaGiamgiaController.saveDiscount()`

**Form Fields**:

- `code` - Mã giảm giá (VD: "SUMMER2024")
- `discountPercent` - Phần trăm giảm (0-100)
- `maxUsageCount` - Số lần dùng tối đa (null = unlimited)
- `expiryDate` - Ngày hết hạn

**Validation**:

- Code unique (case-insensitive)
- discountPercent: 0-100
- expiryDate >= today

---

### 7.2. Áp dụng mã giảm giá

**Endpoint**: `POST /cart/apply-discount`  
**Controller**: `CartController.applyDiscount()`

**Luồng**:

```
Cart Page
    │ Nhập mã giảm giá
    │ Click "Áp dụng"
    ▼
POST /cart/apply-discount
    │ discountCode = "SUMMER2024"
    ▼
CartController.applyDiscount()
    │
    ├─ 1. Load discount từ DB:
    │    discountRepo.findByCodeIgnoreCase(code)
    │
    ├─ 2. Validate:
    │    - Mã có tồn tại?
    │    - Chưa hết hạn? (expiryDate >= today)
    │    - Còn lượt dùng? (currentUsageCount < maxUsageCount)
    │
    ├─ 3. Tính discountAmount:
    │    totalPrice = sum(cart items)
    │    discountAmount = totalPrice × discountPercent / 100
    │
    ├─ 4. Save vào session:
    │    session.setAttribute("discountCode", code)
    │    session.setAttribute("discountPercent", percent)
    │    session.setAttribute("discountAmount", amount)
    │
    └─ 5. Flash success message

    ▼
Redirect /cart (hiển thị giá sau giảm)
```

**Validation Rules**:

```java
if (discount == null) {
    error = "Mã không tồn tại";
}
if (discount.getExpiryDate().isBefore(LocalDate.now())) {
    error = "Mã đã hết hạn";
}
if (discount.getCurrentUsageCount() >= discount.getMaxUsageCount()) {
    error = "Mã đã hết lượt sử dụng";
}
```

---

### 7.3. Tracking Usage

**Khi tạo đơn hàng**:

```java
// CheckoutController.processCheckout()
if (discountCode != null) {
    Discount disc = discountRepo.findByCodeIgnoreCase(discountCode);
    if (disc != null) {
        // Tăng số lần sử dụng
        disc.setCurrentUsageCount(disc.getCurrentUsageCount() + 1);
        discountRepo.save(disc);
    }
}

// Lưu vào Order
order.setDiscountCode(discountCode);
order.setDiscountPercent(percent);
order.setDiscountAmount(amount);
```

---

## 8. THỐNG KÊ & DASHBOARD

### 8.1. Admin Dashboard

**Endpoint**: `GET /admin/dashboard`  
**Controller**: `AdminController.dashboard()`

**Metrics hiển thị**:

```
┌─────────────────────────────────────┐
│      ADMIN DASHBOARD                │
├─────────────────────────────────────┤
│ KPIs (cho 1 ngày cụ thể):          │
│  - Doanh thu                        │
│  - Số đơn hàng                      │
│  - Sản phẩm đã bán                  │
│  - Đơn chờ xử lý (PENDING)          │
│                                     │
│ Charts:                             │
│  - Doanh thu theo ngày (7/30 ngày)  │
│  - Doanh thu theo tháng             │
│  - Top sản phẩm bán chạy            │
│  - Phân bố trạng thái đơn hàng      │
└─────────────────────────────────────┘
```

**API Endpoint**: `GET /admin/api/dashboard/metrics?date=YYYY-MM-DD`  
**Controller**: `AdminController.getDashboardMetrics()`

**Response**:

```json
{
  "revenue": 123456789.0,
  "ordersCount": 45,
  "soldItems": 78,
  "pendingOrders": 12
}
```

---

### 8.2. Thống kê doanh thu

**Endpoint**: `POST /admin/thongke/doanhthu`  
**Controller**: `AdminthongkeController.getRevenueData()`

**Request**:

```json
{
  "type": "daily", // hoặc "monthly"
  "startDate": "2025-11-01",
  "endDate": "2025-11-30"
}
```

**Luồng**:

```
POST /admin/thongke/doanhthu
    │
    ▼
AdminthongkeController.getRevenueData()
    │
    ├─ Parse startDate, endDate
    │
    ├─ If type == "daily":
    │  - orderService.getDailyRevenue(start, end)
    │
    ├─ Else if type == "monthly":
    │  - orderService.getMonthlyRevenue(start, end)
    │
    ▼
OrderService query database:
    │
    ├─ Query chỉ đơn có status:
    │  - CONFIRMED, SHIPPED, DELIVERED
    │  (bỏ PENDING, CANCELED)
    │
    ├─ Group by date/month
    │
    └─ Sum order.total

    ▼
Return List<RevenueDataDto>
```

**Response**:

```json
[
  { "date": "2025-11-01", "revenue": 12345678.00 },
  { "date": "2025-11-02", "revenue": 9876543.00 },
  ...
]
```

**Chart Rendering**:

- Frontend sử dụng Chart.js
- X-axis: dates/months
- Y-axis: revenue (VNĐ)

---

## 9. ĐỔI MẬT KHẨU & QUÊN MẬT KHẨU

### 9.1. Đổi mật khẩu

**Endpoint**: `POST /profile/change-password`  
**Controller**: `ProfileController.changePassword()`

**Luồng**:

```
Profile Page
    │ Click "Đổi mật khẩu"
    ▼
GET /change-password
    │
    └─ Render form:
       - oldPassword (mật khẩu hiện tại)
       - newPassword (mật khẩu mới)
       - confirmNew (nhập lại mật khẩu mới)

User submit form
    ▼
POST /change-password
    │
    ▼
ProfileController.changePassword()
    │
    ├─ 1. Check OAuth2:
    │    if (isOAuth2(auth))
    │      → error: "Tài khoản Google không đổi mật khẩu"
    │
    ├─ 2. Load current user:
    │    user = loadUserFromAuth(auth)
    │
    ├─ 3. Server-side validation:
    │    if (confirmNew != newPassword)
    │      → error: "Mật khẩu mới nhập lại không khớp"
    │
    ├─ 4. Verify old password:
    │    if (!passwordEncoder.matches(oldPassword, user.password))
    │      → error: "Mật khẩu cũ không đúng"
    │
    ├─ 5. Update password:
    │    userService.updatePassword(username, newPassword)
    │
    ▼
UserServiceImpl.updatePassword()
    │
    ├─ Load user by username
    ├─ Encode new password: passwordEncoder.encode(newPassword)
    ├─ user.setPassword(encoded)
    └─ userRepo.save(user)

    ▼
Success message: "Đổi mật khẩu thành công"
```

**Validation**:

- Client-side (JS): `newPassword === confirmNew`
- Server-side:
  - confirmNew matches newPassword
  - oldPassword correct (BCrypt verify)
  - newPassword >= 6 chars (HTML5 minlength)

---

### 9.2. Quên mật khẩu

**Endpoints**:

- `GET /forgot-password` - Form nhập email/username
- `POST /forgot-password` - Gửi email reset
- `GET /reset-password?token=xxx` - Form nhập mật khẩu mới
- `POST /reset-password` - Cập nhật mật khẩu

**Luồng**:

```
Login Page
    │ Click "Quên mật khẩu?"
    ▼
GET /forgot-password
    │
    └─ Form: nhập email hoặc username

User submit
    ▼
POST /forgot-password
    │ identifier = "user@example.com"
    ▼
ForgotPasswordController.processForgot()
    │
    ├─ 1. Find user:
    │    if (identifier.contains("@"))
    │      user = userService.findByEmail(identifier)
    │    else
    │      user = userService.findByUsername(identifier)
    │
    ├─ 2. Validate:
    │    if (user == null || user.email == null)
    │      → error: "Không tìm thấy tài khoản"
    │
    ├─ 3. Delete old tokens:
    │    tokenRepo.deleteByUserId(user.id)
    │
    ├─ 4. Generate new token:
    │    token = UUID.randomUUID().toString()
    │    PasswordResetToken prt = new PasswordResetToken()
    │    prt.token = token
    │    prt.user = user
    │    prt.expiryDate = now + 1 hour
    │    tokenRepo.save(prt)
    │
    ├─ 5. Build reset link:
    │    resetLink = "http://localhost:8080/reset-password?token={token}"
    │
    ├─ 6. Send email:
    │    emailService.sendSimpleMessage(
    │      to: user.email,
    │      subject: "Đặt lại mật khẩu - Laptop Shop",
    │      text: "Nhấp vào liên kết sau để đặt lại mật khẩu:\n{resetLink}"
    │    )
    │
    └─ 7. Success message:
         "Đã gửi email hướng dẫn đến ***@gmail.com"

User clicks link in email
    ▼
GET /reset-password?token=abc123
    │
    ▼
ForgotPasswordController.showResetForm(token)
    │
    ├─ 1. Load token from DB:
    │    optionalPrt = tokenRepo.findByToken(token)
    │
    ├─ 2. Validate:
    │    if (token not found || expired)
    │      → error: "Liên kết không hợp lệ hoặc đã hết hạn"
    │
    └─ 3. Render form:
         - Hidden: token
         - Input: new password

User submit new password
    ▼
POST /reset-password
    │ token, password
    ▼
ForgotPasswordController.processReset()
    │
    ├─ 1. Validate token again
    │
    ├─ 2. Load user from token:
    │    user = prt.user
    │
    ├─ 3. Update password:
    │    userService.updatePassword(user.username, password)
    │    (encode BCrypt inside)
    │
    ├─ 4. Delete token:
    │    tokenRepo.delete(prt)
    │
    └─ 5. Redirect to login:
         message: "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
```

**Token Expiry**: 1 hour  
**Token Table**: `password_reset_tokens`

- `id`, `token`, `user_id`, `expiry_date`

---

## PHỤ LỤC

### A. Order Status Lifecycle

```
┌─────────┐
│ PENDING │ (Chờ xử lý - mới tạo)
└────┬────┘
     │
     ├─ Admin xác nhận ──────────┐
     │                           │
     ▼                           │
┌───────────┐                    │
│ CONFIRMED │ (Đã xác nhận)      │
└─────┬─────┘                    │
      │                          │
      ├─ Admin giao hàng         │
      │                          │
      ▼                          │
┌──────────┐                     │
│ SHIPPING │ (Đang giao)         │
└────┬─────┘                     │
     │                           │
     ├─ Giao thành công          │
     │                           │
     ▼                           │
┌───────────┐                    │
│ DELIVERED │ (Đã giao)          │
└───────────┘                    │
                                 │
                                 ▼
                            ┌──────────┐
                            │ CANCELED │ (Đã hủy)
                            └──────────┘

Hủy được khi: status == PENDING only
```

### B. Inventory Management

**Khi đặt hàng** (`OrderService.createOrder()`):

```java
// TRỪ TỒN KHO
for (CartItem ci : items) {
    laptop.quantity -= ci.quantity;
    laptopRepo.save(laptop);
}
```

**Khi hủy đơn** (`OrderService.cancelOrder()` / `cancelOrderByAdmin()`):

```java
// HOÀN KHO
for (OrderItem item : order.items) {
    laptop.quantity += item.quantity;
    laptopRepo.save(laptop);
}
```

### C. CSRF Protection

**Config**:

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .ignoringRequestMatchers("/ws-chat/**", "/api/chat/**")
)
```

**Template Usage**:

```html
<form th:action="@{/endpoint}" method="post">
  <input
    type="hidden"
    th:name="${_csrf.parameterName}"
    th:value="${_csrf.token}"
  />
  ...
</form>
```

**AJAX**:

```javascript
fetch("/endpoint", {
  method: "POST",
  headers: {
    "X-CSRF-TOKEN": getCsrfToken(),
  },
  body: formData,
});
```

### D. Email Templates

**Order Confirmation** (`email/order-confirmation.html`):

- Thông tin đơn hàng
- Danh sách sản phẩm
- Tổng tiền, discount
- Thông tin người nhận
- Tracking (nếu có)

**Forgot Password**:

- Plain text email
- Reset link có token
- Hướng dẫn reset
- Link hết hạn sau 1 giờ

---

## KẾT LUẬN

Tài liệu này mô tả chi tiết các luồng chức năng chính trong hệ thống Website Bán Laptop. Mỗi luồng bao gồm:

- **Endpoint** và Controller xử lý
- **Sơ đồ luồng** từ user action đến database
- **Validation** rules (client & server)
- **Security** considerations
- **Error handling** và edge cases

**Lưu ý khi triển khai**:

1. Luôn validate dữ liệu ở cả client và server
2. Sử dụng transaction (`@Transactional`) cho operations phức tạp
3. Log đầy đủ để debug và audit
4. Test thoroughly với nhiều scenarios
5. Xử lý exceptions gracefully và trả về messages thân thiện

**Công nghệ sử dụng**:

- Spring Boot 3.x
- Spring Security (BCrypt, OAuth2)
- Spring Data JPA
- Thymeleaf
- WebSocket (STOMP)
- VNPay Payment Gateway
- MySQL Database
- JavaMail (Gmail SMTP)

---

**Version**: 1.0  
**Last Updated**: 24/11/2025
