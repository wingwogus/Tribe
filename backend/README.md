🚀 Spring Boot Kotlin Initial Template

A production-grade multi-module Spring Boot 3.x + Kotlin starter template.
This project includes the essential building blocks for real-world backend services such as:

* API Response standardization
* Global exception handling
* JWT authentication
* Module separation (api / application / domain / batch)
* Logging with MDC
* JPA configuration
* Swagger UI
* Environment-specific profiles

You can run this project immediately, then customize the package name and DB settings to fit your service.


🔧 Required Customization Before Use

This template is prepared for public sharing.
If you plan to use it for your own service, you must update the following items.


1️⃣ Change Base Package (com.tribe → your domain)
All modules (api / application / domain / batch) use package com.tribe.
Rename it to your organization or project domain.

  IntelliJ shortcut:
Right-click package → Refactor → Rename (Shift + F6)
Imports will update automatically.

2️⃣ Configure Your Own Database
The template uses H2 for easy execution.
Replace it with your actual DB (MySQL/PostgreSQL/etc)

src/main/resources/application-local.yml:
spring:
  datasource:
    url: jdbc:h2:mem:tribe-local;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

Local profile now boots against an embedded H2 database by default.
Override `SPRING_DATASOURCE_*` only when you want to point local at PostgreSQL instead.
Local also uses `spring.jpa.hibernate.ddl-auto=create` by default, so the schema is recreated from entities on every startup.

3️⃣ Replace JWT Secret Key
A sample secret is included.
Generate a new one:

openssl rand -hex 32

Set it inside your application-local.yml / application-dev.yml.

4️⃣ Update Swagger Info
Modify SwaggerConfig.kt to match your project branding:
.info(
  Info()
    .title("Your API")
    .description("Your project description")
)

5️⃣ Redis (Optional)

Redis dependency is included.
If your service doesn’t use Redis, simply remove:

implementation("org.springframework.boot:spring-boot-starter-data-redis")


▶ How to Run
⭐ Recommended: IntelliJ Run Button
Runs with the application-local profile by default.

⭐ Official Method: Gradle bootRun
./gradlew :api:bootRun
