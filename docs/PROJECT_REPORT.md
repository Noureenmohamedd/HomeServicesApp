# Home Services Marketplace Platform Report

## 1. Project Overview

This project implements an on-demand home services marketplace using a distributed microservices architecture. The system allows customers to register, manage wallet balance, browse provider offers, book services, receive booking notifications, and view booking history. Providers can register with a profession type, create and update service offers, receive booking information, and complete services. Admins can manage service categories and view users and transaction/booking history.

The project is split into independent services:

- `user-service-ejb`
- `offer-service`
- `booking-service`
- `notification-service`
- `RabbitMQ`

Each service owns its own data and communicates with other services through REST APIs or RabbitMQ messages. No service reads another service database directly.

## 2. Architecture

```text
Customer / Provider / Admin
        |
        | REST
        v
+------------------+       REST        +----------------+
|  offer-service   | <---------------> | user-service   |
|  offers/categories|                  | EJB/auth/wallet|
+------------------+                  +----------------+
        ^
        | REST
        |
+------------------+       REST        +----------------+
| booking-service  | <---------------> | user-service   |
| bookings/payment |                  | wallet deduct   |
+------------------+                  +----------------+
        |
        | RabbitMQ events
        v
+------------------+       consumes     +----------------------+
|     RabbitMQ     | --------------->  | notification-service |
| booking.exchange |                   | persistent notifs    |
+------------------+                   +----------------------+
```

## 3. Services

### 3.1 User Service EJB

Path:

```text
user-service-ejb
```

Base URL:

```text
http://localhost:8080/user-service-ejb-1.0-SNAPSHOT/api
```

Technology:

- Jakarta EE
- EJB
- JAX-RS REST API
- JPA/Hibernate
- WildFly

EJB Types Used:

- Stateless EJB:
  - `UserServiceBean`
  - `AuthServiceBean`
  - `JwtServiceBean`
- Singleton EJB:
  - `SessionManagerBean`

Responsibilities:

- Register customers.
- Register service providers.
- Register admins.
- Login and generate JWT tokens.
- Validate JWT tokens for other services.
- Store users and roles.
- Store customer wallet balances.
- Add wallet funds.
- Deduct wallet balance during booking.
- Refund wallet balance if rollback is needed.
- Store wallet transaction records.
- Allow admins to view users and transaction history.

Important logic:

- Customers register with an initial balance.
- Providers register with a profession type such as `PLUMBER`, `CARPENTER`, or `ELECTRICIAN`.
- Login returns a JWT token containing user ID, username, role, and profession type.
- Other services validate or decode this token to authorize users.
- Wallet deduction checks whether the customer has enough balance.
- Wallet refund restores money if booking/payment rollback is needed.

Main endpoints:

```text
POST /users/register/customer
POST /users/register/provider
POST /admin/register
POST /users/login
GET  /users/token/validate
GET  /users
GET  /users/{id}
POST /users/wallet/add/{id}
GET  /users/wallet/{id}
POST /users/wallet/deduct/{userId}
POST /users/wallet/refund/{userId}
GET  /admin/users
GET  /admin/users/registered
GET  /admin/users/admins
GET  /admin/transactions
```

### 3.2 Offer Service

Path:

```text
offer-service
```

Base URL:

```text
http://localhost:8081
```

Technology:

- Spring Boot
- Spring MVC REST API
- Spring Security
- JPA/Hibernate
- H2 persistent database

Responsibilities:

- Store service categories.
- Allow admins to add categories.
- Allow providers to create service offers.
- Allow providers to update offer details.
- Allow customers to browse offers by category.
- Allow users to view active offers.
- Validate that providers only create offers matching their profession.

Important logic:

- A provider can only create/update an offer category that matches their profession.
- Example: a provider with profession `PLUMBER` can create offers under `PLUMBING`.
- Offers have:
  - title
  - description
  - category
  - price
  - available date/time
  - availability status
- The public API uses only `availabilityStatus`, not the old boolean `available`.
- Supported public availability states:
  - `AVAILABLE`
  - `UNAVAILABLE`
- The internal `available` boolean remains only for compatibility but is not returned in API responses.

Main endpoints:

```text
POST /api/admin/categories
GET  /api/admin/categories
POST /api/offers
GET  /api/offers
GET  /api/offers/{id}
GET  /api/offers/provider/{providerId}
GET  /api/offers/category/{category}
GET  /api/offers/active
PUT  /api/offers/{id}
DELETE /api/offers/{id}
```

Update offer request example:

```json
{
  "title": "Pipe Repair Updated",
  "description": "Fix kitchen and bathroom pipe leaks",
  "price": 60,
  "category": "PLUMBING",
  "availableDateTime": "2026-05-08T10:00:00",
  "availabilityStatus": "AVAILABLE"
}
```

Title-only update is accepted:

```json
{
  "title": "Pipe Repair333"
}
```

Set offer unavailable:

```json
{
  "availabilityStatus": "UNAVAILABLE"
}
```

### 3.3 Booking Service

Path:

```text
booking-service
```

Base URL:

```text
http://localhost:8082
```

Technology:

- Spring Boot
- Spring MVC REST API
- Spring Security
- JPA/Hibernate
- RabbitMQ publisher
- H2 persistent database

Responsibilities:

- Create bookings.
- Validate customer role.
- Fetch offer details from `offer-service`.
- Check offer availability.
- Check customer wallet balance through `user-service-ejb`.
- Deduct wallet balance through `user-service-ejb`.
- Save booking records.
- Publish booking lifecycle events to RabbitMQ.
- Allow customers to view booking history.
- Allow providers to view received bookings.
- Allow providers to mark confirmed bookings as completed.
- Allow admins to view booking history.

Booking statuses:

```text
CONFIRMED
REJECTED
COMPLETED
```

Booking logic:

1. Customer sends booking request with `offerId`.
2. Booking service validates that authenticated user is a customer.
3. Booking service calls offer-service to fetch the offer.
4. If offer is unavailable, booking is saved as `REJECTED`.
5. If offer is available, booking-service checks customer wallet balance.
6. If balance is insufficient, booking is saved as `REJECTED`.
7. If balance is sufficient, booking-service deducts the amount from user-service.
8. If deduction succeeds, booking is saved as `CONFIRMED`.
9. If something fails after deduction, booking-service refunds the wallet.
10. Booking-service publishes a RabbitMQ event for notification-service.

Main endpoints:

```text
POST /api/bookings
GET  /api/bookings/customer/{customerId}
GET  /api/bookings/provider/{providerId}
GET  /api/bookings/provider/{providerId}/completed
GET  /api/bookings/offer/{offerId}
PUT  /api/bookings/{bookingId}/complete
GET  /api/bookings/admin/all
GET  /api/admin/bookings
GET  /api/services/completed
```

Create booking request:

```json
{
  "offerId": 1
}
```

Confirmed booking result:

```json
{
  "status": "CONFIRMED"
}
```

Rejected booking result:

```json
{
  "status": "REJECTED"
}
```

### 3.4 Notification Service

Path:

```text
notification-service
```

Base URL:

```text
http://localhost:8084
```

Technology:

- Spring Boot
- Spring MVC REST API
- Spring AMQP
- RabbitMQ consumer
- JPA/Hibernate
- H2 persistent database

Responsibilities:

- Consume RabbitMQ booking events.
- Store notifications in its own database.
- Return customer notifications through REST.
- Return provider notifications through REST.
- Keep notifications persistent after service restart.

Persistent database:

```text
notification-service/data/notification-service-db.mv.db
```

Main endpoints:

```text
GET /api/notifications/customers/{customerId}
GET /api/notifications/providers/{providerId}
```

Notification types:

```text
BOOKING_CONFIRMATION
BOOKING_REJECTION
BOOKING_COMPLETION
```

## 4. RabbitMQ

RabbitMQ is used for asynchronous notifications. Booking-service does not call notification-service directly. Instead, booking-service publishes an event to RabbitMQ, and notification-service consumes it.

RabbitMQ management UI:

```text
http://localhost:15672
```

Credentials:

```text
username: guest
password: guest
```

Exchange:

```text
booking.exchange
type: topic
```

Queues:

```text
booking.confirmed.notifications.queue
booking.rejected.notifications.queue
booking.completed.notifications.queue
```

Routing keys:

```text
booking.confirmed
booking.rejected
booking.completed
```

Event flow:

```text
booking-service -> booking.exchange -> notification queue -> notification-service -> notification DB
```

Confirmed booking:

```text
routing key: booking.confirmed
queue: booking.confirmed.notifications.queue
recipients: customer and provider
```

Rejected booking:

```text
routing key: booking.rejected
queue: booking.rejected.notifications.queue
recipients: customer only
```

Completed booking:

```text
routing key: booking.completed
queue: booking.completed.notifications.queue
recipients: customer and provider
```

Important note:

If notification-service is running, RabbitMQ queues may show zero messages because messages are consumed immediately. This is correct. To see a message stay in RabbitMQ:

1. Stop notification-service.
2. Create a booking.
3. Refresh RabbitMQ UI.
4. The queue should show `Ready = 1`.
5. Start notification-service.
6. The queue should return to `Ready = 0`.
7. The notification should appear through the REST endpoint.

## 5. Databases

Each service owns its own persistent database.

```text
user-service-ejb:
WildFly UserDS configured database

offer-service:
offer-service/data/offerdb.mv.db

booking-service:
booking-service/data/booking-service-db.mv.db

notification-service:
notification-service/data/notification-service-db.mv.db
```

No service should access another service database directly. Services communicate through REST or RabbitMQ.

## 6. Running The System

### 6.1 Start RabbitMQ

From project root:

```powershell
docker compose up -d rabbitmq
```

Check RabbitMQ UI:

```text
http://localhost:15672
```

### 6.2 Start User Service EJB

The user service runs on WildFly.

Build the WAR:

```powershell
cd user-service-ejb
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd package
```

Then run the WildFly configuration in IntelliJ.

Expected context root:

```text
/user-service-ejb-1.0-SNAPSHOT
```

Expected base URL:

```text
http://localhost:8080/user-service-ejb-1.0-SNAPSHOT/api
```

### 6.3 Start Offer Service

```powershell
cd offer-service
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:8081
```

### 6.4 Start Booking Service

```powershell
cd booking-service
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:8082
```

### 6.5 Start Notification Service

```powershell
cd notification-service
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:8084
```

## 7. Full Test Scenario

Use these Postman variables:

```text
userBase = http://localhost:8080/user-service-ejb-1.0-SNAPSHOT/api
offerBase = http://localhost:8081
bookingBase = http://localhost:8082
notificationBase = http://localhost:8084
```

### Step 1: Register Admin

```text
POST {{userBase}}/admin/register
```

```json
{
  "username": "admin",
  "password": "123"
}
```

### Step 2: Login Admin

```text
POST {{userBase}}/users/login
```

```json
{
  "username": "admin",
  "password": "123"
}
```

Save:

```text
adminToken
```

### Step 3: Register Provider

```text
POST {{userBase}}/users/register/provider
```

```json
{
  "username": "provider1",
  "password": "123",
  "professionType": "PLUMBER"
}
```

### Step 4: Login Provider

```text
POST {{userBase}}/users/login
```

```json
{
  "username": "provider1",
  "password": "123"
}
```

Save:

```text
providerToken
providerId
```

### Step 5: Register Customer

```text
POST {{userBase}}/users/register/customer
```

```json
{
  "username": "customer1",
  "password": "123",
  "balance": 200
}
```

### Step 6: Login Customer

```text
POST {{userBase}}/users/login
```

```json
{
  "username": "customer1",
  "password": "123"
}
```

Save:

```text
customerToken
customerId
```

### Step 7: Admin Adds Category

```text
POST {{offerBase}}/api/admin/categories
Authorization: Bearer {{adminToken}}
```

```json
{
  "name": "PLUMBING",
  "professionType": "PLUMBER"
}
```

### Step 8: Provider Creates Offer

```text
POST {{offerBase}}/api/offers
Authorization: Bearer {{providerToken}}
```

```json
{
  "title": "Pipe Repair",
  "description": "Fix leaking kitchen or bathroom pipes",
  "price": 80,
  "category": "PLUMBING",
  "availableDateTime": "2026-05-08T10:00:00",
  "availabilityStatus": "AVAILABLE"
}
```

Save:

```text
offerId
```

### Step 9: Customer Browses Offers

```text
GET {{offerBase}}/api/offers/category/PLUMBING
Authorization: Bearer {{customerToken}}
```

### Step 10: Customer Checks Wallet

```text
GET {{userBase}}/users/wallet/{{customerId}}
Authorization: Bearer {{customerToken}}
```

### Step 11: Customer Books Offer

```text
POST {{bookingBase}}/api/bookings
Authorization: Bearer {{customerToken}}
```

```json
{
  "offerId": {{offerId}}
}
```

Expected if balance is enough:

```text
status = CONFIRMED
```

### Step 12: Check Wallet After Booking

```text
GET {{userBase}}/users/wallet/{{customerId}}
Authorization: Bearer {{customerToken}}
```

If offer price is `80` and balance was `200`, expected balance:

```text
120
```

### Step 13: Check Customer Notification

```text
GET {{notificationBase}}/api/notifications/customers/{{customerId}}
```

Expected:

```text
BOOKING_CONFIRMATION
```

### Step 14: Check Provider Notification

```text
GET {{notificationBase}}/api/notifications/providers/{{providerId}}
```

Expected:

```text
BOOKING_CONFIRMATION
```

### Step 15: Provider Completes Booking

```text
PUT {{bookingBase}}/api/bookings/{{bookingId}}/complete
Authorization: Bearer {{providerToken}}
```

Expected:

```text
status = COMPLETED
```

### Step 16: Check Completion Notifications

Customer:

```text
GET {{notificationBase}}/api/notifications/customers/{{customerId}}
```

Provider:

```text
GET {{notificationBase}}/api/notifications/providers/{{providerId}}
```

Expected:

```text
BOOKING_COMPLETION
```

## 8. Rejected Booking Scenario

Register a customer with low balance:

```text
POST {{userBase}}/users/register/customer
```

```json
{
  "username": "poorCustomer",
  "password": "123",
  "balance": 5
}
```

Login and save:

```text
poorCustomerToken
poorCustomerId
```

Try booking an offer with price greater than `5`:

```text
POST {{bookingBase}}/api/bookings
Authorization: Bearer {{poorCustomerToken}}
```

```json
{
  "offerId": {{offerId}}
}
```

Expected:

```text
status = REJECTED
```

Check notification:

```text
GET {{notificationBase}}/api/notifications/customers/{{poorCustomerId}}
```

Expected:

```text
BOOKING_REJECTION
```

## 9. Admin Checks

View registered users:

```text
GET {{userBase}}/admin/users/registered
Authorization: Bearer {{adminToken}}
```

View admin users:

```text
GET {{userBase}}/admin/users/admins
Authorization: Bearer {{adminToken}}
```

View wallet transactions:

```text
GET {{userBase}}/admin/transactions
Authorization: Bearer {{adminToken}}
```

View booking history:

```text
GET {{bookingBase}}/api/admin/bookings
Authorization: Bearer {{adminToken}}
```

## 10. Design Assumptions

- Wallet functionality is part of `user-service-ejb`, not a separate wallet service, because wallet balance is strongly related to customer registration and EJB requirements.
- Each service owns its data.
- RabbitMQ is used for asynchronous notification delivery.
- Notification-service stores notifications persistently in its own H2 database.
- Services use REST calls for synchronous operations such as token validation, offer lookup, and wallet operations.
- RabbitMQ queues may show zero messages while the system is working correctly because notification-service consumes messages immediately.

