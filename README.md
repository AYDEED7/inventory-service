# Stockroom: Inventory and Order Service

A backend service with a small web page for reserving stock. When a customer starts checkout, the items are **held** for a limited time. If they don't pay in time, the hold expires and the stock goes back on the shelf. The service never oversells, even when many people buy at the same moment.

![Stockroom page with a hold counting down](docs/screenshot.png)

## What it does

- **Product catalogue** with categories, validation, and full create, read, update, delete
- **Time-limited stock reservation:** creating an order takes stock immediately, and a scheduled job releases it when the hold expires
- **Pay and cancel** endpoints, with safe status changes (an order can't be both paid and expired)
- **Low-stock alerts:** an endpoint listing products under a threshold, plus a warning in the logs
- **Ranked full-text search** with price and in-stock filters
- **Demo web page** with search, category filters, stock levels per product, and live countdown rings for each hold

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot (Web, Data JPA, Validation) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Tests | JUnit |
| Packaging | Docker and Docker Compose |
| Frontend | Plain HTML, CSS and JavaScript, served by Spring |

## Run it

You only need Docker.

```
git clone https://github.com/AYDEED7/inventory-service.git
cd inventory-service
docker compose up -d --build
```

Then open http://localhost:8081/. The first build takes a few minutes. Stop everything with `docker compose stop`, and start it again with `docker compose up -d`. After changing code, rebuild with `docker compose up -d --build`.

**Run from your editor instead.** Start only the database with `docker compose up -d db`, then run `ShopApplication`, or use `./mvnw spring-boot:run` (`.\mvnw.cmd spring-boot:run` on Windows). Stop the Docker `app` container first, since both use port 8081.

**Run the tests.** The database must be running (`docker compose up -d db`), then run `./mvnw test`.

## API

| Method | Path | Description |
|---|---|---|
| GET | `/products` | List all products |
| GET | `/products/{id}` | Get one product |
| POST | `/products` | Create a product |
| PUT | `/products/{id}` | Update a product |
| DELETE | `/products/{id}` | Delete a product |
| GET | `/products/search?q=keyboard&maxPrice=50&inStock=true` | Ranked full-text search |
| GET | `/products/low-stock?threshold=5` | Products with fewer than the threshold left |
| POST | `/orders` | Reserve stock: `{"productId": 2, "quantity": 3}` |
| GET | `/orders/{id}` | Get an order |
| POST | `/orders/{id}/pay` | Confirm a pending order |
| POST | `/orders/{id}/cancel` | Cancel a pending order and release the stock |

A product looks like `{"name": "Mug", "description": "Blue ceramic mug", "price": 9.99, "stock": 50, "category": "Kitchen"}`. The category is optional and defaults to `General`.

Errors use normal status codes: 400 for invalid input, 404 for a missing product or order, and 409 when there isn't enough stock or an order is no longer pending.

## Design decisions

**No overselling.** The obvious approach reads the stock, checks it in Java, then writes the new value. Two buyers can read the same number at the same time and both succeed. Instead, reserving is a single statement:

```sql
UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty
```

PostgreSQL locks the row while it runs, so simultaneous buyers are handled one at a time. Zero rows updated means not enough stock.

**All-or-nothing steps.** Reserving stock and saving the order happen in one `@Transactional` method, so a failure after the stock is reduced rolls the reduction back.

**Safe status changes.** Pay, cancel and expire are conditional updates (`... WHERE status = 'PENDING'`). Stock is released only when exactly one row changed, so it can't be released twice, and a late payment can't win against an expiry.

**Expiry.** A scheduled job runs every 10 seconds and expires pending orders past their deadline. The hold length defaults to 10 minutes and can be changed with `shop.reservation-minutes`.

**Versioned schema.** Tables are created by Flyway migrations and Hibernate only validates them (`ddl-auto=validate`), so the database structure is reviewed in Git like code:

| Migration | What it does |
|---|---|
| `V1` | Products table |
| `V2` | Orders table and expiry index |
| `V3` | Full-text search column and GIN index |
| `V4` | Product categories and sample products |

**One image, any environment.** The app reads its database settings from environment variables, so the same Docker image runs on a laptop or a server. Docker Compose starts the database first and waits for its health check before starting the app.

## Results

**Concurrency test.** `ConcurrencyTest` starts 50 threads that all try to buy 1 item of a product with only 10 in stock. It asserts that exactly 10 orders succeed, 40 are rejected, and the final stock is 0, never negative.

**Search benchmark.** How much faster is a database index than scanning every row?

*Setup:* 100,000 generated products in PostgreSQL 16 (running in Docker), searching for the word "telescope". It appears in 100 products, so both methods return the same 100 results.

| | Slow way: `LIKE` | Fast way: full-text search with an index |
|---|---|---|
| What it does | Reads every product and checks its text for the word | Looks the word up in an index, like the index at the back of a book |
| How PostgreSQL ran it | Sequential scan: all 100,000 rows read | Bitmap index scan: went straight to the matching rows |
| Time | **70.8 ms** | **2.8 ms** |
| Speed | baseline | **about 25 times faster** |

Full-text search also understands word forms, so "telescope" matches "telescopes", which `LIKE` cannot do, and it can rank the best matches first.

Each time is a single `EXPLAIN ANALYZE` measurement on a development laptop, so treat the result as a comparison, not a guarantee. To repeat it, run these in `psql` after loading the data:

```sql
EXPLAIN ANALYZE SELECT id, name FROM products
WHERE name ILIKE '%telescope%' OR description ILIKE '%telescope%';

EXPLAIN ANALYZE SELECT id, name FROM products
WHERE search_vector @@ websearch_to_tsquery('english', 'telescope');
```

## Project structure

```
src/main/java/com/aydeed/shop/    controllers, services, entities, repositories
src/main/resources/db/migration/  Flyway migrations (V1 to V4)
src/main/resources/static/        the demo web page
src/test/java/com/aydeed/shop/    concurrency test
Dockerfile                        builds the app image
docker-compose.yml                app and PostgreSQL
```

## Limitations and next steps

- One product per order (a multi-item cart is planned)
- No login or roles yet
- No pagination on the product list
- The demo page remembers your holds in the browser, not on the server
- Not deployed yet; an AWS deployment is planned