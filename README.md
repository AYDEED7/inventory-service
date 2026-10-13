# Stockroom: Inventory and Order Service

A backend service with a small web page for reserving stock. When a customer starts checkout, the items are **held** for a limited time. If they don't pay in time, the hold expires and the stock goes back on the shelf. The service never oversells, even when many people buy at the same moment.

![Stockroom page with a hold counting down](docs/screenshot.png)

## What it does

- **Product catalogue** with validation, categories and full create, read, update, delete
- **Time-limited stock reservation:** creating an order takes stock immediately, and a scheduled job releases it when the hold expires
- **Pay and cancel** endpoints, with safe status changes (an order can't be both paid and expired)
- **Low-stock alerts:** an endpoint listing products under a threshold, plus a warning in the logs
- **Ranked full-text search** with price and in-stock filters
- **A demo web page** with search, category filters and live countdown timers

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot (Web, Data JPA, Validation) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Tests | JUnit |
| Packaging | Docker and Docker Compose |
| Frontend | Plain HTML, CSS and JavaScript |

## Run it

You only need Docker.

```
git clone https://github.com/AYDEED7/inventory-service.git
cd inventory-service
docker compose up -d --build
```

Then open http://localhost:8081/. Stop it with `docker compose stop`.

To run the app from your editor instead, start only the database with `docker compose up -d db` and run `ShopApplication`, or use `./mvnw spring-boot:run` (`.\mvnw.cmd spring-boot:run` on Windows).

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

**Versioned schema.** Tables are created by Flyway migrations (`V1` to `V4`) and Hibernate only validates them (`ddl-auto=validate`), so the database structure is reviewed in Git like code.

## Results

**Concurrency test.** `ConcurrencyTest` starts 50 threads that all try to buy 1 item of a product with only 10 in stock. It asserts that exactly 10 orders succeed, 40 are rejected, and the final stock is 0, never negative. Run it with `./mvnw test` while the database is running.

**Search benchmark.** On 100,000 generated products, searching for a rare word (local PostgreSQL 16 in Docker, measured with `EXPLAIN ANALYZE`):

| Method | Plan | Execution time |
|---|---|---|
| `LIKE '%telescope%'` on name and description | Sequential scan, read all rows | 70.8 ms |
| Full-text search with a GIN index | Bitmap index scan | 2.8 ms |

That is roughly 25 times faster. These are single measurements on a development machine, so treat them as a comparison, not a guarantee. Full-text search also stems words, so a search for "telescope" matches "telescopes", which `LIKE` cannot do.

## Limitations and next steps

- One product per order (a multi-item cart is planned)
- No login or roles yet
- Not deployed yet; an AWS deployment is planned