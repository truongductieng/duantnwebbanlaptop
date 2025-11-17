# 📋 Return Feature Testing Checklist

## 🎯 Mục tiêu Testing

Kiểm tra toàn bộ flow trả hàng từ Customer → Admin → Database

---

## ✅ Test Case 1: Customer tạo yêu cầu trả hàng

### Pre-conditions:

- [ ] Có đơn hàng với status = `DELIVERED`
- [ ] Đơn hàng được giao trong vòng 14 ngày
- [ ] User đã login

### Steps:

1. [ ] Truy cập `/my-orders/{orderId}`
2. [ ] Kiểm tra hiển thị button "Yêu cầu trả hàng" (chỉ khi DELIVERED)
3. [ ] Click button → Modal hiển thị
4. [ ] Chọn sản phẩm muốn trả (checkbox)
5. [ ] Nhập số lượng trả (≤ số lượng đã mua)
6. [ ] Chọn lý do từ dropdown hoặc nhập "Khác"
7. [ ] Upload ảnh (tùy chọn, max 5 ảnh)
8. [ ] Kiểm tra "Số tiền hoàn trả dự kiến" tự động tính
9. [ ] Click "Gửi yêu cầu"

### Expected Results:

- [ ] Alert "Yêu cầu trả hàng đã được gửi thành công! Mã yêu cầu: #X"
- [ ] Trang reload
- [ ] Database: Bảng `return_requests` có record mới với:
  - `status` = 'REQUESTED'
  - `order_id` = ID đơn hàng
  - `customer_id` = ID user
  - `return_items_json` = JSON array items
  - `refund_amount` = Tổng tiền tính đúng
  - `photos` = Đường dẫn ảnh (nếu có)
  - `created_at` = Thời gian hiện tại
- [ ] Folder `uploads/returns/` chứa file ảnh (nếu upload)

### Test Data:

```
Order ID: _______
Customer: _______
Items: Laptop Dell XPS 15 (2 cái, giá 25,000,000đ/cái)
Return: 1 cái
Expected refund: 25,000,000đ
```

### Console Check:

```
✓ AJAX POST to /api/returns/request
✓ Response: {success: true, returnRequestId: X, status: "REQUESTED"}
```

---

## ✅ Test Case 2: Validation - Không cho phép duplicate

### Steps:

1. [ ] Tạo yêu cầu trả hàng cho đơn X (status REQUESTED)
2. [ ] Thử tạo yêu cầu thứ 2 cho cùng đơn X

### Expected Results:

- [ ] Alert lỗi: "Đơn hàng này đã có yêu cầu trả hàng đang xử lý"
- [ ] Không tạo record mới trong database

---

## ✅ Test Case 3: Validation - Quá hạn 14 ngày

### Steps:

1. [ ] Tìm đơn hàng DELIVERED > 14 ngày trước
2. [ ] Thử tạo yêu cầu trả hàng

### Expected Results:

- [ ] Alert lỗi: "Đã quá thời hạn trả hàng (14 ngày kể từ ngày giao)"

---

## ✅ Test Case 4: Admin xem danh sách

### Steps:

1. [ ] Login với role ADMIN
2. [ ] Truy cập `/admin/returns`
3. [ ] Kiểm tra hiển thị table
4. [ ] Test filter theo status (ALL, REQUESTED, APPROVED, REJECTED, ITEM_RECEIVED, REFUNDED)

### Expected Results:

- [ ] Hiển thị tất cả return requests
- [ ] Mỗi row có: ID, Order#, Customer, Ngày tạo, Số tiền hoàn, Status badge, Dropdown "Hành động"
- [ ] Filter hoạt động đúng
- [ ] Dropdown actions hiển thị đúng theo status:
  - REQUESTED → Phê duyệt / Từ chối
  - APPROVED → Đánh dấu đã nhận
  - ITEM_RECEIVED → Xác nhận hoàn tiền

---

## ✅ Test Case 5: Admin phê duyệt yêu cầu

### Pre-conditions:

- [ ] Có return request với status = REQUESTED
- [ ] Order status = DELIVERED

### Steps:

1. [ ] Trong `/admin/returns`, tìm request có status REQUESTED
2. [ ] Click dropdown "Hành động" → "Phê duyệt"
3. [ ] Confirm dialog

### Expected Results:

- [ ] Alert: "Yêu cầu đã được phê duyệt"
- [ ] Trang reload
- [ ] Database: `return_requests`
  - `status` = 'APPROVED'
  - `processed_at` = Thời gian hiện tại
- [ ] **CRITICAL**: Database `orders` table
  - `status` = 'CANCELED' (cho order liên quan)
- [ ] Console log:

```
DEBUG: Order ID before cancel: X, Status: DELIVERED
DEBUG: Order ID after cancel: X, Status: CANCELED
DEBUG: Return request saved, ID: Y, Status: APPROVED
```

### Verify:

```sql
SELECT status FROM orders WHERE id = [ORDER_ID];
-- Expected: CANCELED

SELECT status, processed_at FROM return_requests WHERE id = [RETURN_ID];
-- Expected: APPROVED, <timestamp>
```

---

## ✅ Test Case 6: Admin từ chối yêu cầu

### Steps:

1. [ ] Click dropdown → "Từ chối"
2. [ ] Modal hiển thị, nhập lý do: "Sản phẩm không còn nguyên vẹn"
3. [ ] Click "Xác nhận từ chối"

### Expected Results:

- [ ] Alert: "Yêu cầu đã bị từ chối"
- [ ] Database: `return_requests`
  - `status` = 'REJECTED'
  - `admin_note` = Lý do đã nhập
  - `processed_at` = Thời gian hiện tại
- [ ] Order status không đổi (vẫn DELIVERED)

---

## ✅ Test Case 7: Admin đánh dấu đã nhận hàng + Restore stock

### Pre-conditions:

- [ ] Return request status = APPROVED
- [ ] Biết số lượng stock hiện tại của sản phẩm

### Steps:

1. [ ] Note stock hiện tại: `SELECT quantity FROM laptops WHERE id = [LAPTOP_ID]`
   - Laptop ID: **\_\_\_**, Stock trước: **\_\_\_** cái
2. [ ] Click dropdown → "Đánh dấu đã nhận"
3. [ ] Confirm

### Expected Results:

- [ ] Alert: "Đã đánh dấu nhận hàng và cập nhật kho"
- [ ] Database: `return_requests`
  - `status` = 'ITEM_RECEIVED'
  - `received_at` = Thời gian hiện tại
- [ ] **CRITICAL**: Database `laptops`
  - `quantity` tăng lên = Stock cũ + Số lượng trả
  - VD: Cũ = 10, Trả 2 → Mới = 12

### Verify:

```sql
SELECT quantity FROM laptops WHERE id = [LAPTOP_ID];
-- Expected: [OLD_QUANTITY] + [RETURN_QUANTITY]
```

---

## ✅ Test Case 8: Admin xác nhận hoàn tiền

### Pre-conditions:

- [ ] Return request status = ITEM_RECEIVED

### Steps:

1. [ ] Click dropdown → "Xác nhận hoàn tiền"
2. [ ] Confirm

### Expected Results:

- [ ] Alert: "Đã hoàn tiền thành công"
- [ ] Database: `return_requests`
  - `status` = 'REFUNDED'
  - `refunded_at` = Thời gian hiện tại

---

## ✅ Test Case 9: Admin xem chi tiết

### Steps:

1. [ ] Click dropdown → "Xem chi tiết đầy đủ"
2. [ ] Hoặc click vào `/admin/returns/{id}`

### Expected Results:

- [ ] Trang chi tiết hiển thị:
  - [ ] Thông tin return request (ID, status, dates)
  - [ ] Order info với link
  - [ ] Customer info
  - [ ] Lý do trả hàng
  - [ ] Bảng sản phẩm trả (tên, giá, số lượng, thành tiền, tổng)
  - [ ] Ảnh chứng minh (nếu có) - click để phóng to
  - [ ] Admin note (nếu có)
  - [ ] Timeline: Created → Processed → Received → Refunded
  - [ ] Action buttons đúng theo status

---

## ✅ Test Case 10: Customer hủy yêu cầu (nếu còn REQUESTED/APPROVED)

### Steps:

1. [ ] API call: `POST /api/returns/{id}/cancel`
2. [ ] Hoặc thêm UI button trong customer orders page

### Expected Results:

- [ ] Status chuyển sang CANCELLED
- [ ] Không thể hủy nếu đã ITEM_RECEIVED hoặc REFUNDED

---

## 🔍 Edge Cases Testing

### EC1: Partial Return (Trả 1 phần sản phẩm)

- [ ] Order có 3 items (A: 2 cái, B: 1 cái, C: 3 cái)
- [ ] Return: A (1 cái), C (2 cái)
- [ ] Refund = A.price × 1 + C.price × 2
- [ ] Stock restore: A +1, C +2, B không đổi

### EC2: Upload nhiều ảnh

- [ ] Upload 5 ảnh (max)
- [ ] Kiểm tra tất cả lưu vào `uploads/returns/`
- [ ] `photos` field có 5 paths cách nhau bởi dấu phẩy

### EC3: Không upload ảnh

- [ ] Tạo request không ảnh
- [ ] `photos` field = NULL hoặc empty

### EC4: Lý do custom

- [ ] Chọn "Khác (nhập chi tiết)"
- [ ] Nhập text dài
- [ ] Lưu đúng vào `reason` field

---

## 📊 Database Integrity Check

Sau khi chạy hết test cases, verify:

```sql
-- 1. Tổng số return requests
SELECT status, COUNT(*) FROM return_requests GROUP BY status;

-- 2. Kiểm tra foreign keys
SELECT rr.id, rr.order_id, o.id as actual_order_id, rr.customer_id, u.id as actual_customer_id
FROM return_requests rr
LEFT JOIN orders o ON rr.order_id = o.id
LEFT JOIN users u ON rr.customer_id = u.id
WHERE o.id IS NULL OR u.id IS NULL;
-- Expected: 0 rows (tất cả FK hợp lệ)

-- 3. Kiểm tra orders đã CANCELED
SELECT o.id, o.status, rr.id as return_id, rr.status as return_status
FROM orders o
INNER JOIN return_requests rr ON o.id = rr.order_id
WHERE rr.status IN ('APPROVED', 'ITEM_RECEIVED', 'REFUNDED');
-- Expected: Tất cả orders có status = CANCELED

-- 4. Kiểm tra timestamps logic
SELECT id, created_at, processed_at, received_at, refunded_at
FROM return_requests
WHERE processed_at < created_at
   OR received_at < processed_at
   OR refunded_at < received_at;
-- Expected: 0 rows (timestamps theo thứ tự đúng)
```

---

## 🐛 Known Issues & Fixes

### Issue 1: ~~getStock() undefined~~

- ✅ **FIXED**: Đã sửa thành `getQuantity()` trong ReturnService.java

### Issue 2: ~~Order không chuyển CANCELED~~

- ✅ **FIXED**: Đã thêm logic trong `approveReturn()`

### Issue 3: Debug logs trong production

- ⚠️ **TODO**: Remove `System.out.println` trong ReturnService line 265-268

---

## ✅ Final Acceptance Criteria

- [ ] **Customer flow**: Tạo request thành công cho đơn DELIVERED trong 14 ngày
- [ ] **Admin approve**: Order chuyển CANCELED
- [ ] **Admin receive**: Stock restore chính xác
- [ ] **Admin refund**: Status REFUNDED
- [ ] **Validation**: Không duplicate, không quá hạn
- [ ] **UI/UX**: Button/dropdown hiển thị đúng theo status
- [ ] **Database**: Không có orphan records, timestamps hợp lệ

---

## 📝 Testing Notes

**Tested by**: ********\_********  
**Date**: ********\_********  
**Environment**: Local / Dev / Staging  
**Browser**: Chrome / Firefox / Edge

**Issues found**:

1. ***
2. ***
3. ***

**Overall status**: ✅ Pass / ❌ Fail / ⚠️ Partial
