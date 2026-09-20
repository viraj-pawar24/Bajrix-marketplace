# BajriX — Multi-Seller Product Marketplace

A small working marketplace for construction and home-building products.
Buyers browse a shared product catalogue and compare seller offers; sellers
manage their own price/stock/MOQ listings against that catalogue.

Stack: **React (Vite)** frontend, **Java 17 / Spring Boot 3** backend,
**MySQL** database, **Flyway** migrations.

> The original brief specifies PostgreSQL. This build uses MySQL instead —
> worth flagging in a technical discussion, since it's a deliberate
> deviation from the written spec, not an oversight.

## 1. How to run it

### Option A — Docker Compose (recommended, one command)

```bash
docker compose up --build
```

- Backend: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:5173

MySQL starts first, Flyway runs the schema + seed migrations on backend
startup, so the app comes up with sample data already loaded.

### Option B — Run locally

**Database**

```bash
docker run --name bajrix-db -e MYSQL_DATABASE=bajrix -e MYSQL_USER=bajrix \
  -e MYSQL_PASSWORD=bajrix -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:8.4
```

(or point `DB_URL`/`DB_USER`/`DB_PASSWORD` in `backend/src/main/resources/application.yml`
at any MySQL 8+ instance you already have running — including one managed
through MySQL Workbench, which is a fine way to inspect the seeded data
after Flyway has run.)

**Backend**

```bash
cd backend
mvn spring-boot:run
```

Flyway applies `V1__init_schema.sql` and `V2__seed_data.sql` automatically.
API comes up on `:8080`.

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

Runs on `:5173` and talks to `:8080` by default (override with a `.env`
containing `VITE_API_BASE_URL=...`).

**Tests**

```bash
cd backend
mvn test
```

Tests run against an in-memory H2 database in MySQL compatibility mode
(`application-test.yml`), so no external DB is needed to run them.

## 2. Important architectural decisions

**Product vs. Seller Listing.** `products` is the shared catalogue entry
(name, brand, category, unit). `seller_listings` is the join between a
`seller` and a `product`, carrying everything that varies per seller:
price, stock, minimum order quantity, and whether they're still selling it
(`active`). A `(product_id, seller_id)` unique constraint means a seller
edits their existing listing instead of ever having two rows for the same
product — this is enforced at the DB level, not just in application code.

**Mocked authentication, real authorization.** There's no login. A seller
identifies itself by sending `X-Seller-Id: <id>` on every `/api/seller/**`
request (see `SellerAuthInterceptor`). What *is* real is the authorization
boundary: `SellerListingService` always re-checks `listing.seller.id ==
sellerId-from-header` before any mutation, so a seller literally cannot
touch another seller's listing regardless of what the frontend sends — the
id in the URL is never trusted, only the header-derived identity is. The
frontend's "seller dashboard" is a mock selector over the sellers table,
standing in for a login screen.

**Seller status and buyer visibility.** Sellers have `APPROVED` /
`PENDING` / `REJECTED` status. Buyer-facing queries only ever return
listings from `APPROVED` sellers with `active = true` and `stock >= 0`
(seed data includes a `PENDING` seller's listings specifically to prove
they're filtered out). A seller's own dashboard shows all of *their*
listings regardless of their own status, since a pending seller should
still be able to set their catalogue up before approval.

**Optimistic concurrency.** `seller_listings.version` (`@Version`) guards
against two people editing the same listing at once — see the Concurrency
section below.

**Soft "stop selling".** A seller stops selling a product by flipping
`active = false`, not by deleting the row. This keeps the listing's history
intact and is what a real system would need anyway once orders reference a
listing.

**Errors.** A single `GlobalExceptionHandler` maps domain exceptions to
HTTP status codes with a consistent JSON shape (`{status, error, message,
details}`): 400 for validation, 401/403 for the mocked auth boundary, 404
for missing resources, 409 for duplicate listings and optimistic-lock
conflicts, 422 for business-rule violations (e.g. price ≤ 0).

## 3. Assumptions made

- "Reasonable mechanism" for auth = a request header (`X-Seller-Id`)
  resolved against the `sellers` table, per the brief's explicit note that
  production auth isn't required. The frontend persists the chosen seller
  in `localStorage` purely as a UX convenience.
- Any seller can add a **product** to the shared catalogue (there's no
  catalogue "owner") — only **listings** are seller-scoped. In a real
  system, new catalogue entries would probably go through a moderation
  step; that's out of scope here.
- Buyers are anonymous — there's no buyer account, cart, or order. The
  brief explicitly excludes checkout.
- A listing with `stock = 0` or `stock < minOrderQty` is still shown to
  buyers (so they can see it exists and its normal price) but flagged as
  not currently orderable (`ListingDto.orderable`), rather than hidden —
  this felt closer to how real marketplaces behave ("out of stock" is
  still useful information) than silently disappearing.
- Price/stock/MOQ validation treats `price <= 0`, `stock < 0`, and
  `minOrderQty < 1` as hard errors; `stock < minOrderQty` is *not* a hard
  error (a seller may legitimately be temporarily low on stock).

## 4. What was intentionally not implemented

- Real authentication/authorization (sessions, JWTs, password hashing).
- Checkout, cart, orders, payments, RFQ workflows — explicitly out of
  scope per the brief.
- Seller self-registration / approval workflow UI (sellers and their
  status are seeded directly; there's no "become a seller" or "admin
  approves seller" screen).
- Product images, reviews, ratings.
- Full-text/typo-tolerant search — see the scale notes below for what this
  would become at real volume.

## 5. What would improve with more time

- Promote `category` (and possibly `brand`) to its own lookup table
  instead of a free-text column, so the frontend filter list is driven by
  real data rather than an ad hoc `DISTINCT` query.
- Add a lightweight seller-registration and admin-approval flow instead of
  seeding seller status directly, since "not every seller is visible to
  buyers" implies some process moves a seller between statuses.
- Add pessimistic locking or a message-queue-backed stock decrement path
  once actual orders exist — optimistic locking is the right choice for
  "two admins editing a listing in the seller dashboard," but a real
  checkout flow under contention wants something stronger (see below).
- Cursor-based pagination instead of offset pagination for the product
  list, and server-driven faceted filters (price range, in-stock only).
- More frontend tests (currently backend-only); at minimum component tests
  for `AddListingModal`'s validation and the price-sort/compare view.

## 6. What I'd reconsider at millions of products/listings

- **Search**: the current `LOWER(...) LIKE '%term%'` query is fine for a
  demo but doesn't use an index at that scale. I'd move to MySQL
  `FULLTEXT` indexes as a first step, and to a dedicated search engine
  (OpenSearch/Elasticsearch/Meilisearch) if faceted filtering and
  typo tolerance become important — 10M+ listings joined against 1M
  products for "cheapest offer per product" is exactly the kind of query
  that benefits from a purpose-built index rather than relational joins.
- **"Cheapest listing per product"**: today this is a query-time
  `MIN(price)` over `seller_listings` filtered by product + seller status.
  At scale I'd precompute and cache this (a materialized view refreshed
  on listing write, or a denormalized `cheapest_price` column on
  `products` updated by trigger/event) rather than aggregating live on
  every catalogue page render.
- **Pagination**: offset pagination (`LIMIT/OFFSET`) degrades on deep pages
  at this volume; I'd switch to keyset/cursor pagination ordered by an
  indexed column.
- **Concurrency**: optimistic locking (`@Version`) is fine for the seller
  dashboard's low-contention "one seller edits one listing" case, which is
  all this project implements. It would *not* be fine for a hot listing
  being decremented by many concurrent buyer purchases — that needs an
  atomic `UPDATE ... SET stock = stock - :qty WHERE stock >= :qty`
  (conditional update, no read-modify-write race) or a queue-based
  reservation system, which I'd introduce once checkout exists.
- **Partitioning/sharding**: `seller_listings` at 10M rows is still
  comfortably within a single well-indexed InnoDB table's range, so I
  wouldn't reach for partitioning yet — I'd revisit if it grew another
  order of magnitude or if seller-scoped queries started dominating (in
  which case partitioning by `seller_id` range would fit the access
  pattern).
- **Caching**: category lists, "popular products," and other read-heavy,
  slow-changing data would move behind a cache (Redis) rather than hitting
  MySQL on every request.

## 7. Concurrency (as requested in the brief)

Two people editing the *same seller listing* at the same time is handled
with optimistic locking: `seller_listings.version` increments on every
update. The frontend's edit form carries the version it last read; the
backend rejects an update whose version doesn't match the current row
(`409 Conflict`, "this listing was changed by another request — reload and
try again") instead of silently letting the second write clobber the
first. `SellerListingConcurrencyIT` exercises this directly against a real
persistence context (not a mock) to prove the guarantee actually holds,
alongside a test proving the DB-level unique constraint stops duplicate
listings. See "millions of products" above for why this wouldn't be
sufficient once real stock-decrementing purchases exist.

## 8. Project layout

```
backend/    Spring Boot REST API (Java 17, Maven)
  src/main/java/com/bajrix/marketplace/
    entity/      Product, Seller, SellerListing, SellerStatus
    repository/  Spring Data JPA repositories
    service/     business logic + authorization boundary
    controller/  REST endpoints
    dto/         request/response shapes
    auth/        mocked X-Seller-Id resolution (SellerContext, interceptor)
    exception/   domain exceptions + GlobalExceptionHandler
    config/      CORS + interceptor wiring
  src/main/resources/db/migration/   Flyway schema + seed data
  src/test/                          unit tests + a real-DB concurrency test

frontend/   React (Vite) SPA
  src/pages/       BuyerHome (search/browse), ProductDetail (compare
                    sellers), SellerDashboard (manage own listings)
  src/components/  AddListingModal, Pagination, Money
  src/context/      mocked "logged in as seller" state
  src/api/client.js thin fetch wrapper mirroring the backend's endpoints

docker-compose.yml   MySQL + backend + frontend, one command
```

## 9. API overview

No formal spec was required, so this is a summary — see Swagger UI
(`/swagger-ui.html`) for the live, authoritative version.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/products` | none | search/browse (`q`, `category`, `page`, `size`, `sortBy`, `sortDir`) |
| GET | `/api/products/{id}` | none | product detail + all buyer-visible seller listings, cheapest first |
| POST | `/api/products` | none | add a catalogue product |
| GET | `/api/sellers` | none | seller directory (drives the mock seller picker) |
| GET | `/api/seller/listings` | `X-Seller-Id` | the current seller's own listings |
| POST | `/api/seller/listings` | `X-Seller-Id` | create a listing |
| PUT | `/api/seller/listings/{id}` | `X-Seller-Id` | update price/stock/MOQ (version-checked) |
| DELETE | `/api/seller/listings/{id}` | `X-Seller-Id` | stop selling (soft delete, version-checked) |
