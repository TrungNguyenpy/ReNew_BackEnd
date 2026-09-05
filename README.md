# Used Marketplace API

Backend cho sàn thương mại điện tử chuyên bán đồ điện tử & gia dụng **đã qua sử dụng**.

**Tiến độ hiện tại: đã hoàn thành Phase 11 (Trade-in).** Phase tiếp theo là Notification (12), rồi Chat (13), Admin dashboard APIs (14) và Testing tổng thể (15).

## Tech stack

- Kotlin + Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- PostgreSQL 16 + Flyway migrations (`V1`–`V6`)
- JWT authentication (access + refresh token)
- Cloudinary (ảnh sản phẩm / review / trade-in) · Stripe Test Mode (thanh toán)
- JUnit 5 · MockK · MockMvc
- MapStruct (DTO tách khỏi entity)

## Cấu trúc thư mục

```
src/main/kotlin/com/usedmarket/
  ├── config/          # Health check, cấu hình Spring
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
  ├── payment/         # Stripe PaymentIntent + webhook
  ├── shipment/        # Entity + repository (chưa có REST)
  ├── review/          # Đánh giá sản phẩm
  ├── wishlist/        # Wishlist
  ├── tradein/         # Thu mua đồ cũ
  ├── notification/    # Entity + repository (chưa có REST) — Phase 12
  └── chat/            # Entity + repository (chưa có REST) — Phase 13

Mỗi domain đã có API thường gồm: controller / service / repository / entity / dto / mapper.
```

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
| Coupon | 7 / 8 | Một phần | Entity + repo; preview trên cart và áp khi checkout. **Chưa** có API quản lý coupon |
| **Order + Checkout** | 8 | Hoàn thành | `POST /api/orders`, list/detail/timeline/cancel; STAFF/ADMIN `GET /manage`, `PATCH /{id}/status` |
| Shipment | 8 | Schema only | Entity + repository — **chưa** có REST |
| **Payment** | 9 | Hoàn thành | `POST /api/orders/{id}/payment/intent`, `GET .../payment`, `POST /api/webhooks/stripe` |
| **Review + Wishlist** | 10 | Hoàn thành | `/api/products/{id}/reviews`, `/api/wishlist` |
| **Trade-in** | 11 | Hoàn thành | `/api/trade-ins` (tạo, media, offer/accept/decline/complete) |
| Notification | 12 | Schema only | Bảng + entity + repository — **chưa** có service/controller |
| Chat | 13 | Schema only | `chat_rooms` / `chat_messages` + entity + repository — **chưa** có REST |
| Admin dashboard | 14 | Chưa làm | `SecurityConfig` đã chặn `/api/admin/**` và `/api/staff/**`, nhưng **chưa** có controller dashboard riêng. Thao tác admin/staff đang nằm trên Product, Order, Trade-in, Catalog |
| Testing tổng thể | 15 | Chưa làm | Đã có test theo module (Auth, Product, Inventory, Cart, Order, Payment, Review, Wishlist, Trade-in, context load). Chưa có phase test E2E/tổng thể |

Các bảng Notification, Chat, Coupon, Shipment đã được tạo sẵn trong Flyway (`V4`, `V5`) để schema khớp spec; phần REST còn lại thuộc Phase 12–14.

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

Profile mặc định là `dev` (PostgreSQL). Cổng **8081** (override trong `application-dev.yml`).

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
| 9  | Payment (Stripe) | Done |
| 10 | Review + Wishlist | Done |
| 11 | Trade-in (thu mua đồ cũ) | **Done — bạn đang ở đây** |
| 12 | Notification | Tiếp theo (schema sẵn, chưa có API) |
| 13 | Chat | Chưa (schema sẵn, chưa có API) |
| 14 | Admin dashboard APIs | Chưa |
| 15 | Testing tổng thể | Chưa |

## Ghi chú kỹ thuật

- 3 profile: `dev` (PostgreSQL + Flyway), `test` (H2 in-memory), cộng cấu hình chung trong `application.yml`.
- Schema do Flyway quản lý (`V1__init_core_schema` → `V6__auth_tokens`). `ddl-auto: validate` trên `dev` để Hibernate không tự sinh bảng.
- JWT filter chain đã thay default Spring Security. Endpoint public: `/api/auth/**`, `/api/health`, `/api/webhooks/**`, GET catalog/product.
- Ảnh upload qua Cloudinary (product, review, trade-in). Thanh toán Stripe Test Mode qua PaymentIntent + webhook.
- Quyền: `CUSTOMER` / `STAFF` / `ADMIN`. STAFF vận hành (sản phẩm, kho, đơn, inspection, trade-in); ADMIN thêm quản lý Category/Brand.
