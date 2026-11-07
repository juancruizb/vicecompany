# ViceCompany

Sincroniza la **Vicepresidencia** y la **Compañía** de los tickets de ManageEngine
ServiceDesk Plus con los atributos del usuario que vienen del Active Directory.
Reemplaza la página `sync_vice_cia.jsp` de una aplicación JSP existente.

Hecho con Spring Boot 4.1, Java 21, Thymeleaf, Spring Security y SQL Server (JDBC).

## Qué hace

Cada ticket tiene dos campos adicionales, Vice y Cía, que deben coincidir con los del
usuario que lo creó:

| Dato | Origen (usuario, viene del AD) | Destino (ticket) |
|---|---|---|
| Vicepresidencia | `UserAdditionalFields.UDF_CHAR2` | `Request_Additional_Fields_1.UDF_CHAR20` |
| Compañía | `UserAdditionalFields.UDF_CHAR1` | `Request_Additional_Fields_1.UDF_CHAR19` |

Un ticket es **inconsistente** si su Vice o su Cía están vacías (`NULL`, `''`, `(null)`),
tienen un valor de relleno (`vicepresidencia`, `company`) o no coinciden con las del
usuario. La sincronización copia el valor **actual** del usuario; si el usuario cambia de
vicepresidencia, sus tickets anteriores también se actualizan.

No se copian valores vacíos ni de relleno: si el usuario no tiene un dato real, el ticket
se deja como está. Los tickets sin fila en `Request_Additional_Fields_1` tampoco se pueden
actualizar.

## Página `/inconsistencias`

- **Resumen de candidatos**: cuántos tickets hay por motivo, si se pueden corregir y
  tickets de ejemplo. Se repliega a los 5 segundos; su título lo vuelve a abrir.
- **Tabla** de tickets inconsistentes con el motivo y si se pueden corregir.
- **Buscar un ticket** (`/inconsistencias?ticket=N`): lo muestra esté bien o no, y permite
  sincronizar solo ese ticket.
- **Sincronizar todo**: corrige todos los tickets corregibles en una transacción.
- **Estado de la sincronización automática**: si está activa y el resultado de la última
  ejecución.

Abrir la página nunca modifica la base. Solo escriben los botones (`POST`) y la tarea
programada, y únicamente si la escritura está habilitada.

## Configuración

Los datos reales de conexión van en `application-local.properties`, en la carpeta desde
donde se ejecuta la aplicación. Ese archivo **no se sube a Git**. Hay una plantilla en
[`application-local.properties.example`](application-local.properties.example).

| Propiedad | Por defecto | Para qué |
|---|---|---|
| `spring.datasource.url` / `username` / `password` | sin credenciales | Conexión a la base `servicedesk` |
| `vice.servicedesk.url` | `https://localhost` | Base de los enlaces a tickets y usuarios |
| `vice.sincronizacion.habilitada` | `false` | Si es `false`, nada escribe en la base |
| `vice.sincronizacion.cron` | `-` (apagada) | Cuándo sincroniza solo. `0 0 * * * *` = cada hora |
| `logging.file.name` | sin archivo | Log en archivo, por ejemplo `logs/vicecompany.log` |
| `server.port` / `server.address` | `8080` / todas | En el servidor: `8090` y `127.0.0.1` |

### Usuario de base de datos

La aplicación **no debe usar `sa`**. El script
[`scripts/create-sql-user.sql`](scripts/create-sql-user.sql) crea el login `vicecompany` con
el mínimo de permisos:

| Permiso | Tabla | Columnas |
|---|---|---|
| `SELECT` | `WorkOrder` | `WORKORDERID`, `REQUESTERID`, `CREATEDTIME` |
| `SELECT` | `SDUser` | `USERID` |
| `SELECT` | `AaaUser` | `USER_ID`, `FIRST_NAME` |
| `SELECT` | `UserAdditionalFields` | `INSTANCE_ID`, `UDF_CHAR2`, `UDF_CHAR1` |
| `SELECT` | `Request_Additional_Fields_1` | `WORKORDERID`, `UDF_CHAR20`, `UDF_CHAR19` |
| `UPDATE` | `Request_Additional_Fields_1` | `UDF_CHAR20` (Vice), `UDF_CHAR19` (Cía) |

No puede borrar, insertar, cambiar la estructura ni leer otras tablas o columnas.

1. En el servidor, con SQL Server Management Studio y un usuario administrador, abrir el
   script, cambiar `CAMBIAR_CONTRASENA` por una contraseña fuerte (sin guardar el archivo) y
   ejecutarlo.
2. Ejecutar la sección "Verificación": debe listar 14 filas, una por columna (12 `SELECT`
   y 2 `UPDATE`). La prueba como `vicecompany` lee 5 filas y luego `SELECT *` **debe**
   fallar con varios `Msg 230 ... permission was denied` en rojo: es el resultado esperado.
3. En `application-local.properties`: `spring.datasource.username=vicecompany` y la
   contraseña nueva. La contraseña solo se escribe en el servidor.
4. Reiniciar con `iniciar.bat` y comprobar que `/inconsistencias` muestra el mismo total
   que antes, sin errores en la ventana ni en `logs/vicecompany.log`.

Si al agregar una consulta nueva aparece `The SELECT permission was denied`, falta dar
permiso a esa columna en el script. Para volver atrás: poner otra vez el usuario anterior en
las propiedades y reiniciar; el script trae al final cómo eliminar el usuario.

Hacer este cambio por separado de la puesta en marcha (un cambio a la vez).

## Desarrollo

```bash
./mvnw spring-boot:run      # http://localhost:8080/login
./mvnw test                 # pruebas, sin base de datos real
./mvnw package              # genera target/vicecompany-0.0.1-SNAPSHOT.jar
```

El login usa por ahora un usuario de prueba en memoria, definido en `SecurityConfig.java`.

## Despliegue en el servidor (Windows)

La base solo es accesible desde la red del servidor, así que la aplicación se ejecuta allí.
El servidor tiene Java 8 para la aplicación JSP; ViceCompany usa un **JDK 21 portable**
propio, sin instalar nada ni cambiar el Java del sistema.

Contenido de la carpeta `despliegue` (se arma en local y no se sube a Git):

```
despliegue\
  vicecompany.jar                 ← ./mvnw package
  application-local.properties    ← credenciales y configuración
  iniciar.bat                     ← copia de scripts/iniciar.bat
  READ-ME.txt                     ← copia de scripts/READ-ME.txt (guía de operación)
  jdk21.zip                       ← Temurin 21 para Windows x64 (adoptium.net)
```

1. Copiar la carpeta al servidor, por ejemplo a `F:\ViceCompany2\despliegue`.
2. Ejecutar `iniciar.bat`. La primera vez descomprime el JDK, y luego arranca la aplicación.
3. Abrir http://localhost:8090/login desde el propio servidor.
4. Para detenerla, pulsar Ctrl+C o cerrar la ventana.

Al salir de una sesión RDP hay que **desconectarse, no cerrar sesión**: cerrar sesión detiene
la aplicación. Pendiente: instalarla como servicio de Windows.

Guía de operación para el servidor (arrancar, detener, modos y qué parámetro cambiar):
[`scripts/READ-ME.txt`](scripts/READ-ME.txt). Se copia en la carpeta `despliegue`.

### Puesta en marcha recomendada

1. Primero con `vice.sincronizacion.habilitada=false` y revisar la lista y el resumen.
2. Probar con un solo ticket: habilitar la escritura, usar "Sincronizar este ticket" y
   verificarlo en ServiceDesk Plus.
3. Activar `vice.sincronizacion.cron=0 0 * * * *` con la escritura habilitada y **cerrar
   el JSP**, para que no haya dos procesos sincronizando.
4. Revisar a la hora en punto la página y `logs/vicecompany.log`.

### Vuelta atrás

Si algo no se ve bien: detener ViceCompany (Ctrl+C) y abrir otra vez el JSP. Todo queda
como antes; ViceCompany no cambia la estructura de la base.

## Estructura

```
src/main/java/com/integratec/vicecompany/
  config/          SecurityConfig, SchedulingConfig
  inconsistencias/ Inconsistencia, InconsistenciaRepository, ResumenCandidatos,
                   SincronizacionAutomatica
  web/             PageController, InconsistenciaController
src/main/resources/
  templates/       login, home, inconsistencias (Thymeleaf)
  static/          css/, js/
scripts/           iniciar.bat
```

El historial del proyecto, las decisiones y los pendientes están en
[`BITACORA.md`](BITACORA.md).
