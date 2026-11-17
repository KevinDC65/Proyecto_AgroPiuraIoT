# 09 · Reglas del proyecto

Reglas obligatorias para todas las personas del equipo **y para cualquier asistente de
código con IA** que se use en este repositorio.

> **Si usas un asistente de código:** antes de pedirle cambios, indícale que lea este
> archivo completo y que lo cumpla. Ejemplo: *"Lee `docs/09-reglas-del-proyecto.md`,
> `docs/02-arquitectura.md` y `docs/05-seguridad-y-permisos.md` antes de tocar nada.
> No modifiques las zonas protegidas sin pedirme permiso."*
> Tú eres responsable de lo que se sube: revisa cada cambio antes de hacer commit.

---

## 1. Zonas protegidas

No se modifican sin acuerdo explícito del equipo. Un asistente de código **debe detenerse
y preguntar** antes de tocarlas, aunque crea que el cambio es una mejora.

| Zona | Archivos | Por qué |
|---|---|---|
| Seguridad | `config/SecurityConfig.java`, `security/PermisosCultivo.java`, `security/AppUserDetailsService.java` | Un cambio aquí puede abrir el acceso a datos de todos los usuarios |
| Anotaciones de permisos | Todas las `@PreAuthorize("@permisos...")` de `controller/` | Quitar o cambiar una expone cultivos ajenos |
| Esquema de BD | `database/esquema.sql`, `database/indices.sql`, `model/entity/*.java` | Deben coincidir exactamente; si no, la app no arranca |
| Configuración | `application.properties.example`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, `server.error.*` | Protegen la BD y evitan filtrar información |
| Versiones | `pom.xml`: Java 25, Spring Boot 4.1.1 y versiones de librerías | Un cambio de versión puede romper todo el proyecto |
| Datos iniciales | `config/DataSeeder.java` | Crea catálogos de los que depende todo el sistema |
| Sistema visual | Variables `:root` de `static/css/app.css`, `templates/components/layout.html` | Afectan a todas las pantallas |
| Pruebas existentes | `src/test/**` | No se borran ni se debilitan para que "pasen" |

## 2. Reglas para evitar inventar cosas

Los asistentes de código a veces proponen clases, métodos, columnas o librerías que **no
existen** en este proyecto. Para evitarlo:

1. **Verificar antes de usar.** Antes de llamar a un método, una clase, una columna o una
   ruta, comprobar que existe en el código (`grep`, búsqueda del IDE). No suponer.
2. **Columnas y tablas:** la única referencia es `database/esquema.sql`. No inventar
   columnas; si hace falta una nueva, se sigue el proceso de
   [04-base-de-datos.md](04-base-de-datos.md#scripts).
3. **Versiones reales:** Spring Boot **4.1** y Spring Security **7** (no 2.x ni 3.x).
   Antes de usar una API de Spring, confirmar que existe en esa versión en la
   documentación oficial. Ejemplos de nombres propios de esta versión: los starters
   `spring-boot-starter-webmvc` y `spring-boot-starter-security-test`.
4. **No añadir dependencias** al `pom.xml` sin acuerdo del equipo. Antes de añadir una,
   comprobar si lo que se necesita ya está cubierto (Guava, Commons Lang3, POI, Spring).
5. **No afirmar que algo funciona sin probarlo.** "Debería funcionar" no vale: se ejecuta
   `.\mvnw.cmd test` y se revisa la pantalla afectada en el navegador.
6. **Mensajes de error reales:** si algo falla, leer el error completo antes de cambiar
   código. No "arreglar" a ciegas probando cambios al azar.
7. **Cambios mínimos:** tocar solo lo necesario para la tarea. No reformatear archivos
   enteros, no renombrar cosas que no se pidieron, no "aprovechar" para refactorizar.

## 3. Reglas de código

- **Idioma:** todo en español (clases, métodos, variables, mensajes, vistas, commits).
- **Capas:** controlador → servicio → repositorio. Nada de consultas en controladores ni
  lógica de negocio en vistas. Ver [02-arquitectura.md](02-arquitectura.md).
- **Permisos:** toda ruta nueva que reciba un id de cultivo lleva
  `@PreAuthorize("@permisos.puedeVer(...)")` o `puedeAdministrar(...)`. Toda lista nueva se
  filtra por el usuario de la sesión.
- **Errores:** usar `ReglaNegocioException` o `RecursoNoEncontradoException`; nunca
  devolver un 500 por un dato del usuario.
- **Formularios:** POST con `th:action` (token CSRF automático). Nunca desactivar CSRF.
- **Esquema:** nunca cambiar `ddl-auto` a `update` o `create`.
- **Caché de umbrales:** si un cambio modifica umbrales o asignaciones de nodos, invalidar
  la caché (`AlertasService.invalidarCacheUmbrales`).
- **Interfaz:** usar las clases y variables de `app.css`; no estilos en línea ni colores
  sueltos; probar a 390px de ancho.
- **Secretos:** nunca escribir claves, contraseñas ni API keys en el código ni en archivos
  versionados. Van en variables de entorno.

## 4. Antes de cada commit

- [ ] `.\mvnw.cmd test` en verde (56 o más pruebas).
- [ ] La pantalla afectada probada en el navegador, en escritorio y a 390px.
- [ ] Sin claves ni datos personales en los cambios (`git diff` revisado línea por línea).
- [ ] Si cambió el esquema: `database/esquema.sql` actualizado.
- [ ] Si cambió el comportamiento: documentación de `docs/` actualizada y entrada en
      [08-registro-de-cambios.md](08-registro-de-cambios.md).
- [ ] Ningún archivo de configuración local de editores o herramientas en el commit.

## 5. Commits y ramas

- Trabajar en una rama por tarea: `feat/nombre-corto`, `fix/nombre-corto`, `docs/...`.
  No hacer commits directos en `main` sin revisión del equipo.
- Mensaje en español con prefijo, siguiendo el historial del repositorio:
  `Feat:`, `Fix:`, `Docs:`, `Refactor:`, `Test:`, `Chore:`. Ejemplo:
  `Fix: el histórico conserva el filtro de fechas al exportar a Excel`.
- **Los mensajes de commit y las descripciones de pull request no llevan firmas,
  coautorías ni menciones a herramientas o asistentes** (nada de líneas `Co-Authored-By`
  añadidas por herramientas, ni textos tipo "Generado con…"). El autor del commit es la
  persona que lo revisa y lo sube.
- No usar `git push --force` en `main`.

## 6. Archivos que no se suben

Ya están en `.gitignore`; no forzarlos con `git add -f`:

- `src/main/resources/application.properties` (credenciales locales)
- `target/`, `logs/`, `evidencia/`
- Carpetas y archivos de configuración de editores y asistentes de código
  (`.vscode/`, `.idea/` y similares)

## 7. Si algo sale mal

- **La app no arranca tras un cambio:** leer el primer error del log; casi siempre es una
  entidad que no coincide con el esquema o un bean mal configurado.
- **Una prueba falla:** se corrige el código, no la prueba. Solo se cambia una prueba si el
  comportamiento esperado cambió de verdad y el equipo lo acordó.
- **Se subió una clave por error:** avisar al equipo y cambiar la clave de inmediato.
