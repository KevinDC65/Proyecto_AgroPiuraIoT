# 03 · Funcionalidades y rutas

## Requerimientos funcionales

| RF | Descripción | Estado | Dónde |
|---|---|:---:|---|
| RF-01 | Registro e inicio de sesión con contraseña cifrada (BCrypt) | ✔ | `AuthController`, `UsuarioService` |
| RF-02 | CRUD de cultivos y gestión de nodos IoT | ✔ | `CultivoController`, `CultivoService` |
| RF-03 | Umbrales mínimo/máximo por cultivo y variable | ✔ | `UmbralController`, `UmbralService` |
| RF-04 | Dashboard con el resumen de los cultivos | ✔ | `DashboardController`, `DashboardService` |
| RF-05 | Motor de alertas y notificaciones | ✔ | `AlertaController`, `AlertasService` |
| RF-06 | Clima de terceros (OpenWeatherMap) | ✔ | `ClimaService`, `OpenWeatherCliente` |
| RF-07 | Roles administrador / colaborador por cultivo | ✔ | `AccesoController`, `AccesoService`, `PermisosCultivo` |
| RF-08 | Histórico por cultivo con gráfica y exportación a Excel | ✔ | `HistoricoController`, `HistoricoService` |
| — | Mapa de cultivos | ✔ | `MapaController` |
| — | Simulador de lecturas (demostración) | ✔ | `SimuladorService` |

Pendientes conocidos (no implementados): límite de intentos de inicio de sesión,
paginación del histórico y de las alertas, recepción real de lecturas desde el ESP32
(hoy se usan lecturas simuladas), pagos (la portada lo muestra como "Pronto").

## Pantallas

| Pantalla | Qué muestra | Acciones |
|---|---|---|
| **Portada** `/` | Presentación del producto con una vista de ejemplo de la app (datos ilustrativos) | Ingresar, crear cuenta |
| **Login** `/login` | Formulario de acceso; avisos de error y de sesión cerrada | — |
| **Registro** `/registro` | Nombre, correo, contraseña y confirmación con errores por campo | — |
| **Dashboard** `/dashboard` | Indicadores (cultivos activos, alertas pendientes, humedad y temperatura promedio), lista de cultivos con su banda de humedad, panel de clima | Ir al detalle de cada cultivo |
| **Cultivos** `/cultivos` | Tabla de cultivos del usuario (tarjetas en celular) | Nuevo, exportar a Excel, ver detalle |
| **Detalle de cultivo** `/cultivos/{id}` | Datos del cultivo, nodos asignados, accesos rápidos | Simular lectura, registrar o asignar nodo, editar, eliminar, umbrales, histórico, accesos |
| **Umbrales** `/cultivos/{id}/umbrales` | Umbral de cada variable con su banda de rango | Guardar o eliminar umbral |
| **Alertas** `/alertas` | Notificaciones no leídas y alertas de los cultivos del usuario | Marcar leída, marcar atendida |
| **Accesos** `/cultivos/{id}/accesos` | Personas con acceso y su rol | Asignar por correo, cambiar rol, quitar |
| **Histórico** `/cultivos/{id}/historico` | Gráfica cronológica, resumen por variable (mín, máx, promedio) y tabla de lecturas | Filtrar por fechas, exportar a Excel |
| **Mapa** `/mapa` | Cultivos con coordenadas, marcados por estado | Ir al detalle |
| **Errores** | 403, 404, 4xx y 5xx con mensaje claro y enlace de regreso | — |

## Tabla de rutas

Todas las rutas, salvo las públicas, requieren sesión. "Miembro" es cualquier persona con
acceso al cultivo; "Admin" es el administrador **de ese cultivo**.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/` | Público | Portada |
| GET | `/login` | Público | Formulario de login (POST lo procesa Spring Security) |
| GET / POST | `/registro` | Público | Crear cuenta |
| POST | `/logout` | Sesión | Cerrar sesión; vuelve a `/?logout` |
| GET | `/dashboard` | Sesión | Resumen de los cultivos propios |
| GET | `/cultivos` | Sesión | Lista de cultivos propios |
| GET | `/cultivos/nuevo` | Sesión | Formulario de alta |
| POST | `/cultivos` | Sesión | Crear cultivo (el creador queda como Admin) |
| GET | `/cultivos/exportar.xlsx` | Sesión | Excel con los cultivos propios |
| GET | `/cultivos/{id}` | Miembro | Detalle |
| GET / POST | `/cultivos/{id}/editar` | Admin | Editar |
| POST | `/cultivos/{id}/eliminar` | Admin | Eliminar (pide confirmación) |
| POST | `/cultivos/{id}/dispositivos/nuevo` | Admin | Registrar nodo nuevo con sus sensores |
| POST | `/cultivos/{id}/dispositivos` | Admin | Asignar un nodo existente por id |
| POST | `/cultivos/{id}/simular` | Miembro | Generar una ronda de lecturas |
| GET | `/umbrales` | Sesión | Redirige a los umbrales del primer cultivo (o a `/cultivos` si no hay) |
| GET | `/cultivos/{id}/umbrales` | Miembro | Ver umbrales |
| POST | `/cultivos/{id}/umbrales` | Admin | Guardar umbral |
| POST | `/cultivos/{id}/umbrales/{idUmbral}/eliminar` | Admin | Eliminar umbral |
| GET | `/alertas` | Sesión | Alertas y notificaciones propias |
| POST | `/alertas/{idNotificacion}/leida` | Dueño de la notificación | Marcar como leída |
| POST | `/alertas/{idAlerta}/atender` | Miembro del cultivo de la alerta | Marcar como atendida |
| GET | `/cultivos/{id}/accesos` | Miembro | Ver accesos |
| POST | `/cultivos/{id}/accesos` | Admin | Asignar o cambiar rol |
| POST | `/cultivos/{id}/accesos/quitar` | Admin | Quitar acceso |
| GET | `/historico` | Sesión | Redirige al histórico del primer cultivo (o a `/cultivos` si no hay) |
| GET | `/cultivos/{id}/historico?desde=&hasta=` | Miembro | Histórico filtrado |
| GET | `/cultivos/{id}/historico/exportar.xlsx?desde=&hasta=` | Miembro | Excel del histórico con el mismo filtro |
| GET | `/mapa` | Sesión | Mapa de cultivos propios |

Un cultivo ajeno o inexistente responde **403** (no revela qué ids existen). Una ruta
inexistente responde **404**.

## Reglas de negocio

- **Umbrales:** uno por cultivo y variable; si ya existe se actualiza. Mínimo ≤ máximo.
- **Alertas:** se generan cuando la lectura está fuera del rango del umbral de su variable.
  Sin umbral no hay alerta. Una alerta atendida deja de contarse como pendiente.
- **Accesos:** un cultivo siempre tiene al menos un administrador; no se puede quitar ni
  degradar al último.
- **Nodos:** un nodo nuevo nace con un sensor por cada tipo del catálogo. Puede estar en
  varios cultivos; al eliminar un cultivo, sus nodos exclusivos se eliminan con sus
  sensores y lecturas, y los compartidos se conservan.
- **Histórico:** "desde" y "hasta" son opcionales; si "desde" es posterior a "hasta" se
  muestra un mensaje en lugar de un error.
- **Simulador:** usa el rango del umbral si existe, o rangos plausibles por variable si no.
  Con umbral, cerca del 20 % de los valores salen fuera de rango.
