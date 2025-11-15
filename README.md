![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge)
![SpringBoot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgresSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)

# Electronic Cheque Payment System
Peer-to-Peer / Client-Server Payment Platform (Spring Boot + PostgreSQL)

This project implements a secure, peer-to-peer electronic cheque payment system designed for mobile and web use

The system supports cheque issuance, transfer (QR / Email), clearing, settlement, encryption, digital signatures, and full audit logging

## Tech Stack

### Backend (Java)
- Spring Boot 3
- Spring Web (REST API)
- Spring Security (JWT authentication)
- Spring Data JPA + Hibernate
- PostgreSQL
- Lombok
- Java Cryptography (AES-GCM, RSA-OAEP, SHA-256)
- Spring AOP (Audit Logging)

### Infrastructure
- PostgreSQL (local or cloud)
- Optional: ELK Stack (ElasticSearch + Logstash + Kibana)

## System Architecture Overview

```
 ┌──────────────────┐       ┌───────────────────────┐
 │    Frontend      │ <---> │   Spring Boot API     │
 │   Javascript     │       │  Authentication       │
 └──────────────────┘       │  Cheque Management    │
                            │  Transfer (QR/Email)  │
                            │  Clearing & Settlement│
                            │  Encryption Module    │
                            │  Audit Logging        │
                            └──────────┬────────────┘
                                       │
                            ┌──────────▼───────────┐
                            │     PostgreSQL       │
                            │ Users / Cheques /    │
                            │ Accounts / AuditLogs │
                            └──────────────────────┘
```

## Core Features

### 1. Authentication & User Management Module
- Register (username, email, real name, phone number)
- Login (JWT)
- Profile management
- Logout
- Validation rules (name <25 chars, phone <15 digits)

### 2. Cheque Management Module
- Issue cheque (with AES + RSA encryption + signature)
- Query cheque by ID or status
- Update cheque status (CLEARED / VOIDED / EXPIRED / SPLIT)
- Fragment cheque into smaller child cheques
    - Child cheques cannot be split again
    - Split amount total must equal parent cheque amount

### 3. Cheque Transfer Module
Supports three delivery methods:
- Email Transfer (Spring Mail + SMTP provider, e.g., SendGrid / Outlook)
- QR Code Transfer (Generates Base64 PNG encoded QR containing encrypted cheque payload)

### 4. Security & Encryption Module
- **AES-GCM (Symmetric Encryption)**
    - Used to encrypt cheque payloads
    - GCM provides built-in tamper detection and integrity via authenticated encryption
    - Each cheque payload includes : `amount, payerRealName, payerUsername, payeeUsername, expiryDate, nonce`

- **RSA-2048 (Asymmetric Encryption)**
    - AES key is encrypted using RSA-OAEP with SHA-256 padding
    - Only the server holding the RSA private key can decrypt the AES key
    - Provides confidentiality even if the cheque is transmitted via Email and QR Code

- **Digital Signature (SHA-256 + RSA)**
    - Each cheque is signed using the RSA private key : `Sign( SHA-256(chequeData) )`
    - During clearing, the system performs :
        1. Decrypt AES key → decrypt cheque payload
        2. Re-build the original cheque JSON
        3. Verify signature using RSA public key
    - If either signature or AES integrity tag fails → the cheque is rejected

- **JWT Authentication**  
  Used for login, profile fetching, and access control for cheque APIs

- **Password Hashing (BCrypt)**  
  All passwords hashed using BCrypt with salt and cost factor

- **Replay-Attack Protection**  
  Each cheque contains a unique nonce:
    - Stored inside encrypted cheque payload
    - Saved to database only after settlement
    - Checked during verification

### 5. Clearing & Settlement Module
- Verify encryption
- Decrypt AES key using RSA private key
- Decrypt cheque data
- Verify signature
- Check expiry date and replay nonce
- Settle → update account balance and mark nonce as used
- Scheduled batch settlement (Spring Scheduler)

### 6. Audit & Logging Module
- Spring AOP intercepts all controller calls
- Logs success / failure / security events
- Sensitive data (passwords, emails, phone numbers) masked
- Logs stored in DB + Logback file
- Supports ELK integration for visualization

## Project Structure (Backend)

```
src/main/java/com/chequepay
 ├── audit/         # Audit logs, AOP aspects, masking utilities
 ├── config/        # Spring Security, JWT config, CORS, App configs
 ├── controller/    # REST controllers (Auth, Cheque, Transfer, Clearing)
 ├── dto/           # Request & Response DTOs
 ├── entity/        # JPA entities (User, Cheque, Account, AuditLog)
 ├── repository/    # Spring Data JPA Repositories
 ├── service/       # Core business logic (Auth, Cheque, Transfer, Clearing)
 ├── util/          # Crypto utilities (AES, RSA), signatures, nonce, etc.
 └── ChequePayApplication.java
```

## API Examples

### User APIs
- `POST /auth/register`
- `POST /auth/login`
- `GET /auth/profile`
- `POST /auth/logout`
- `GET /account/balance`

### Cheque APIs
- `POST /cheques`
- `GET /cheques/{id}`
- `PATCH /cheques/{id}/status`
- `POST /cheques/{id}/split`

### Transfer APIs
- `POST /transfer/email?chequeId={id}`
- `POST /transfer/qr?chequeId={id}`

### Clearing APIs
- `POST /clearing/verify?chequeId={id}`
- `POST /clearing/settle?chequeId={id}`

## Running the Project

1. Configure `application.yaml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cheque-pay
    username: postgres
    password: yourpassword
```

2. Start Backend
```bash
mvn spring-boot:run
```

3. Start Frontend
```bash
npm install
npm run dev
```

## Future Enhancements
- Multi-signature cheques
- Blockchain-based cheque lineage
- Push notification on cheque received
- QR offline verification

## Purpose of This Project

This system is designed for academic research, demonstrating:
- Secure P2P payment architecture
- Cryptographic cheque verification
- Anti-tampering & anti-replay mechanisms
- Digital settlement workflow
- Best-practice logging & system observability