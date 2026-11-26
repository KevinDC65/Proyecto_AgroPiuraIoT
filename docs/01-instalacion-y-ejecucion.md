# 01 · Instalación y ejecución

## Requisitos

| Herramienta | Versión | Comprobación |
|---|---|---|
| JDK | **25** (el proyecto no compila con 17 ni 21) | `java -version` |
| PostgreSQL | 15 o superior (probado con 18) | `psql --version` |
| Git | cualquiera reciente | `git --version` |
| Maven | no hace falta instalarlo: se usa el wrapper `mvnw` | — |

### JDK 25 cuando hay otras versiones instaladas

Maven usa el JDK al que apunta `JAVA_HOME`. Si en el equipo hay varias versiones:

- **Solo para la terminal actual** (no afecta a otros proyectos):
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.4.1"
  $env:Path = "$env:JAVA_HOME\bin;$env:Path"
  ```
- **De forma permanente:** Panel de control → Sistema → Variables de entorno → definir
  `JAVA_HOME` con la ruta del JDK 25 y poner `%JAVA_HOME%\bin` (o la ruta completa) antes
  de cualquier otra entrada de Java en `Path`. No desinstalar los otros JDK: un proyecto que
  necesite otra versión puede usar su propio `JAVA_HOME` o configurarla en el IDE.
- **IntelliJ IDEA:** File → Project Structure → SDK = 25.
- **VS Code:** la extensión de Java toma `JAVA_HOME` o `java.jdt.ls.java.home`.

Después de cambiar variables de entorno hay que cerrar y abrir de nuevo la terminal y el IDE.

## 1. Crear la base de datos

```powershell
createdb -h localhost -U postgres agropiura
psql -h localhost -U postgres -d agropiura -f database/esquema.sql
```

`database/esquema.sql` crea las 13 tablas con sus claves, relaciones e índices. Los
catálogos (`roles`, `tipo_sensor`) y el administrador de demostración **no** van en el
script: los crea la aplicación al arrancar (`config/DataSeeder`).

**Si ya tenías la BD creada con el DDL anterior del informe**, no la borres: ejecuta solo
los índices y restricciones nuevos (el script se puede ejecutar varias veces):

```powershell
psql -h localhost -U postgres -d agropiura -f database/indices.sql
```

Y si esa BD tiene cultivos creados antes de los permisos por cultivo (al ingresar no ves
tus cultivos, o el log avisa de cultivos sin administrador), asígnalos a tu usuario:

```powershell
psql -h localhost -U postgres -d agropiura -v correo=tu@correo.com -f database/migracion-datos-anteriores.sql
```

Si tu BD ya existía, aplica también las **migraciones** de `database/migraciones/` que aún
no tengas, en orden de fecha (todas se pueden ejecutar varias veces):

```powershell
psql -h localhost -U postgres -d agropiura -f database/migraciones/2026-11-25_A1_token_y_conexion_nodo.sql
psql -h localhost -U postgres -d agropiura -f database/migraciones/2026-11-26_A3_lecturas_sin_duplicados.sql
```

Detalle de tablas y scripts en [04-base-de-datos.md](04-base-de-datos.md).

## 2. Configuración local

```powershell
copy src\main\resources\application.properties.example src\main\resources\application.properties
```

`application.properties` está en `.gitignore` (regla `*.properties`): **nunca** se sube al
repositorio. Las credenciales se leen de variables de entorno:

| Variable | Obligatoria | Por defecto | Uso |
|---|---|---|---|
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/agropiura` | Conexión JDBC |
| `DB_USER` | no | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | **sí** | vacío | Clave de PostgreSQL |
| `OPENWEATHER_API_KEY` | no | vacío | Clima en el dashboard (RF-06). Sin ella el panel de clima se oculta y el resto funciona |
| `ADMIN_DEMO_CLAVE` | fuera de local, sí | `AgroAdmin2026` | Clave del administrador de demostración |

Otras propiedades útiles (en `application.properties`):

| Propiedad | Valor por defecto | Para qué |
|---|---|---|
| `agro.demo.admin.habilitado` | `true` | `false` desactiva el administrador de demostración |
| `clima.cache.minutos` | `30` | Minutos que se guarda en caché el clima de cada ubicación |
| `clima.openweather.url` | API 2.5 oficial | Cambiar el proveedor o usar un simulador |
| `spring.jpa.hibernate.ddl-auto` | `validate` | **No cambiar**: Hibernate solo comprueba el esquema, nunca lo modifica |

La clave gratuita de OpenWeatherMap se obtiene en <https://openweathermap.org/api>.

## 3. Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

O compilar y ejecutar el JAR:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\AgroPiuraIoT-0.0.1-SNAPSHOT.jar
```

La aplicación queda en <http://localhost:8080>. Para otro puerto: `--server.port=8081`.

Al arrancar por primera vez, el log muestra:

```
Catálogo roles sembrado: administrador, colaborador
Catálogo tipo_sensor sembrado con 4 tipos
Usuario administrador de demostración sembrado: admin@agropiura.com
```

y una advertencia si el administrador demo usa la clave pública por defecto.

Los logs se guardan también en `logs/agropiura.log` (carpeta ignorada por git).

## 4. Primer recorrido

1. Ingresar con `admin@agropiura.com` / `AgroAdmin2026`.
2. **Cultivos → Nuevo cultivo.** Con latitud y longitud aparece en el mapa.
3. En el detalle del cultivo, **Registrar nodo nuevo**: el nodo nace con un sensor por cada
   tipo del catálogo.
4. **Umbrales:** definir mínimo y máximo de cada variable.
5. **Simular lectura** en el detalle del cultivo: genera una lectura por sensor, como si
   llegara de un ESP32. Alrededor del 20 % sale fuera de rango para generar alertas.
6. Revisar **Dashboard**, **Alertas**, **Histórico** (con exportación a Excel) y **Mapa**.

## 5. Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas usan la BD local configurada; cada prueba corre en una transacción que se
revierte, así que no dejan datos. Detalle en [07-pruebas-y-verificacion.md](07-pruebas-y-verificacion.md).

## 6. Javadoc

```powershell
.\mvnw.cmd javadoc:javadoc
```

Se genera en `target/site/apidocs`.

## Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `release version 25 not supported` o `invalid target release: 25` | Maven está usando un JDK anterior | Apuntar `JAVA_HOME` al JDK 25 (ver arriba) |
| `password authentication failed for user "postgres"` | Falta `DB_PASSWORD` o es incorrecta | Definir la variable de entorno y reiniciar la terminal |
| `Schema-validation: missing table [...]` | La BD no tiene el esquema | Ejecutar `database/esquema.sql` |
| `Schema-validation: missing column [...]` | La BD es anterior a una migración | Ejecutar los scripts pendientes de `database/migraciones/` (ver arriba) |
| `Schema-validation: wrong column type` | El esquema de la BD no coincide con las entidades | Comparar con `database/esquema.sql`; no cambiar `ddl-auto` |
| `Port 8080 was already in use` | Otra instancia corriendo | Cerrarla o usar `--server.port=8081` |
| El panel de clima no aparece | Sin `OPENWEATHER_API_KEY` o el cultivo no tiene ubicación | Es el comportamiento esperado |
| El mapa muestra un aviso en lugar del mapa | Sin internet: Leaflet y los mapas se cargan de internet | Conectarse a internet |
| Un cultivo no aparece en el mapa | No tiene latitud y longitud | Editar el cultivo y completarlas |
| "Simular lectura" avisa que no hay nodos | El cultivo no tiene nodos con sensores | Registrar un nodo en el detalle del cultivo, o ejecutar `migracion-datos-anteriores.sql` si son nodos antiguos |
| Al ingresar no aparece ningún cultivo, aunque la BD tiene datos | Los cultivos no tienen administrador (datos anteriores a los permisos por cultivo) | `database/migracion-datos-anteriores.sql` con tu correo |
| No sé con qué cuenta entrar | — | Ver [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md) |
