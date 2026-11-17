# 05 · Seguridad y permisos

## Autenticación

- Login por formulario con Spring Security (`config/SecurityConfig`). El correo es el
  nombre de usuario.
- Contraseñas guardadas con **BCrypt**; nunca en texto plano ni en logs.
- Rutas públicas: `/`, `/login`, `/registro`, `/css/**`, `/js/**`, `/img/**`, `/error`.
  Todo lo demás exige sesión y redirige a `/login`.
- `/api/**` está **cerrada para todos** (`denyAll`). La API de telemetría ya existe, pero
  abrirla sin autenticar cada nodo permitiría escribir lecturas en cultivos ajenos. Se abre
  en el issue A2, con token por nodo.
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

- Límite de intentos de inicio de sesión (protección contra fuerza bruta).
- Cabeceras de seguridad adicionales (Content-Security-Policy) si se publica en internet.
