# AgroPiura IoT

Plataforma web para monitorear cultivos de pequeños y medianos agricultores de Piura.
Nodos sensores ESP32 miden humedad de suelo, humedad ambiente, temperatura y pH; la
aplicación compara cada lectura con los umbrales configurados por cultivo, genera alertas
y notifica a las personas con acceso a ese cultivo.

Proyecto académico (UTP) desarrollado con Java 25, Spring Boot 4.1 y PostgreSQL.

## Inicio rápido

```powershell
# 1. Base de datos (una sola vez)
createdb -h localhost -U postgres agropiura
psql -h localhost -U postgres -d agropiura -f database/esquema.sql

# 2. Configuración local (una sola vez)
copy src\main\resources\application.properties.example src\main\resources\application.properties
$env:DB_PASSWORD = "tu_clave_de_postgres"

# 3. Ejecutar
.\mvnw.cmd spring-boot:run
```

Abrir <http://localhost:8080> e ingresar con `admin@agropiura.com` / `AgroAdmin2026`
(administrador de demostración, solo para entorno local). Las demás cuentas de prueba
están en [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md).

La guía completa, con requisitos y solución de problemas, está en
[01-instalacion-y-ejecucion.md](01-instalacion-y-ejecucion.md).

## Documentación

| Documento | Contenido |
|---|---|
| [01 · Instalación y ejecución](01-instalacion-y-ejecucion.md) | Requisitos, base de datos, variables de entorno, ejecución, problemas frecuentes |
| [02 · Arquitectura](02-arquitectura.md) | Capas, paquetes, flujo de una lectura, decisiones técnicas |
| [03 · Funcionalidades y rutas](03-funcionalidades-y-rutas.md) | Requerimientos RF-01 a RF-08, pantallas y tabla de rutas |
| [04 · Base de datos](04-base-de-datos.md) | Tablas, relaciones, borrados en cascada, índices y scripts |
| [05 · Seguridad y permisos](05-seguridad-y-permisos.md) | Login, roles por cultivo, CSRF, manejo de errores |
| [06 · Diseño de interfaz](06-diseno-de-interfaz.md) | Sistema visual, componentes, accesibilidad y uso en celular |
| [07 · Pruebas y verificación](07-pruebas-y-verificacion.md) | Pruebas automáticas y lista de verificación manual |
| [08 · Registro de cambios](08-registro-de-cambios.md) | Historial detallado de lo que se ha hecho en el proyecto |
| [09 · Reglas del proyecto](09-reglas-del-proyecto.md) | Reglas obligatorias para colaborar y para asistentes de código |
| [10 · Usuarios de prueba](10-usuarios-de-prueba.md) | Cuentas de prueba de la BD local, contraseñas y para qué sirve cada una |
| [11 · Plan de sprints](11-plan-de-sprints.md) | Frentes de trabajo, tareas por sprint y criterios para cerrar el proyecto |
| [13 · Reglas agronómicas](13-reglas-agronomicas.md) | Umbrales por cultivo con fuentes, sensor dañado, severidad, episodios, recomendaciones y validación con escenarios |

## Stack

- **Backend:** Java 25, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation), Hibernate
- **Vistas:** Thymeleaf renderizado en servidor, CSS y JavaScript planos (sin build de frontend)
- **Base de datos:** PostgreSQL 15 o superior (probado con 18)
- **Librerías:** Apache POI (Excel), Guava (caché), Apache Commons Lang3, Logback
- **Frontend:** Chart.js 4 (local), Leaflet 1.9 + OpenStreetMap (mapa), Google Fonts
- **Integración externa:** OpenWeatherMap (clima, opcional)
- **Build y pruebas:** Maven Wrapper, JUnit 5, MockMvc

## Estructura del repositorio

```
Proyecto_AgroPiuraIoT/
├── database/
│   ├── esquema.sql          # Esquema completo para una BD nueva
│   └── indices.sql          # Índices y restricciones para BD ya existentes
├── docs/                    # Esta documentación
├── src/main/java/com/utp/AgroPiuraIoT/
│   ├── config/              # Seguridad, datos iniciales, atributos globales de vistas
│   ├── controller/          # Controladores web (una pantalla o acción por método)
│   ├── exception/           # Errores de dominio (404 y 400)
│   ├── integration/clima/   # Cliente de OpenWeatherMap
│   ├── model/dto/           # Formularios y objetos de vista
│   ├── model/entity/        # 11 entidades JPA (ver docs/04)
│   ├── port/                # Interfaces hacia servicios externos
│   ├── repository/          # Acceso a datos (Spring Data JPA)
│   ├── security/            # Carga de usuarios y permisos por cultivo
│   ├── service/             # Lógica de negocio
│   └── util/excel/          # Exportadores a Excel
├── src/main/resources/
│   ├── static/              # css/app.css, js/app.js, js/chart.umd.min.js, img/favicon.svg
│   ├── templates/           # Vistas Thymeleaf (components/ tiene layout e iconos)
│   ├── application.properties.example
│   └── logback-spring.xml
└── src/test/java/...        # 56 pruebas automáticas
```

## Equipo

Repositorio: <https://github.com/KevinDC65/Proyecto_AgroPiuraIoT>
