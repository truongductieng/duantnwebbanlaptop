# Revenue Statistics - File Collection

## Backend Controllers

### 1. AdminReportApiController.java

**Location**: `src/main/java/com/ductieng/controller/AdminReportApiController.java`
**Purpose**: REST API endpoints for revenue statistics
**Key Endpoints**:

- `GET /admin/api/reports/day?date=2025-01-15` - Daily revenue details with order breakdown
- Revenue calculation with multiple helper methods for flexible data extraction

## Backend Repositories

### 2. OrderRepository.java

**Location**: `src/main/java/com/ductieng/repository/OrderRepository.java`
**Purpose**: Database queries for order and revenue statistics
**Key Queries**:

- `sumTotalByStatus(OrderStatus)` - Total revenue by order status
- `sumTotalBetween(start, end, statuses)` - Revenue in date range
- `revenueDailyBetween()` - Daily revenue breakdown
- `topProductsByRevenueBetween()` - Top products by revenue
- `topProductsByQtyBetween()` - Top products by quantity
- `findWithItemsInDay()` - All orders for a specific date with items
- `productSummaryInDay()` - Product breakdown for specific date

## Backend Models

### 3. Order.java

**Location**: `src/main/java/com/ductieng/model/Order.java`
**Fields**:

- `id` - Order ID
- `customer` - User reference (FK)
- `status` - OrderStatus enum (PENDING, CONFIRMED, SHIPPED, DELIVERED, COMPLETED, CANCELLED)
- `total` - Total amount after discount (BigDecimal, precision 19.2)
- `totalBeforeDiscount` - Amount before discount
- `discountCode` - Discount/voucher code used
- `discountPercent` - Discount percentage (e.g., 10 = 10%)
- `discountAmount` - Amount discounted (BigDecimal)
- `paymentMethod` - Payment method (enum)
- `recipientName`, `recipientPhone`, `recipientAddress` - Shipping info
- `createdAt` - Order creation timestamp
- `deliveredAt` - Delivery completion timestamp
- `canceledAt`, `cancelReason`, `canceledBy` - Cancellation info
- `items` - OrderItem collection (OneToMany)

## Backend DTOs

### 4. DayDetailResponse.java

**Location**: `src/main/java/com/ductieng/dto/report/DayDetailResponse.java`

```java
public record DayDetailResponse(
    LocalDate date,
    BigDecimal revenue,
    List<OrderDetailDTO> orders
)
```

### 5. OrderDetailDTO.java

**Location**: `src/main/java/com/ductieng/dto/report/OrderDetailDTO.java`

```java
public record OrderDetailDTO(
    Long id,
    String code,                    // Format: "DH-{id}"
    String customer,                // Customer name
    String customerPhone,           // Phone number
    String shippingAddress,         // Shipping address
    BigDecimal total,               // Total amount
    String discountCode,            // Discount code
    BigDecimal discountAmount,      // Discount amount
    BigDecimal discountPercent,     // Discount percentage
    List<ItemDetailDTO> items       // Order items
)
```

### 6. ItemDetailDTO.java

**Location**: `src/main/java/com/ductieng/dto/report/ItemDetailDTO.java`

```java
public record ItemDetailDTO(
    Long productId,
    String productName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal            // unitPrice * quantity
)
```

### 7. TopProductDto.java

**Location**: `src/main/java/com/ductieng/dto/TopProductDto.java`

```java
public class TopProductDto {
    private Long productId;
    private String productName;
    private Long quantity;          // Total quantity sold
    private BigDecimal revenue;     // Total revenue
}
```

### 8. RevenueDataDto.java

**Location**: `src/main/java/com/ductieng/dto/RevenueDataDto.java`

```java
public class RevenueDataDto {
    private String period;          // "2025-07-28" or "2025-07"
    private BigDecimal revenue;
}
```

### 9. RatingAgg.java

**Location**: `src/main/java/com/ductieng/dto/RatingAgg.java`

```java
public record RatingAgg(
    Long laptopId,
    Double avg,                     // Average rating
    Long count                      // Number of reviews
)
```

## Frontend Templates

### 10. admin/dashboard.html

**Location**: `src/main/resources/templates/admin/dashboard.html`
**Purpose**: Admin dashboard with charts and statistics
**Key Sections**:

- Revenue statistics cards
- Revenue trends chart
- Top products chart
- Order status distribution
- Customer analytics
- Charts using Chart.js

## Key Features

### Revenue Calculation

- ✅ Filters by order status (CONFIRMED, SHIPPED, DELIVERED, COMPLETED)
- ✅ Date range queries with LocalDateTime precision
- ✅ Discount tracking (code, amount, percentage)
- ✅ Order status breakdown

### Product Analytics

- ✅ Top 10 products by revenue
- ✅ Top 10 products by quantity
- ✅ Product aggregation by sales period

### Order Statistics

- ✅ Order count by status
- ✅ Daily revenue breakdown
- ✅ Order details with customer & shipping info
- ✅ Item-level breakdown per order

### Data Extraction

- Safe reflection-based getters for flexible data access
- Handles multiple field name variations
- Builds shipping address from components
- Fallback mechanisms for missing data

## Related Enums

### OrderStatus

- PENDING
- CONFIRMED
- SHIPPED
- DELIVERED
- COMPLETED
- CANCELLED

## Database Schema Notes

### Table: orders

- Primary Key: id (BIGINT, auto-increment)
- Foreign Key: customer_id → users(id)
- Revenue Fields: total, total_before_discount, discount_amount, discount_percent
- Status Field: status (VARCHAR, ENUM)
- Timestamps: created_at, delivered_at, canceled_at

### Table: order_items (via OrderItem entity)

- Links orders to products
- Fields: quantity, unitPrice
- Used in revenue calculations via JOIN

## API Response Format

Day Detail Response Example:

```json
{
  "date": "2025-01-15",
  "revenue": "50000000.00",
  "orders": [
    {
      "id": 123,
      "code": "DH-123",
      "customer": "Nguyễn Văn A",
      "customerPhone": "0901234567",
      "shippingAddress": "123 Đường ABC, Quận 1, TP.HCM",
      "total": "5000000.00",
      "discountCode": "SALE20",
      "discountAmount": "1000000.00",
      "discountPercent": "20.00",
      "items": [
        {
          "productId": 1,
          "productName": "Laptop Dell XPS",
          "quantity": 1,
          "unitPrice": "5000000.00",
          "lineTotal": "5000000.00"
        }
      ]
    }
  ]
}
```

## Setup & Usage

1. **Endpoints**: Access via `/admin/api/reports/*`
2. **Authentication**: Requires ADMIN role
3. **Date Format**: ISO 8601 (YYYY-MM-DD)
4. **Revenue Status Filter**: Configurable via REVENUE_STATUSES set
5. **Timezone**: Uses server LocalDateTime (no timezone conversion)

## Notes

- Revenue calculations sum `total` field from orders (already includes discounts)
- Order status filtering allows flexible revenue period definitions
- Dashboard supports real-time data via AJAX
- All monetary values use BigDecimal for precision
- Product aggregation uses JPQL JOIN with GROUP BY
