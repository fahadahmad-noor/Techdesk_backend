# TechDesk Backend Services

## Overview
TechDesk is a production-grade, multi-tenant IT Service Management (ITSM) platform. The backend is designed as a microservices architecture utilizing Spring Boot 3.x and Java 17. It provides fully isolated PostgreSQL schema-level data separation for each tenant (company), enabling secure processing of employee IT support tickets, gadget procurement, asset tracking, Role-Based Access Control (RBAC), and SLA enforcement.

## Architecture
The platform is composed of 13 independent microservices communicating via a centralized Spring Cloud API Gateway and orchestrated using Docker Compose.

### Core Infrastructure Components
* **API Gateway**: Provides dynamic routing and tenant context extraction (via subdomain mapping or JWT parsing) while enforcing rate limiting and edge security.
* **Eureka Server**: Facilitates dynamic service discovery and client-side load balancing.
* **Database**: PostgreSQL 15 deployed with schema-per-tenant isolation.
* **Migrations**: Automated schema versioning via Flyway, segregating public platform tables from isolated tenant tables.

### Tech Stack
* **Language**: Java 17
* **Framework**: Spring Boot 3.2.5 / Spring Cloud 2023.0.1
* **Database**: PostgreSQL 15
* **Authentication**: JWT with asymmetric signing and refresh token rotation
* **Containerization**: Docker & Docker Compose
* **Testing**: JUnit 5, Mockito

## Local Development Setup

### Prerequisites
* JDK 17
* Maven 3.8+
* Docker Desktop

### Environment Configuration
Copy the `.env.example` file to `.env` in the root directory and populate the required database and secret values. Note: As per security guidelines, the `src/main/resources` folder is excluded from version control. You must provide your own `application.yml` configurations locally.

### Running the Application

1. **Build all microservices:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Spin up the infrastructure:**
   ```bash
   docker-compose up -d --build
   ```

3. **Verify the services:**
   * Eureka Registry: http://localhost:8761
   * API Gateway: http://localhost:8080
   * PostgreSQL Admin: http://localhost:5050

## Migration Management
Database schemas are managed automatically on startup. The `tenant-service` executes the global `public` schema migrations. Tenant-specific schemas are generated dynamically and populated upon new company registration.

## License
Confidential - Adept Tech Solutions | Internal Use Only
