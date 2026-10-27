# Stockroom: Inventory and Order Service

A backend service with a small web page for reserving stock. When a customer starts checkout, the items are held for a limited time. If they don't pay in time, the hold expires and the stock goes back on the shelf. The service never oversells, even when many people buy at the same moment.

## Features

- Product catalogue with categories, validation, and create, read, update and delete
- Time-limited stock reservation, with a scheduled job that releases expired holds
- Pay and cancel endpoints with safe status changes
- Low-stock alerts through an endpoint and a log warning
- Ranked full-text search with price and in-stock filters
- A demo web page with search, category filters and countdown timers for each hold

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

You only need Docker. Run these commands:

    git clone https://github.com/AYDEED7/inventory-service.git
    cd inventory-service
    docker compose up -d --build

Then open http://localhost:8081/ in your browser. The first build takes a few minutes.

- Stop everything: `docker compose stop`
- Start it again: `docker compose up -d`
- After changing code: `docker compose up -d --build`

To run the app from your editor instead, start only the database with `docker compose up -d db`, then run `ShopApplication` or `./mvnw spring-boot:run`. Stop the Docker `app` container first, because both use port 8081.

To run the tests, start the database with `docker compose up -d db` and run `./mvnw test`.

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/products` | List all products |
| GET | `/products/{id}` | Get one product |
| POST | `/products` | Create a product |
| PUT | `/products/{id}` | Update a product |
| DELETE | `/products/{id}` | Delete a product |
| GET | `/products/search?q=keyboard&maxPrice=50&inStock=true` | Ranked full-text search |
| GET | `/products/low-stock?threshold=5` | Products with fewer than the threshold left |
| POST | `/orders` | Reserve stock, for example `{"productId": 2, "quantity": 3}` |
| GET | `/orders/{id}` | Get an order |
| POST | `/orders/{id}/pay` | Confirm a pending order |
| POST | `/orders/{id}/cancel` | Cancel a pending order and release the stock |

Errors use standard status codes: 400 for invalid input, 404 for a missing product or order, and 409 when there is not enough stock or an order is no longer pending.

## How it works

**No overselling.** Reading the stock in Java, checking it, then writing it back lets two buyers both succeed. Instead, reserving is one SQL statement:

    UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty

PostgreSQL locks the row while it runs, so simultaneous buyers are handled one at a time. Zero rows updated means there was not enough stock.

**All or nothing.** Reserving stock and saving the order happen in one transaction, so a failure rolls both back.

**Safe status changes.** Pay, cancel and expire are conditional updates that only apply to pending orders. Stock is released only when exactly one row changed, so it cannot be released twice.

**Expiry.** A scheduled job runs every 10 seconds and expires pending orders past their deadline. The hold length is 10 minutes by default and can be changed with `shop.reservation-minutes`.

**Versioned schema.** Flyway migrations create the tables, and Hibernate only validates them:

- V1: products table
- V2: orders table
- V3: full-text search column and index
- V4: categories and sample products

**Same image everywhere.** The app reads its database settings from environment variables, so the same Docker image runs on a laptop or a server.

## Results

### Concurrency test

`ConcurrencyTest` starts 50 threads that each try to buy 1 item of a product with 10 in stock. It checks that exactly 10 orders succeed, 40 are rejected, and the final stock is 0, never negative.

### Search benchmark

I compared a `LIKE` search with a full-text search that uses an index, on 100,000 generated products in PostgreSQL 16, searching for the word "telescope". Both returned the same 100 products.

| Method | How it works | Time |
|---|---|---|
| `LIKE` | Reads all 100,000 rows | 70.8 ms |
| Full-text search with index | Looks the word up in an index | 2.8 ms |

The indexed search was about 25 times faster. Each time is one `EXPLAIN ANALYZE` measurement on a development laptop, so read it as a comparison rather than a guarantee. Full-text search also matches word forms ("telescopes") and can rank results, which `LIKE` cannot.

## Project structure

    src/main/java/com/aydeed/shop/    controllers, services, entities, repositories
    src/main/resources/db/migration/  Flyway migrations
    src/main/resources/static/        the demo web page
    src/test/java/com/aydeed/shop/    concurrency test
    Dockerfile                        builds the app image
    docker-compose.yml                app and PostgreSQL

## Limitations and next steps

- One product per order (a multi-item cart is planned)
- No login or roles yet
- No pagination on the product list
- The demo page remembers holds in the browser, not on the server
- Not deployed yet, an AWS deployment is planned