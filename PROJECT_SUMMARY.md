# Used Marketplace — Tổng kết dự án

Sàn thương mại điện tử chuyên bán đồ điện tử & gia dụng đã qua sử dụng.
Backend REST API hoàn chỉnh, xây dựng qua 15 phase.

## Tech stack

- **Backend**: Kotlin + Spring Boot 3.3.4 (Web, Data JPA, Security, Validation)
- **Database**: PostgreSQL 16 + Flyway (6 migration, 31+ bảng)
- **Auth**: JWT (access token) + refresh token xoay vòng (lưu hash SHA-256)
- **Thanh toán**: Stripe (test mode) + COD
- **Ảnh**: Cloudinary
- **Test**: JUnit 5 + MockMvc + H2 (in-memory)

## Checklist 15 phase

| # | Phase | Nội dung chính |
|---|---|---|
| 1 | Project setup | Gradle Kotlin DSL, cấu trúc thư mục, Docker Postgres |
| 2 | Database + Entities | ~30 entity, 5 migration (V1-V5), 31 bảng |
| 3 | Authentication | JWT, refresh token rotation, forgot/reset password, RBAC |
| 4 | Product + Category + Brand | CRUD, search/filter/sort, upload ảnh Cloudinary |
| 5 | Condition + Inspection + Warranty | Điểm tình trạng, báo cáo kiểm định (public/internal), bảo hành |
| 6 | Inventory | Ledger tồn kho tách biệt Product, reserve/release/confirmSale |
| 7 | Cart | Giỏ hàng, tính phí ship, preview coupon (không commit) |
| 8 | Order + Checkout | **Chống mua trùng (pessimistic lock)**, state machine, coupon thật |
| 9 | Payment | Stripe PaymentIntent + webhook, COD auto-paid khi giao hàng |
| 10 | Review + Wishlist | Review theo verified-purchase, theo dõi giá giảm |
| 11 | Trade-in | Workflow 6 bước thu mua đồ cũ |
| 12 | Notification | 5/9 sự kiện tự động (đơn hàng, thanh toán, giá giảm) |
| 13 | Chat | Phòng chat customer-shop, auto-claim, read receipt (REST, không WebSocket) |
| 14 | Admin Dashboard | Doanh thu, đơn hàng, sản phẩm bán chạy (chỉ ADMIN) |
| 15 | Testing tổng thể | Bổ sung test còn thiếu, tổng kết |

**Tổng số test: ~77** (Phase 3-15), tất cả `BUILD SUCCESSFUL`.

## Kiến trúc

```
controller  -> nhận HTTP request, validate input (@Valid)
service     -> business logic, @Transactional
repository  -> Spring Data JPA
entity      -> JPA entity
dto         -> request/response, tách biệt hoàn toàn khỏi entity
mapper      -> entity <-> DTO (viết tay, không dùng MapStruct — xem "Bài học" bên dưới)
```

Mỗi domain (`product`, `order`, `cart`, `inventory`...) là 1 package độc lập theo mô hình package-by-feature.

## Toàn bộ endpoint

### Auth (`/api/auth`)
```
POST /register, /login, /refresh, /logout, /forgot-password, /reset-password
```

### User (`/api/users`)
```
GET /me
```

### Catalog (`/api/categories`, `/api/brands`)
```
GET  /               (public)
GET  /{id}, /slug/{slug}, /tree (categories only)
POST, PUT /{id}, DELETE /{id}   (ADMIN only)
```

### Product (`/api/products`)
```
GET  /?keyword=&categoryId=&brandId=&minPrice=&maxPrice=&condition=&sortBy=&sortDir=&page=&size=  (public)
GET  /manage        (STAFF/ADMIN — thấy cả hidden/inactive)
GET  /{id}, /slug/{slug}
POST, PUT /{id}                          (STAFF/ADMIN)
PATCH /{id}/visibility, DELETE /{id}     (STAFF/ADMIN)
POST /{id}/images, DELETE .../images/{imageId}, PATCH .../images/{imageId}/primary  (STAFF/ADMIN)

GET/PUT /{id}/condition-score             (xem: public; sửa: STAFF/ADMIN)
GET  /{id}/inspection                     (public, báo cáo mới nhất)
GET  /{id}/inspection/all                 (STAFF/ADMIN)
POST /{id}/inspection, PATCH .../publish  (STAFF/ADMIN)
GET/PUT /{id}/warranty                    (xem: public; sửa: STAFF/ADMIN)

GET  /{id}/inventory, /{id}/inventory/history   (STAFF/ADMIN)
POST /{id}/inventory/adjust                     (STAFF/ADMIN)

GET  /{id}/reviews, /{id}/reviews/summary  (public)
POST /{id}/reviews                          (customer, cần đơn DELIVERED)
POST /{id}/reviews/{reviewId}/images        (chủ review)
PATCH /{id}/reviews/{reviewId}/reply        (STAFF/ADMIN)
```

### Cart (`/api/cart`)
```
GET, DELETE                        (giỏ hàng của tôi)
POST /items, PUT/DELETE /items/{productId}
POST /coupon                       (preview, không commit)
```

### Order (`/api/orders`)
```
POST                          (checkout)
GET                           (đơn của tôi)
GET /manage?status=           (STAFF/ADMIN)
GET /{id}, /{id}/timeline
POST /{id}/cancel
PATCH /{id}/status            (STAFF/ADMIN)
```

### Payment (`/api/orders/{orderId}/payment`)
```
POST /intent    (tạo Stripe PaymentIntent)
GET  /          (trạng thái thanh toán)
POST /api/webhooks/stripe   (public, xác thực bằng chữ ký Stripe)
```

### Wishlist (`/api/wishlist`)
```
GET, POST/DELETE /{productId}
```

### Trade-in (`/api/trade-ins`)
```
POST, GET                     (customer)
GET /manage?status=           (STAFF/ADMIN)
GET /{id}
POST /{id}/items               (đính kèm ảnh/video)
PATCH /{id}/status              (STAFF: PENDING→INSPECTING/REJECTED)
POST /{id}/offer                (STAFF: INSPECTING→OFFERED)
POST /{id}/accept, /{id}/decline (customer)
PATCH /{id}/complete             (STAFF: CUSTOMER_ACCEPTED→PURCHASED)
```

### Notification (`/api/notifications`)
```
GET, GET /unread-count
PATCH /{id}/read, /read-all
```

### Chat (`/api/chat/rooms`)
```
POST, GET                      (customer)
GET /manage                    (STAFF/ADMIN)
GET/POST /{id}/messages
POST /{id}/claim (STAFF/ADMIN), PATCH /{id}/close
```

### Admin Dashboard (`/api/admin/dashboard`) — ADMIN only
```
GET /summary, /revenue-chart, /orders-by-status, /best-selling, /revenue-by-category
```

## Cơ chế kỹ thuật cốt lõi

**Chống mua trùng sản phẩm `stock=1` (mục 20 spec)** — `InventoryService.reserveStock()` dùng `SELECT ... FOR UPDATE` (pessimistic lock), gọi bên trong transaction của `OrderService.checkout()`. Đã kiểm chứng bằng test 2 thread thật đồng thời checkout cùng 1 sản phẩm — đúng 1 thành công, 1 nhận `409`.

**State machine** — `Order` và `TradeInRequest` đều có bảng chuyển trạng thái tường minh, chặn bước nhảy vô lý (`409 InvalidOrderStateException`).

**Snapshot dữ liệu lịch sử** — `OrderItem` lưu snapshot tên/giá/tình trạng tại thời điểm mua, không phụ thuộc `Product` sau này có bị sửa/xóa.

**Ẩn thông tin nhất quán** — mọi trường hợp truy cập tài nguyên của người khác (đơn hàng, trade-in, chat, notification) đều trả `404`, không phải `403`, để không lộ sự tồn tại của tài nguyên.

## Bài học / quyết định kỹ thuật đáng chú ý

1. **MapStruct + Kotlin data class** gây bug âm thầm (field `isActive` luôn `false`, `NullPointerException` với giá trị mặc định) — chuyển hẳn sang **mapper viết tay** cho mọi module sau Phase 4.
2. **Kotlin cho phép nested comment** — chuỗi `/api/admin/**` bên trong khối `/** */` vô tình mở comment lồng, gây lỗi biên dịch "Unclosed comment".
3. **`sumOf` có thể mơ hồ giữa `Int`/`Long`** khi dùng với khối `when` trả literal — dùng `.map{}.sum()` để tránh.
4. **Circular dependency** giữa `Order`↔`Payment`: giải quyết bằng cách chỉ để `PaymentService → OrderService` (1 chiều), `OrderService` tự xử lý COD qua `PaymentRepository` trực tiếp.

## Giới hạn đã biết / có thể mở rộng thêm

- **Chat chưa real-time** — REST polling, nâng cấp WebSocket (STOMP) là bước tiếp theo hợp lý
- **`WARRANTY_EXPIRING` chưa tự động** — cần thêm Spring `@Scheduled` job quét `Warranty.endDate` hằng ngày
- **Trade-in → Product** chưa tự động liên kết `InventoryHistory` — staff tạo Product thủ công sau khi nhận hàng
- **Stripe integration chưa test với API key thật** — logic viết đúng theo tài liệu SDK nhưng khuyến nghị test thật với `STRIPE_SECRET_KEY` test mode trước khi deploy

## Chạy project

```powershell
docker compose up -d          # PostgreSQL
./gradlew bootRun             # chạy app (port 8081 nếu 8080 đã bị chiếm)
./gradlew clean test          # chạy toàn bộ test
```

Cần `.env` với `JWT_SECRET`, `CLOUDINARY_*`, `STRIPE_*` (xem `.env.example`).
