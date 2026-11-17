# 10 · Usuarios de prueba

Registro de las cuentas de prueba que existen en las bases de datos **locales** de
desarrollo, con su contraseña y qué puede hacer cada una. Así nadie crea usuarios
"sueltos" sin saber luego cómo entrar con ellos.

> Estas cuentas solo existen en la BD local de cada integrante. **Nunca** usar estas
> contraseñas en un servidor compartido o publicado.

## Regla

**Quien cree un usuario de prueba lo anota en este archivo** (correo, contraseña, para qué
sirve y qué cultivos tiene). Si lo borra, también lo quita de aquí. Las pruebas
automáticas (`.\mvnw.cmd test`) crean usuarios dentro de una transacción que se revierte:
esos no quedan en la BD y no se anotan.

## Cuentas

| Correo | Contraseña | Rol y cultivos | Para qué sirve | Origen |
|---|---|---|---|---|
| `admin@agropiura.com` | `AgroAdmin2026` | Administrador de **Parcela Demo - Piura** | Demostración y pruebas de administrador | Lo crea la app al arrancar (`DataSeeder`). La clave cambia con `ADMIN_DEMO_CLAVE` |
| `k@gmail.com` | `12345678` | Administrador de **Mango - Parcela A** y **Papa** | Cuenta de desarrollo de Kevin con los datos de prueba iniciales | Registro manual (14/10/2026). Cultivos asignados con `database/migracion-datos-anteriores.sql` |
| `colab.test@agro.com` | *desconocida* (ver abajo) | Colaborador de **Parcela Demo - Piura** | Probar el rol colaborador: ve el cultivo pero no puede editarlo | Creada el 25/10/2026 al probar RF-07 |
| `test@agropiura.pe` | `test1234` | Sin cultivos | Probar un usuario nuevo sin datos (dashboard vacío, 403 en cultivos ajenos) | Registro manual (14/10/2026) |

### Cuenta con contraseña desconocida

La contraseña de `colab.test@agro.com` no quedó registrada y no se puede recuperar (se
guarda cifrada con BCrypt). Dos opciones:

- **Darle una contraseña nueva conocida** (solo en BD local) y anotarla en la tabla.
- **Crear otra cuenta colaboradora:** registrarse en `/registro`, entrar como
  `admin@agropiura.com` → Parcela Demo - Piura → Accesos → asignar el correo nuevo como
  colaborador. Anotarla aquí.

## Probar los roles rápidamente

| Quiero probar… | Entrar como |
|---|---|
| Todo lo que puede hacer un administrador | `admin@agropiura.com` o `k@gmail.com` |
| Que un colaborador ve pero no administra | `colab.test@agro.com` (en Parcela Demo - Piura) |
| Aislamiento entre usuarios (403 en cultivos ajenos) | `test@agropiura.pe` abriendo `/cultivos/180` |
| Cultivos con datos anteriores a los permisos | `k@gmail.com` |

## Si tu BD local tiene cultivos que nadie ve

Si al arrancar el log muestra
`N cultivo(s) sin administrador, invisibles para todos los usuarios`, son datos creados
antes de los permisos por cultivo. Se asignan a un usuario con:

```powershell
psql -h localhost -U postgres -d agropiura -v correo=tu@correo.com -f database/migracion-datos-anteriores.sql
```

Ver [04-base-de-datos.md](04-base-de-datos.md#scripts).
