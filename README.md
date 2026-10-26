# AgroPiura IoT

Plataforma web que monitorea cultivos de pequeños y medianos agricultores de Piura:
nodos sensores ESP32 con humedad de suelo y ambiente, temperatura, motor de alertas
por umbrales configurables y una interfaz web sencilla para el agricultor.

## Stack

- Java 25 + Spring Boot 4.1.1 (MVC, Thymeleaf, Security, Data JPA)
- PostgreSQL (esquema según DDL del informe)
- Maven Wrapper (mvnw)
- Apache POI (Excel), Guava (cache), Apache Commons Lang3, Logback
- Chart.js (gráfica del histórico)

## Arquitectura (MVC + DAO)

- `controller/` — Web MVC (Portada, Dashboard, Cultivos, Umbrales, Alertas, Histórico, Auth)
- `service/` — Lógica de negocio (Cultivo, Umbral, Alertas, Histórico, Usuario)
- `repository/` — DAO con Spring Data JPA (13 repositorios según modelo físico)
- `model/entity` (`cultivo`, `umbrales`, `lecturas`, `alertas`, …)
- `util/excel/` — Exportadores Apache POI

## Funcionalidades (SRS)

| RF  | Descripción                                       | Estado |
|-----|--------------------------------------------------|--------|
| RF-01 | Registro/login con BCrypt                       | ✔ |
| RF-02 | CRUD de cultivos + dispositivos                 | ✔ |
| RF-03 | Umbrales por cultivo y variable                 | ✔ |
| RF-04 | Dashboard resumen en tiempo real                | ✔ |
| RF-05 | Motor de alertas + notificaciones               | ✔ |
| RF-06 | Clima de terceros                               | pendiente |
| RF-07 | Roles admin/colaborador parcial (entidad+seguridad) | parcial |
| RF-08 | Histórico por cultivo, gráfica y Excel          | ✔ |

## Cómo ejecutar

1. PostgreSQL local con BD `agropiura` (esquema vía `database/query.sql` del informe).
2. Copiar `src/main/resources/application.properties.example` a
   `src/main/resources/application.properties` y ajustar usuario/contraseña (el
   archivo real está fuera del repo por *.properties en .gitignore).
3. `.\mvnw.cmd spring-boot:run` → http://localhost:8080

## Pruebas (TDD)

`.\mvnw.cmd test` — 22 pruebas de servicio (usuarios, cultivos, umbrales sin
duplicar, motor de alertas, histórico con rango de fechas, export Excel).

## Javadoc

`.\mvnw.cmd javadoc:javadoc` genera la "apidocs" en `target/site/apidocs`.
