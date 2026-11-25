# 07 · Pruebas y verificación

## Pruebas automáticas

```powershell
.\mvnw.cmd test                              # todas
.\mvnw.cmd test -Dtest=CultivoServiceTest    # una clase
```

**129 pruebas** en `src/test/java/com/utp/AgroPiuraIoT/`: 112 activas, 12 de telemetría
desactivadas hasta el issue A2 y 5 de validación agronómica desactivadas hasta B1, B2 y A2. Usan la BD local configurada;
cada prueba está anotada con `@Transactional` y se revierte al terminar, así que no deja
datos. Requieren PostgreSQL encendido con el esquema creado.

| Clase | Pruebas | Qué cubre |
|---|:---:|---|
| `AutorizacionWebTest` | 13 | Recorre las plantillas reales con MockMvc: login obligatorio, cada usuario solo ve sus cultivos, cultivo ajeno o inexistente → 403, colaborador ve pero no administra, colaborador puede simular, admin elimina, POST sin CSRF rechazado, umbral invertido muestra error, umbral de otro cultivo → 404, histórico con rango invertido, dashboard/mapa/alertas renderizan, el creador queda como admin |
| `AccesoServiceTest` | 7 | Asignar por correo y rol, reasignar sin duplicar, quitar, correo o rol inexistente, nunca quitar ni degradar al único administrador |
| `CultivoServiceTest` | 8 | CRUD, detección de cultivos sin administrador, nodos con un sensor por tipo, el creador es administrador, al eliminar se borran los nodos exclusivos y se conservan los compartidos, errores de nodo inexistente o repetido |
| `AlertasServiceTest` | 6 | Alerta y notificación fuera de rango, nada dentro de rango ni sin umbral, sin notificaciones duplicadas, marcar leída, contador |
| `HistoricoServiceTest` | 6 | Rango de fechas, sin fechas, un solo extremo, rango invertido, Excel, resumen |
| `UmbralServiceTest` | 5 | Guardar, actualizar sin duplicar, rango inválido, cultivo inexistente, eliminar |
| `ClimaServiceTest` | 4 | Clima por ubicación, caché, sin ubicación, proveedor sin datos (usa un cliente falso, sin red) |
| `UsuarioServiceTest` | 8 | Clave cifrada, correo duplicado, claves distintas |
| `CultivoExcelExporterTest` | 2 | Cabecera y filas, lista vacía |
| `DataSeederTest` | 2 | Los usuarios demo existen y autentican con su rol |
| `AgroPiuraIoTApplicationTests` | 1 | El contexto de Spring arranca |
| `ViewControllerTest` | 14 | Cada pantalla renderiza su plantilla con el modelo esperado |
| `CultivoControllerTest` | 12 | Formularios de cultivo, mensajes, nodos y exportación |
| `SecurityControllerTest` | 8 | Rutas públicas y protegidas, login, logout, registro y API de telemetría cerrada |
| `UmbralControllerTest` | 6 | Lista, guardado, errores y eliminación de umbrales |
| `AppUserDetailsServiceTest` | 6 | Carga de usuarios para el login y sus roles |
| `TelemetriaControllerTest` | 12 | API de telemetría. **Desactivada** (`@Disabled`) hasta autenticar nodos con token (issue A2) |
| `ValidacionAgronomicaTest` | 9 | Escenarios de B3 sobre un mango con la plantilla de B2: sequía, exceso de riego, ola de calor (4 activas). **Desactivadas** hasta B1, B2 y A2: sensor dañado, nodo caído, episodios, severidad y recomendaciones (5). Ver [13-reglas-agronomicas.md](13-reglas-agronomicas.md) |

### Escribir pruebas nuevas

- Un servicio nuevo → `XxxServiceTest` con `@SpringBootTest` y `@Transactional`.
- Una regla de permisos nueva → caso en `AutorizacionWebTest` con
  `.with(user("correo"))` (y `.with(csrf())` en los POST), usando usuarios creados en la
  propia prueba.
- Un bug corregido → primero una prueba que falle reproduciéndolo, después la corrección.
- Nombres en español que describan el comportamiento: `noPermiteQuitarAlUnicoAdministrador`.

## Verificación manual antes de entregar

Con la aplicación corriendo y la BD local:

**Instalación limpia**
- [ ] BD vacía + `database/esquema.sql` + arrancar: el log muestra los catálogos sembrados
      y la app responde en `/login`.

**Usuario nuevo**
- [ ] Registro con datos inválidos muestra errores por campo y no crea la cuenta.
- [ ] Registro válido → login → dashboard vacío con mensaje de qué hacer.
- [ ] Login con clave incorrecta muestra aviso.

**Cultivo completo**
- [ ] Crear cultivo con coordenadas → aparece en cultivos, dashboard y mapa.
- [ ] Registrar nodo → el detalle lo lista.
- [ ] Umbral con mínimo > máximo muestra error; umbral válido se guarda.
- [ ] Simular lectura varias veces → aparecen alertas y sube el contador del menú.
- [ ] Atender alerta y marcar notificación como leída.
- [ ] Asignar acceso a otro usuario (y a un correo inexistente: error claro).
- [ ] Histórico con gráfica; filtros con solo "desde", solo "hasta" y rango invertido.
- [ ] Exportar cultivos e histórico a Excel y abrir los archivos.
- [ ] Eliminar el cultivo (pide confirmación).

**Seguridad**
- [ ] Con otro usuario: el cultivo anterior no aparece y su URL da 403.
- [ ] Colaborador: ve el cultivo pero no tiene botones de editar, eliminar ni umbrales.
- [ ] Sin sesión, cualquier página interna redirige a login.
- [ ] Una ruta inexistente muestra la página 404.

**Interfaz**
- [ ] Todas las páginas a 1440px y 390px de ancho, sin desplazamiento horizontal.
- [ ] Menú móvil abre y cierra (también con Escape).
- [ ] Navegación con teclado: Tab llega primero a "Saltar al contenido".
- [ ] Consola del navegador sin errores de JavaScript.

## Última verificación completa (17/11/2026)

- `mvnw clean test`: 108 en verde y 12 desactivadas a propósito (telemetría), con JDK 25.
- Instalación limpia en una BD temporal con `esquema.sql`: arranca, valida el esquema y
  siembra catálogos y administrador demo.
- Recorrido completo en navegador con un usuario de prueba (registro, cultivo, nodo,
  umbrales, simulación, alertas, accesos, histórico, exportaciones, aislamiento, CSRF,
  eliminación, cierre de sesión): todo correcto. Los datos de prueba se borraron después.
- Administrador demo: 12 páginas internas y 3 públicas responden 200 a 1440px y 390px sin
  desplazamiento horizontal ni errores de JavaScript.
