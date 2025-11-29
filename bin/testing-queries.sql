-- ==========================================
-- SQL QUERIES FOR RETURN FEATURE TESTING
-- ==========================================

-- 1. Kiểm tra tất cả return requests
SELECT 
    rr.id,
    rr.order_id,
    o.status as order_status,
    rr.customer_id,
    u.username as customer_name,
    rr.status,
    rr.refund_amount,
    rr.created_at,
    rr.processed_at,
    rr.received_at,
    rr.refunded_at,
    rr.reason
FROM return_requests rr
LEFT JOIN orders o ON rr.order_id = o.id
LEFT JOIN users u ON rr.customer_id = u.id
ORDER BY rr.created_at DESC;

-- 2. Thống kê theo status
SELECT 
    status,
    COUNT(*) as count,
    SUM(refund_amount) as total_refund
FROM return_requests
GROUP BY status
ORDER BY 
    CASE status
        WHEN 'REQUESTED' THEN 1
        WHEN 'APPROVED' THEN 2
        WHEN 'REJECTED' THEN 3
        WHEN 'ITEM_RECEIVED' THEN 4
        WHEN 'REFUNDED' THEN 5
        WHEN 'CANCELLED' THEN 6
    END;

-- 3. Kiểm tra orders đã CANCELED sau khi approve return
SELECT 
    o.id as order_id,
    o.status as order_status,
    rr.id as return_id,
    rr.status as return_status,
    rr.processed_at,
    o.total as order_total,
    rr.refund_amount
FROM orders o
INNER JOIN return_requests rr ON o.id = rr.order_id
WHERE rr.status IN ('APPROVED', 'ITEM_RECEIVED', 'REFUNDED');
-- Expected: order_status = 'CANCELED' cho tất cả

-- 4. Kiểm tra stock restoration (so sánh trước/sau)
-- Run BEFORE marking as received:
SELECT id, name, quantity FROM laptops WHERE id IN (
    SELECT DISTINCT oi.product_id 
    FROM order_items oi
    INNER JOIN return_requests rr ON rr.order_id = oi.order_id
    WHERE rr.status = 'APPROVED'
);
-- Note: Ghi lại quantity hiện tại

-- Run AFTER marking as received:
-- Verify: quantity = old_quantity + return_quantity

-- 5. Kiểm tra timestamps logic (phải theo thứ tự)
SELECT 
    id,
    status,
    created_at,
    processed_at,
    received_at,
    refunded_at,
    CASE 
        WHEN processed_at < created_at THEN 'ERROR: processed before created'
        WHEN received_at < processed_at THEN 'ERROR: received before processed'
        WHEN refunded_at < received_at THEN 'ERROR: refunded before received'
        ELSE 'OK'
    END as timeline_check
FROM return_requests
WHERE processed_at IS NOT NULL OR received_at IS NOT NULL OR refunded_at IS NOT NULL;
-- Expected: All 'OK'

-- 6. Kiểm tra foreign key integrity
SELECT 
    rr.id,
    rr.order_id,
    o.id as actual_order,
    rr.customer_id,
    u.id as actual_customer
FROM return_requests rr
LEFT JOIN orders o ON rr.order_id = o.id
LEFT JOIN users u ON rr.customer_id = u.id
WHERE o.id IS NULL OR u.id IS NULL;
-- Expected: 0 rows

-- 7. Tìm orders DELIVERED có thể tạo return request
SELECT 
    o.id,
    o.customer_id,
    u.username,
    o.status,
    o.delivered_at,
    DATEDIFF(NOW(), o.delivered_at) as days_since_delivery,
    o.total,
    COUNT(oi.id) as item_count
FROM orders o
INNER JOIN users u ON o.customer_id = u.id
LEFT JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN return_requests rr ON o.id = rr.order_id AND rr.status IN ('REQUESTED', 'APPROVED', 'ITEM_RECEIVED')
WHERE o.status = 'DELIVERED'
  AND o.delivered_at IS NOT NULL
  AND DATEDIFF(NOW(), o.delivered_at) <= 14
  AND rr.id IS NULL  -- Chưa có return request
GROUP BY o.id
ORDER BY o.delivered_at DESC;

-- 8. Chi tiết return items (parse JSON)
SELECT 
    rr.id,
    rr.return_items_json,
    oi.id as order_item_id,
    l.name as product_name,
    oi.quantity as ordered_qty,
    oi.unit_price
FROM return_requests rr
INNER JOIN orders o ON rr.order_id = o.id
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN laptops l ON oi.product_id = l.id
WHERE rr.id = ?;  -- Replace with actual return request ID

-- 9. Kiểm tra photos upload
SELECT 
    id,
    photos,
    CHAR_LENGTH(photos) as photos_length,
    (CHAR_LENGTH(photos) - CHAR_LENGTH(REPLACE(photos, ',', '')) + 1) as photo_count
FROM return_requests
WHERE photos IS NOT NULL AND photos != '';

-- 10. Tìm return requests cần xử lý (admin view)
SELECT 
    rr.id,
    rr.status,
    o.id as order_id,
    u.username as customer,
    rr.refund_amount,
    rr.created_at,
    TIMESTAMPDIFF(HOUR, rr.created_at, NOW()) as hours_pending
FROM return_requests rr
INNER JOIN orders o ON rr.order_id = o.id
INNER JOIN users u ON rr.customer_id = u.id
WHERE rr.status = 'REQUESTED'
ORDER BY rr.created_at ASC;

-- 11. Audit log - Lịch sử thay đổi status
SELECT 
    id,
    status,
    created_at as time_requested,
    processed_at as time_processed,
    received_at as time_received,
    refunded_at as time_refunded,
    TIMESTAMPDIFF(HOUR, created_at, processed_at) as hours_to_process,
    TIMESTAMPDIFF(HOUR, processed_at, received_at) as hours_to_receive,
    TIMESTAMPDIFF(HOUR, received_at, refunded_at) as hours_to_refund
FROM return_requests
WHERE status IN ('APPROVED', 'REJECTED', 'ITEM_RECEIVED', 'REFUNDED')
ORDER BY created_at DESC;

-- 12. Top customers by return count
SELECT 
    u.id,
    u.username,
    u.email,
    COUNT(rr.id) as return_count,
    SUM(CASE WHEN rr.status = 'REFUNDED' THEN rr.refund_amount ELSE 0 END) as total_refunded
FROM users u
INNER JOIN return_requests rr ON u.id = rr.customer_id
GROUP BY u.id
HAVING return_count > 0
ORDER BY return_count DESC, total_refunded DESC;

-- 13. Cleanup test data (USE WITH CAUTION!)
-- DELETE FROM return_requests WHERE id = ?;

-- 14. Reset order status for testing
-- UPDATE orders SET status = 'DELIVERED' WHERE id = ?;

-- 15. Check specific return request details
SELECT 
    rr.*,
    o.status as order_status,
    u.username,
    u.email
FROM return_requests rr
INNER JOIN orders o ON rr.order_id = o.id
INNER JOIN users u ON rr.customer_id = u.id
WHERE rr.id = ?;  -- Replace with actual ID
