# Used Marketplace API

Backend cho sàn thương mại điện tử chuyên bán đồ điện tử & gia dụng **đã qua sử dụng**.

**Tiến độ hiện tại: đã hoàn thành 15/15 phase.** Toàn bộ luồng nghiệp vụ chính (Auth → Trade-in, Notification, Chat REST, Admin dashboard) có REST API + service + test MockMvc.

## Tech stack

- Kotlin + Spring Boot 3.3.4 (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL 16 + Flyway (`V1`–`V6`)
- JWT (access + refresh token, hash SHA-256, rotation)
- Cloudinary (ảnh sản phẩm / review / trade-in) · Stripe Test Mode + COD
- JUnit 5 · MockMvc · H2 (profile `test`)
- Mapper viết tay cho hầu hết domain; MapStruct còn dùng ở Category/Brand

## Cấu trúc thư mục

```
src/main/kotlin/com/usedmarket/
  ├── config/          # Health check, JPA auditing
  ├── security/        # JWT filter, SecurityConfig, UserDetails
  ├── common/          # BaseEntity, GlobalExceptionHandler
  ├── media/           # Cloudinary
  ├── auth/            # Register / login / refresh / logout / reset password
  ├── user/            # User, Address, GET /api/users/me
  ├── catalog/         # Category, Brand
  ├── product/         # Product + ConditionScore + Inspection + Warranty
  ├── inventory/       # Tồn kho + lịch sử điều chỉnh
  ├── cart/            # Giỏ hàng + preview coupon
  ├── coupon/          # Entity + repository (áp dụng qua Cart/Order)
  ├── order/           # Checkout, đơn hàng, timeline, huỷ / đổi trạng thái
  ├── payment/         # Stripe PaymentIntent + webhook + COD
  ├── shipment/        # Entity + repository (tạo lúc checkout, chưa có REST)
  ├── review/          # Đánh giá sản phẩm
  ├── wishlist/        # Wishlist + cờ giảm giá
  ├── tradein/         # Thu mua đồ cũ
  ├── notification/    # REST inbox + sự kiện tự động từ Order/Payment/Product
  ├── chat/            # Phòng chat customer–shop (REST, không WebSocket)
  └── admin/           # Dashboard doanh thu (ADMIN only)
```

Mỗi domain đã có API thường gồm: controller / service / repository / entity / dto / mapper.

## Module đã có code chạy được

Đối chiếu với roadmap 15 phase. **Chạy được** = REST API + service + test MockMvc (trừ ghi chú).

| Module | Phase | Trạng thái | API chính |
|---|---|---|---|
| Project setup | 1 | Hoàn thành | `GET /api/health` |
| Database + entities | 2 | Hoàn thành | Flyway `V1`–`V6`; Hibernate `ddl-auto: validate` (profile `dev`) |
| **Auth** | 3 | Hoàn thành | `POST /api/auth/register`, `/login`, `/refresh`, `/logout`, `/forgot-password`, `/reset-password` |
| User | 3 | Một phần | `GET /api/users/me`. Entity `Address` dùng khi checkout; **chưa** có CRUD địa chỉ |
| **Product + Category + Brand** | 4 | Hoàn thành | `/api/products`, `/api/categories`, `/api/brands` (public GET; STAFF/ADMIN ghi; ADMIN quản lý catalog) |
| **Condition + Inspection + Warranty** | 5 | Hoàn thành | `/api/products/{id}/condition-score`, `/inspection`, `/warranty` |
| **Inventory** | 6 | Hoàn thành | `/api/products/{id}/inventory` (STAFF/ADMIN) |
| **Cart** | 7 | Hoàn thành | `/api/cart` (CRUD item, clear, preview coupon) |
| Coupon | 7 / 8 | Một phần | Entity + repo; preview trên cart và commit khi checkout. **Chưa** có API quản lý coupon |
| **Order + Checkout** | 8 | Hoàn thành | `POST /api/orders`, list/detail/timeline/cancel; STAFF/ADMIN `GET /manage`, `PATCH /{id}/status`. Pessimistic lock chống mua trùng `stock=1` |
| Shipment | 8 | Schema + ghi lúc checkout | Entity + repository; bản ghi `Shipment` được tạo khi checkout — **chưa** có REST vận đơn |
| **Payment** | 9 | Hoàn thành | `POST /api/orders/{id}/payment/intent`, `GET .../payment`, `POST /api/webhooks/stripe`; COD đánh dấu paid khi `DELIVERED` |
| **Review + Wishlist** | 10 | Hoàn thành | `/api/products/{id}/reviews`, `/api/wishlist` |
| **Trade-in** | 11 | Hoàn thành | `/api/trade-ins` (tạo, media, offer/accept/decline/complete) |
| **Notification** | 12 | Hoàn thành | `GET /api/notifications`, `/unread-count`, `PATCH /{id}/read`, `/read-all`. Tự bắn khi đơn hàng / thanh toán / giá wishlist giảm. **Chưa** có job `WARRANTY_EXPIRING` |
| **Chat** | 13 | Hoàn thành (REST) | `/api/chat/rooms` (tạo, list, messages, claim, close). Không WebSocket — client poll REST |
| **Admin dashboard** | 14 | Hoàn thành | `GET /api/admin/dashboard/summary`, `/revenue-chart`, `/orders-by-status`, `/best-selling`, `/revenue-by-category` (ADMIN). Chart `daily`/`monthly`; period khác → 400 |
| Testing tổng thể | 15 | Hoàn thành (phần consolidation) | `FinalConsolidationTest` + test theo module kể cả Dashboard. **~76** test MockMvc + context load. Chưa có E2E ngoài process (curl/Postman/browser) |

Coupon và Shipment có schema từ Flyway (`V4`/`V5`); coupon dùng qua Cart/Order, shipment ghi nội bộ lúc checkout — không còn “chỉ schema” như README cũ.

## Endpoint (module đã chạy)

### Auth — `/api/auth`
```
POST /register, /login, /refresh, /logout, /forgot-password, /reset-password
```

### User — `/api/users`
```
GET /me
```

### Catalog — `/api/categories`, `/api/brands`
```
GET  /               (public)
GET  /{id}, /slug/{slug}, /tree (categories)
POST, PUT /{id}, DELETE /{id}   (ADMIN)
```

### Product — `/api/products`
```
GET  /?keyword=&categoryId=&brandId=&minPrice=&maxPrice=&condition=&sortBy=&sortDir=&page=&size=  (public)
GET  /manage        (STAFF/ADMIN)
GET  /{id}, /slug/{slug}
POST, PUT /{id}                          (STAFF/ADMIN)
PATCH /{id}/visibility, DELETE /{id}     (STAFF/ADMIN)
POST /{id}/images, DELETE .../images/{imageId}, PATCH .../images/{imageId}/primary  (STAFF/ADMIN)

GET/PUT /{id}/condition-score
GET  /{id}/inspection, GET /{id}/inspection/all, POST /{id}/inspection, PATCH .../publish
GET/PUT /{id}/warranty
GET  /{id}/inventory, /history, POST /{id}/inventory/adjust   (STAFF/ADMIN)
GET  /{id}/reviews, /summary; POST /{id}/reviews; POST .../images; PATCH .../reply
```

### Cart — `/api/cart`
```
GET, DELETE
POST /items, PUT/DELETE /items/{productId}
POST /coupon          (preview, không commit usage)
```

### Order — `/api/orders`
```
POST                  (checkout)
GET                   (đơn của tôi)
GET /manage?status=   (STAFF/ADMIN)
GET /{id}, /{id}/timeline
POST /{id}/cancel
PATCH /{id}/status    (STAFF/ADMIN)
```

### Payment
```
POST /api/orders/{orderId}/payment/intent
GET  /api/orders/{orderId}/payment
POST /api/webhooks/stripe
```

### Wishlist — `/api/wishlist`
```
GET, POST/DELETE /{productId}
```

### Trade-in — `/api/trade-ins`
```
POST, GET
GET /manage?status=           (STAFF/ADMIN)
GET /{id}
POST /{id}/items
PATCH /{id}/status
POST /{id}/offer, /accept, /decline
PATCH /{id}/complete
```

### Notification — `/api/notifications`
```
GET, GET /unread-count
PATCH /{id}/read, /read-all
```

### Chat — `/api/chat/rooms`
```
POST, GET
GET /manage                   (STAFF/ADMIN)
GET/POST /{id}/messages
POST /{id}/claim              (STAFF/ADMIN)
PATCH /{id}/close
```

### Admin dashboard — `/api/admin/dashboard` (ADMIN only)
```
GET /summary
GET /revenue-chart?period=daily|monthly&limit=
GET /orders-by-status
GET /best-selling?limit=
GET /revenue-by-category
```

## Chạy project (local dev)

### 1. Yêu cầu

- JDK 21
- Docker (chạy PostgreSQL)

### 2. Khởi động PostgreSQL

```bash
docker compose up -d
```

### 3. Tạo file môi trường

```bash
cp .env.example .env
# chỉnh JWT_SECRET, Cloudinary, Stripe keys
```

### 4. Chạy ứng dụng

Profile mặc định là `dev` (PostgreSQL). Cổng **8081** (override trong `application-dev.yml`; `application.yml` để 8080).

```bash
./gradlew bootRun
```

PowerShell (nếu cần set mật khẩu DB):

```powershell
$env:DB_PASSWORD="123456"; ./gradlew bootRun
```

Ứng dụng (profile `dev`): `http://localhost:8081`

Kiểm tra health check:

```bash
curl http://localhost:8081/api/health
```

### 5. Chạy test

```bash
./gradlew test
```

Test dùng profile `test` với H2 in-memory (không cần PostgreSQL). Flyway tắt trên profile này; schema do Hibernate `create-drop`.

## Roadmap các phase

| Phase | Nội dung | Trạng thái |
|---|---|---|
| 1  | Project setup | Done |
| 2  | Database + entities | Done |
| 3  | Authentication (JWT, register/login/refresh) | Done |
| 4  | Product + Category + Brand | Done |
| 5  | Condition + Inspection + Warranty | Done |
| 6  | Inventory | Done |
| 7  | Cart | Done |
| 8  | Order + Checkout | Done |
| 9  | Payment (Stripe + COD) | Done |
| 10 | Review + Wishlist | Done |
| 11 | Trade-in (thu mua đồ cũ) | Done |
| 12 | Notification | Done (REST + 7/9 loại sự kiện; thiếu job `WARRANTY_EXPIRING`) |
| 13 | Chat | Done (REST polling, chưa WebSocket) |
| 14 | Admin dashboard APIs | Done (ADMIN only; chart `daily`/`monthly`) |
| 15 | Testing tổng thể | Done (`FinalConsolidationTest` + test theo module; chưa E2E ngoài JVM) |

**Bạn đang ở đây:** 15 phase backend đã đóng. Phần còn lại là giới hạn đã biết (WebSocket chat, job `WARRANTY_EXPIRING`, CRUD coupon/address/shipment, E2E ngoài JVM).

## Ghi chú kỹ thuật

- 3 profile: `dev` (PostgreSQL + Flyway), `test` (H2 in-memory), cộng cấu hình chung trong `application.yml`.
- Schema do Flyway quản lý (`V1__init_core_schema` → `V6__auth_tokens`). `ddl-auto: validate` trên `dev` để Hibernate không tự sinh bảng.
- JWT filter chain thay default Spring Security. Endpoint public: `/api/auth/**`, `/api/health`, `/api/webhooks/**`, GET catalog/product.
- Ảnh upload qua Cloudinary (product, review, trade-in). Thanh toán Stripe Test Mode qua PaymentIntent + webhook; COD paid khi giao hàng.
- Quyền: `CUSTOMER` / `STAFF` / `ADMIN`. STAFF vận hành (sản phẩm, kho, đơn, inspection, trade-in, chat claim); ADMIN thêm Category/Brand và báo cáo dashboard.
- Checkout dùng `SELECT … FOR UPDATE` trên tồn kho. Order/Trade-in có state machine (chuyển trạng thái sai → 409).
- Giới hạn đã biết: chat chưa real-time; `WARRANTY_EXPIRING` chưa scheduled; trade-in chưa tự tạo Product/inventory; Stripe chưa verify với key test thật trên môi trường deploy.
