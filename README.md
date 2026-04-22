# 🚀 Kafka Order System — Learning Project
> **Spring Boot + Angular + Apache Kafka + H2**
> A real-world example to learn Kafka concepts hands-on.

---

## 📐 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FULL FLOW                                     │
│                                                                       │
│  Angular UI                                                           │
│  (localhost:4200)                                                     │
│       │                                                               │
│       │  1. POST /api/orders  (place order)                          │
│       ▼                                                               │
│  Spring Boot API                                                      │
│  (localhost:8080)                                                     │
│       │                                                               │
│       │  2. Save Order to H2 (status=PENDING)                        │
│       │  3. Publish to Kafka topic: "order-placed"                   │
│       │  4. Return 201 immediately (non-blocking!)                   │
│       │                                                               │
│       ▼                                                               │
│  Apache Kafka                                                         │
│  (localhost:9092)                                                     │
│  Topic: order-placed  [partition-0] [partition-1] [partition-2]      │
│       │                                                               │
│       │  5. Consumer receives message (async, 2–5 sec later)         │
│       ▼                                                               │
│  Spring Boot Consumer (@KafkaListener)                                │
│       │  6. Update status → PROCESSING                               │
│       │  7. Simulate work (payment check, inventory...)              │
│       │  8. Update status → COMPLETED or FAILED (80/20 chance)       │
│       │  9. Publish to topic: "order-processed"                      │
│       │  10. Fire Spring event → SSE push to Angular                 │
│       ▼                                                               │
│  Angular UI receives SSE push                                         │
│       └─▶ Table auto-updates with new status 🎉                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start (3 Steps)

### Step 1 — Start Kafka with Docker

```bash
cd kafka-order-project
docker-compose up -d
```

This starts:
- **Zookeeper** on port 2181
- **Kafka Broker** on port 9092
- **Kafka UI** on http://localhost:8090 (visual topic browser!)

Verify Kafka is running:
```bash
docker-compose ps
# All containers should show "Up"
```

---

### Step 2 — Start Spring Boot Backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at **http://localhost:8080**

Useful URLs:
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:orderdb`
  - Username: `sa`
  - Password: _(leave blank)_
- **API**: http://localhost:8080/api/orders

---

### Step 3 — Start Angular Frontend

```bash
cd frontend
npm install
npm start
```

App opens at **http://localhost:4200** 🎉

---

## 📁 Project Structure

```
kafka-order-project/
│
├── docker-compose.yml          ← Kafka + Zookeeper + Kafka UI
│
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/kafkaorder/
│       ├── KafkaOrderApp.java              ← Main class
│       ├── config/
│       │   └── KafkaConfig.java            ← Topic definitions, CORS
│       ├── entity/
│       │   └── Order.java                  ← JPA Entity (maps to H2)
│       ├── dto/
│       │   ├── OrderRequest.java           ← Incoming HTTP body
│       │   └── OrderEvent.java             ← Kafka message payload
│       ├── repository/
│       │   └── OrderRepository.java        ← Spring Data JPA
│       ├── service/
│       │   ├── OrderService.java           ← Business logic
│       │   ├── OrderProducer.java          ← KAFKA PRODUCER ⭐
│       │   ├── OrderConsumer.java          ← KAFKA CONSUMER ⭐
│       │   ├── OrderStatusChangedEvent.java
│       │   └── OrderStatusEventListener.java
│       └── controller/
│           └── OrderController.java        ← REST API + SSE
│
└── frontend/
    └── src/app/
        ├── models/order.model.ts           ← TypeScript interfaces
        ├── services/order.service.ts       ← HTTP + SSE client
        ├── components/
        │   ├── order-form/                 ← Place order form
        │   └── order-list/                 ← Live order table
        └── app.component.*                 ← Root component + layout
```

---

## 📚 Kafka Concepts Explained (with Code References)

### 1. Topics & Partitions
```java
// KafkaConfig.java
@Bean
public NewTopic orderPlacedTopic() {
    return TopicBuilder.name("order-placed")
            .partitions(3)   // 3 partitions = 3 consumers can work in parallel
            .replicas(1)     // 1 replica (fine for dev, use 3 in production)
            .build();
}
```

### 2. Producer
```java
// OrderProducer.java
kafkaTemplate.send("order-placed", orderId.toString(), event);
//                  ↑ topic          ↑ key (same key → same partition)  ↑ value (JSON)
```

### 3. Consumer
```java
// OrderConsumer.java
@KafkaListener(topics = "order-placed", groupId = "order-processor-group")
public void consumeOrderPlaced(ConsumerRecord<String, OrderEvent> record) {
    // record.partition() — which partition this came from
    // record.offset()    — position within that partition
    // record.value()     — the OrderEvent object (deserialized from JSON)
}
```

### 4. Consumer Groups
Two different groups consume the `order-processed` topic independently:
```java
// Group 1: order-processor-group — the main processor
@KafkaListener(topics = "order-placed", groupId = "order-processor-group")

// Group 2: notification-group — simulates email notifications
@KafkaListener(topics = "order-processed", groupId = "notification-group")
```

### 5. Serialization
```properties
# Sending: Java Object → JSON bytes
spring.kafka.producer.value-serializer=JsonSerializer

# Receiving: JSON bytes → Java Object
spring.kafka.consumer.value-deserializer=JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.example.kafkaorder.dto
```

---

## 🌐 API Reference

| Method | Endpoint             | Description                    |
|--------|----------------------|--------------------------------|
| POST   | `/api/orders`        | Place a new order              |
| GET    | `/api/orders`        | Get all orders                 |
| GET    | `/api/orders/{id}`   | Get single order               |
| GET    | `/api/orders/stats`  | Dashboard stats by status      |
| GET    | `/api/orders/stream` | SSE stream (real-time updates) |

### Example POST
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Ravi Sharma",
    "product": "MacBook Pro 16",
    "quantity": 1,
    "price": 2499.99
  }'
```

Response (instant, before Kafka processing):
```json
{
  "id": 1,
  "customerName": "Ravi Sharma",
  "product": "MacBook Pro 16",
  "quantity": 1,
  "price": 2499.99,
  "status": "PENDING",
  "statusMessage": "Order received. Waiting for processing...",
  "createdAt": "2024-03-22T10:30:00",
  "processedAt": null
}
```

---

## 🔍 Monitoring

### Kafka UI (Best way to understand what's happening!)
Open **http://localhost:8090** to see:
- All topics and their partition counts
- Messages flowing through each topic
- Consumer group offsets
- Which partition each message went to

### H2 Console
Open **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:orderdb`
- Run SQL: `SELECT * FROM ORDERS ORDER BY CREATED_AT DESC`

### Application Logs
Watch the Spring Boot console — every Kafka event is logged:
```
📨 Received message | topic=order-placed | partition=1 | offset=3 | orderId=5
🔄 Order #5 status updated to PROCESSING
⚙️  Processing order... (3241ms)
🔄 Order #5 status updated to COMPLETED
✅ Published order-processed | orderId=5 | status=COMPLETED
🔔 Notification consumer: orderId=5 finalStatus=COMPLETED — would send email here
```

---

## 💡 What to Experiment With

1. **Place 5-10 orders quickly** — watch them process in parallel across 3 partitions
2. **Check Kafka UI** — see messages land on different partitions
3. **H2 Console** — query the DB while orders are processing
4. **Watch the logs** — see partition/offset printed for each message
5. **Change consumer group** — add a new `@KafkaListener` with a different `groupId`
6. **Stop and restart** — Kafka remembers offsets; no messages are lost!

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Frontend  | Angular   | 17      |
| Backend   | Spring Boot| 3.2.3  |
| Messaging | Apache Kafka | 7.5   |
| Database  | H2 (in-memory) | —  |
| Container | Docker Compose | —  |

---

## ❓ Troubleshooting

**"Connection refused" on Kafka**
```bash
docker-compose ps       # Check containers are running
docker-compose logs kafka  # Check Kafka logs
```

**Angular can't reach backend**
```bash
# Make sure Spring Boot is running:
curl http://localhost:8080/api/orders
```

**H2 data disappears on restart**
That's expected! H2 is in-memory (`mem:orderdb`). Change to file-based if needed:
```properties
spring.datasource.url=jdbc:h2:file:./orderdb
```
