# Pulse — Distributed Microservices Platform

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud%20Gateway-Reactive-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0-blue.svg)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%207.0-green.svg)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Cache%20%26%20Presence-Redis-red.svg)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Streaming-Apache%20Kafka-black.svg)](https://kafka.apache.org/)
[![Novu](https://img.shields.io/badge/Notifications-Novu-purple.svg)](https://novu.co/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

**Pulse** is a distributed, event-driven microservices platform engineered for students, developers, and tech organizers. It powers authentication with GitHub OAuth2, rich developer profiles, hackathon lifecycle and team registrations, multimedia social content, real-time WebSocket chat with active room presence detection, transactional outbox messaging, fan-out timeline feeds, and multi-channel notifications via Novu.

---

## 🏛 Architecture Overview

```mermaid
flowchart TD
    Client(["Client / Web / Mobile"]) -->|HTTP / REST| Gateway["API Gateway (Port 8080)<br/>Spring Cloud Gateway + Reactive JWT Auth"]
    Client -.->|WebSocket / STOMP| Chat["Chat Service (Port 8087)<br/>Real-Time Chat, Presence, STOMP"]

    Gateway -->|/api/v1/auth/**| Auth["Auth Service (Port 8081)<br/>JWT, OAuth2, RBAC"]
    Gateway -->|/api/v1/users/**| User["User Service (Port 8083)<br/>Profiles, Blocks, Following"]
    Gateway -->|/api/v1/posts/** & /api/v1/hackathons/**| Post["Post & Hackathon Service (Port 8084)<br/>Content, Events, Outbox"]
    Gateway -->|/api/v1/notifications/**| Notif["Notification Service (Port 8085)<br/>Decision Engine & Novu Dispatch"]
    Gateway -->|/api/v1/feed/**| Feed["Feed Service (Port 8086)<br/>Fan-Out Timeline"]
    Gateway -->|/api/v1/chats/**| Chat

    Auth -->|JDBC| AuthDB[("auth_db (MySQL)")]
    User -->|JDBC| UserDB[("user_db (MySQL)")]
    Post -->|JDBC| PostDB[("post_db (MySQL)")]
    Notif -->|JDBC| NotifDB[("notification_db (MySQL)")]
    Feed -->|JDBC| FeedDB[("feed_db (MySQL)")]
    Chat -->|Mongo Driver| ChatDB[("pulse_chat_db (MongoDB)")]

    Chat <-->|Presence & Active Chat TTL| RedisStore[("Redis (Presence Store)")]
    Notif <-->|Anti-Flood Throttling| RedisStore

    Post -->|Outbox Events| OutboxTable[("Outbox Table")]
    OutboxTable -->|Scheduled Poller| Kafka(("Apache Kafka"))
    Chat -->|Message Sent Events| Kafka

    Kafka -->|post-events| Feed
    Kafka -->|post-events & chat-events| Notif

    Notif -->|REST API Trigger| NovuEngine["Novu Engine<br/>In-App, Push & Email"]
    Post -.->|Media Uploads| S3[("AWS S3 Storage")]
```

---

## 🚀 Microservices Breakdown

| Service | Port | Database / Storage | Primary Responsibilities |
|---|---|---|---|
| **`api-gateway`** | `8080` | — | Central entry point, Reactive WebFlux Security, JWT signature validation (HMAC-SHA384), path-based reverse proxy routing. |
| **`auth-service`** | `8081` | `auth_db` (MySQL) | Email/password registration & login, GitHub OAuth2 social authentication, JWT token issuance, Refresh Token rotation, RBAC (`USER`, `ORGANIZER`, `ADMIN`), and organizer role elevation. |
| **`user-service`** | `8083` | `user_db` (MySQL) | Developer/student profiles, organizer profiles, bidirectional user blocking, follow graph, and message privacy settings (`EVERYONE`, `FOLLOWERS_ONLY`). |
| **`post-service`** | `8084` | `post_db` (MySQL) | Social posts, comments, likes, AWS S3 media uploads, Hackathon hosting & lifecycle, team/solo registrations with capacity capping, and Transactional Outbox publishing. |
| **`notification-service`** | `8085` | `notification_db` (MySQL) | Event-driven notification engine for post likes and chat alerts, DND quiet hour evaluation, Redis anti-flood throttling, and multi-channel dispatch via Novu. |
| **`feed-service`** | `8086` | `feed_db` (MySQL) | Fan-out on write timeline architecture. Consumes Kafka post events to construct and serve personalized feeds. |
| **`chat-service`** | `8087` | `pulse_chat_db` (MongoDB) + Redis | 1-on-1, group, and hackathon team chat with STOMP WebSockets, conversation muting, and real-time active room presence tracking via Redis. |

---

## ✨ Key Features

### 1. Authentication & Security
- **Stateless JWT with HMAC-SHA384**: Embeds user UUID, email, name, and role directly into signed claims.
- **Refresh Token Lifecycle**: Persistent, revokable refresh tokens with sliding expiration.
- **GitHub OAuth2 Integration**: Seamless one-click login for developers.
- **Role-Based Access Control**: Strict separation between `USER`, `ORGANIZER`, and `ADMIN`.

### 2. Hackathons & Organizer Ecosystem
- **Organizer Profiles**: Clubs, universities, and tech organizations can host and manage hackathons.
- **Event Lifecycle**: Configurable modes (`ONLINE`, `IN_PERSON`, `HYBRID`), team bounds (`minTeamSize`, `maxTeamSize`), and registration deadlines.
- **Team Registration & Capacity Enforcement**:
  - Case-insensitive team name grouping with automatic rejection when a team exceeds `maxTeamSize`.
  - Duplicate registration protection per user per hackathon.
  - Team roster lookups and auto-provisioned hackathon team chats.
- **Participant Status Management**: Organizers can mark participants as `SHORTLISTED`, `PARTICIPATED`, `WINNER`, or `RUNNER_UP`.

### 3. Real-Time Chat & Presence System
- **STOMP WebSockets**: Sub-millisecond bi-directional messaging with delivered and read receipts.
- **Active Room Presence Detection**: Redis-backed active chat tracking (`presence:user:{id}:active_chat`) automatically suppresses notifications when the recipient is actively in the conversation screen.
- **Conversation Muting**: Granular muting per conversation (e.g., 8 hours, 1 week, or indefinitely).
- **User Blocking & Privacy Enforcement**: Prevents conversation creation or adding members if either party has blocked the other.

### 4. Multi-Channel Notification Engine (Novu)
- **Context-Aware Delivery**:
  - User in chat screen $\rightarrow$ Suppressed.
  - User elsewhere in app $\rightarrow$ In-App Toast notification.
  - User offline / locked $\rightarrow$ Mobile Push (FCM / APNs) via Novu.
- **Do Not Disturb (DND)**: User-configurable quiet hours (e.g., 10:00 PM – 7:00 AM) that suppress notifications while preserving unread history.
- **Anti-Flood Throttling**: Atomic Redis rate-limiting (5-second window) prevents push notification spam during rapid message exchanges.
- **Device Push Token Management**: Native endpoints for mobile clients to register FCM/APNs push credentials.

### 5. Distributed Resilience
- **Transactional Outbox Pattern**: Database writes and event creation commit in the exact same transaction, eliminating dual-write inconsistencies.
- **Kafka Event Streaming**: Fully decoupled asynchronous message dispatch between chat, posts, feed generation, and notification delivery.

---

## 🛠 Technology Stack

- **Language / Runtime:** Java 25
- **Frameworks:** Spring Boot 3.4.1, Spring Framework 6.2.15, Spring WebSocket (STOMP)
- **Gateway & Security:** Spring Cloud Gateway (Reactive), Spring Security 6, Nimbus JWT
- **Databases & Persistence:** 
  - Spring Data JPA, Hibernate ORM, MySQL 8.0
  - Spring Data MongoDB (Chat & Messages)
  - Spring Data Redis (Presence, Active Rooms, Anti-Flood Throttling)
- **Message Broker:** Apache Kafka 3.x
- **Notification Infrastructure:** Novu (In-App Center, Web Push, Mobile FCM/APNs)
- **Cloud Storage:** AWS SDK for Java 2.x (Amazon S3)
- **Utilities:** Project Lombok (1.18.48), Jackson (JSR310 Date Module)

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.