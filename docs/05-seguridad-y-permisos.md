# 05 · Seguridad y permisos

## Autenticación

- Login por formulario con Spring Security (`config/SecurityConfig`). El correo es el
  nombre de usuario.
- Contraseñas guardadas con **BCrypt**; nunca en texto plano ni en logs.
- **Política de contraseñas (C1):** en el registro (`RegistroRequest` y `UsuarioService`) se
  exigen entre 8 y 72 caracteres combinando al menos una letra y un número. Las cuentas
  existentes previas siguen pudiendo iniciar sesión.
- **Estado de cuenta y bloqueo temporal (C1):** `AppUserDetailsService` marca como
  deshabilitadas las cuentas con `estado != 'activo'` y como bloqueadas las que tienen
  `bloqueado_hasta` vigente. `EventosAutenticacionListener` escucha los eventos de
  autenticación de Spring Security: 5 intentos fallidos bloquean la cuenta por 15 minutos
  (configurables vía `agro.seguridad.max-intentos-fallidos` y `agro.seguridad.bloqueo-minutos`);
  un ingreso correcto reinicia el contador.
- **No enumeración de usuarios:** todos los fallos de login (correo inexistente, clave
  incorrecta, cuenta inactiva o bloqueada) redirigen a `/login?error` con el mismo mensaje
  genérico.
- **Auditoría:** los inicios de sesión exitosos (`LOGIN_EXITOSO`), fallos (`LOGIN_FALLIDO`) y
  bloqueos (`CUENTA_BLOQUEADA`) se registran en la tabla `auditoria` vía `AuditoriaService`.
- Rutas públicas: `/`, `/login`, `/registro`, `/css/**`, `/js/**`, `/img/**`, `/error`.
  Todo lo demás exige sesión y redirige a `/login`.
- `/api/**` (A2) usa una cadena de seguridad separada (`@Order(1)`, `SessionCreationPolicy.STATELESS`,
  sin CSRF y sin redirección al login). Solo `POST /api/telemetria` está abierto y exige
  cabecera `Authorization: Bearer <token>` de un nodo activo (`ROLE_NODO`); el token se almacena
  cifrado con BCrypt (`dispositivo_iot.token_hash`), se muestra una sola vez al crearlo o
  regenerarlo, y cualquier otro endpoint bajo `/api/**` queda denegado. Sin token o con token
  inválido responde `401` en JSON; si el nodo está inactivo responde `403` en JSON. Ver
  [12-api-de-lecturas.md](12-api-de-lecturas.md).
- Login correcto → `/dashboard`. Login fallido → `/login?error`. Cerrar sesión (POST
  `/logout`) borra la cookie `JSESSIONID` y vuelve a `/?logout` con un aviso.
- **CSRF activo** en todos los formularios POST. Con `th:action` Thymeleaf añade el token
  solo; un formulario escrito con `action` plano o una petición sin token responde 403.

## Roles por cultivo (RF-07)

El rol **no es global**: se guarda por cultivo en `usuario_cultivo`. Una misma persona
puede ser administradora de un cultivo y colaboradora en otro.

| Acción | Colaborador | Administrador del cultivo |
|---|:---:|:---:|
| Ver detalle, umbrales, histórico y accesos | ✔ | ✔ |
| Exportar el histórico a Excel | ✔ | ✔ |
| Simular lecturas y atender alertas | ✔ | ✔ |
| Editar o eliminar el cultivo | | ✔ |
| Registrar o asignar nodos | | ✔ |
| Crear, editar o eliminar umbrales | | ✔ |
| Asignar, cambiar o quitar accesos | | ✔ |

- Quien crea un cultivo queda como su administrador.
- No se puede quitar ni degradar al **último** administrador de un cultivo.
- Cada persona solo ve sus cultivos, alertas, mapa y exportaciones.
- La vista oculta los botones que la persona no puede usar, pero **la protección real está
  en el servidor**: aunque alguien envíe el formulario a mano, recibe 403.

### Cómo se aplica

`security/PermisosCultivo` es un bean llamado `permisos` con tres comprobaciones:

```java
@PreAuthorize("@permisos.puedeVer(#idCultivo, authentication)")          // miembro
@PreAuthorize("@permisos.puedeAdministrar(#idCultivo, authentication)")  // administrador
@PreAuthorize("@permisos.puedeVerAlerta(#idAlerta, authentication)")     // miembro del cultivo de la alerta
```

Cada método de controlador que recibe un id de cultivo **debe** llevar una de estas
anotaciones. Un cultivo inexistente se trata igual que uno ajeno (403), para no revelar
qué ids existen. Las listas generales (`/cultivos`, `/dashboard`, `/alertas`, `/mapa`) se
filtran en el servicio con el correo de la sesión (`listarPara(correo)`).

## Manejo de errores

| Situación | Respuesta | Vista |
|---|---|---|
| Dato inválido en un formulario (Bean Validation) | Se vuelve a mostrar el formulario con el error bajo cada campo | La misma |
| Regla de negocio incumplida (`ReglaNegocioException`) | Mensaje en el formulario; si se escapa, **400** | La misma / `error/4xx.html` |
| Recurso inexistente (`RecursoNoEncontradoException`) | **404** | `error/404.html` |
| Sin permiso sobre el cultivo / sin token CSRF | **403** | `error/403.html` |
| Error inesperado | **500** sin trazas ni mensajes internos | `error/5xx.html` |

`server.error.include-stacktrace=never` y `server.error.include-message=never` impiden que
se filtren detalles internos.

## Secretos y configuración

- `application.properties` nunca se sube (regla `*.properties` en `.gitignore`). Solo se
  versiona `application.properties.example`, sin claves reales.
- Claves por variables de entorno: `DB_PASSWORD`, `OPENWEATHER_API_KEY`, `ADMIN_DEMO_CLAVE`.
- El administrador de demostración usa una clave pública (`AgroAdmin2026`). Fuera del
  entorno local: definir `ADMIN_DEMO_CLAVE` o desactivarlo con
  `agro.demo.admin.habilitado=false`. Al arrancar con la clave por defecto, el log muestra
  una advertencia.
- Si una clave llega a subirse por error al repositorio, hay que **cambiarla** (no basta con
  borrarla en un commit nuevo: queda en el historial).

## Pendiente

- Cabeceras de seguridad adicionales (Content-Security-Policy) en C2 si se publica en internet.
