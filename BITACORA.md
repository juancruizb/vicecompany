# Bitácora del proyecto ViceCompany

Registro de lo que se ha hecho, por qué, y qué sigue pendiente. Se actualiza en cada sesión de trabajo.

## Qué es este proyecto

Aplicación Java con **Spring Boot** que expone una página de login web, pensada para
eventualmente conectarse a la misma base de datos SQL Server que usa una aplicación
JSP/Servlets existente (corre sobre Tomcat, desarrollada en Eclipse).

- **Backend**: Spring Boot 4.1.2, Java 21, Maven (con `mvnw`, no requiere Maven instalado).
- **Vistas**: Thymeleaf.
- **Seguridad**: Spring Security (login por formulario).
- **Paquete base**: `com.integratec.vicecompany`.
- **Repositorio**: https://github.com/juancruizb/vicecompany

> **Propósito real (aclarado 2026-11-05):** este proyecto nació como **piloto para aprender
> IntelliJ IDEA** — es el primer proyecto Java del autor usando este IDE (antes trabajaba con
> Eclipse). La idea de conectarlo a la base de datos SQL Server real es una extensión natural
> para seguir practicando, pero **no es, por ahora, un reemplazo decidido** del login de la
> app JSP existente. Queda abierto si en el futuro se integra, reemplaza, o se queda como
> proyecto de aprendizaje aparte.

## Estado actual

- Login funcional con usuario **en memoria** (`operador` / `911911`) — temporal, pendiente
  reemplazar por la tabla de usuarios real en SQL Server.
- Diseño visual del login inspirado en un mockup de un sistema 911 (header con drone,
  operadoras, badge "911"), pero usando fondos generados en CSS/SVG con temática IA + Java
  (red neuronal, circuito, `public class AI {}`) en vez de fotos reales, ya que no se contaba
  con las imágenes originales.
- Rutas protegidas: todo requiere sesión excepto `/login`.
- Después de login exitoso, redirige a `/home` (página de bienvenida simple, placeholder).

## Historial de cambios

### 2026-11-05 — Setup inicial y login funcional

- Se generó el proyecto con Spring Initializr (web, thymeleaf, security, devtools).
- Se creó `SecurityConfig` (usuario en memoria, formLogin) y `PageController` (`/login`, `/home`).
- Se armó el diseño visual del login completo (HTML + CSS).
- **Bug encontrado y corregido**: con el CSS embebido directo en el `<style>` del HTML,
  Tomcat enviaba la respuesta antes de que Thymeleaf terminara de procesar la plantilla
  (el buffer de salida se llenaba), y al llegar al `<form>` Spring Security no podía crear
  la sesión para el token CSRF (`Cannot create a session after the response has been committed`).
  **Solución**: mover el CSS a un archivo externo (`static/css/login.css`), dejando el HTML
  ligero. Lección: contenido pesado inline (CSS/JS/base64) en una plantilla Thymeleaf puede
  cortar la respuesta a medias — mejor siempre como archivo estático aparte.
- Se verificó el flujo completo: login → home → logout → mensaje de confirmación.

### 2026-11-05 (noche) — IntelliJ, renombrado y Git

- Se detectó que el proyecto no estaba importado como proyecto Maven en IntelliJ (por eso
  no había resaltado de sintaxis para clases/anotaciones ni navegación a definiciones).
  Se corrigió con "Add as Maven Project" sobre el `pom.xml`.
- Se renombró el proyecto completo de `BeginJava`/`login911` a **ViceCompany**:
  - Carpeta: `BeginJava` → `ViceCompany`
  - Paquete: `com.integratec.login911` → `com.integratec.vicecompany`
  - Clase principal: `Login911Application` → `ViceCompanyApplication`
  - `artifactId`/`name` en `pom.xml` y `spring.application.name`: `login911` → `vicecompany`
- Se inicializó Git, se ajustó `.gitignore` (agregando `target/` e `.idea/`, que faltaban),
  primer commit, y se conectó al repositorio remoto en GitHub
  (`https://github.com/juancruizb/vicecompany.git`).

### 2026-11-06 — Repaso de conceptos y CLAUDE.md

- Se aclaró en esta bitácora el propósito del proyecto: piloto para aprender IntelliJ.
- Se recorrieron `pom.xml`, `SecurityConfig`, las plantillas Thymeleaf y la carpeta `static/`.
- Prueba: se comentó `.loginPage("/login")` para ver la página de login que Spring Security
  genera por defecto ("Please sign in"). Luego se restauró la línea.
- Se creó `CLAUDE.md`, que Claude Code lee al iniciar cada sesión: indica leer esta bitácora,
  responder en español y subir todo a GitHub, y documenta comandos, estructura y cuidados.
- Acuerdo: todo commit se sube a GitHub.
- Se creó la carpeta local `referencia/`, ignorada por Git, para guardar el material de la
  app JSP (consultas SQL, DAO/JDBC, estructura de tablas). Motivo: el repositorio en GitHub
  es **público** y ese material describe una base de datos en producción. Antes de copiar
  archivos ahí, quitar contraseñas, IPs y usuarios de base de datos.

### 2026-11-06 (tarde) — Página de inconsistencias Vice/Cía

- Se copiaron a `referencia/` (local, no se sube) `Consultas.java`, `sync_vice_cia.jsp` y
  `gui.png` de la app JSP.
- Hallazgo: la base `servicedesk` es la de **ManageEngine ServiceDesk Plus**. La app JSP es
  una herramienta propia que corrige datos en esa base. Vice y Cía son atributos del usuario
  que vienen del Active Directory (`UserAdditionalFields.UDF_CHAR2`/`UDF_CHAR1`) y se
  quieren en cada ticket (`Request_Additional_Fields_1.UDF_CHAR20`/`UDF_CHAR19`).
- Se creó la página `/inconsistencias` (enlazada desde `/home`):
  - Lista los tickets inconsistentes con la misma consulta que `getInconsistencies()`.
  - Botón **Sincronizar** (`POST` con CSRF) que copia Vice/Cía del usuario a sus tickets,
    en una transacción.
  - Diferencias con el JSP: abrir la página **no** modifica la base (el JSP actualizaba en
    cada visita y cada hora); no se copian valores vacíos ni de relleno
    (`vicepresidencia`/`company`), que dejaban tickets inconsistentes para siempre; se usan
    dos `UPDATE` con `JOIN` en lugar de uno por ticket.
- Conexión: `spring-boot-starter-jdbc` + driver `mssql-jdbc`, consultas con `JdbcClient`.
  Los datos reales van en `application-local.properties` (raíz, ignorado por Git); hay una
  plantilla en `application-local.properties.example`.
- Pruebas: `InconsistenciaControllerTests` (5 casos, base simulada con `@MockitoBean`).
- **Aún no se probó contra la base real.**
- Para probar antes del lote:
  - `/inconsistencias?ticket=N` (o el buscador) muestra un ticket, esté bien o no, con su
    motivo; el botón **Sincronizar este ticket** aplica el mismo `UPDATE` solo a ese ticket.
    En esa vista no aparece "Sincronizar todo".
  - Columnas **Motivo** y **¿Se corrige?** en cada fila, y un **Resumen de candidatos**
    arriba: cantidad de tickets por motivo y 3 tickets de ejemplo por grupo para probar.
  - Motivos: campo vacío (`NULL`, `''`, `(null)`), con relleno (`vicepresidencia`/`company`),
    distinto del AD; "sin dato en AD" si el usuario no tiene un valor real; "Ticket sin
    campos adicionales" si no hay fila en `Request_Additional_Fields_1` (el `UPDATE` no la crea).
  - Pruebas: `InconsistenciaTests` (10 casos de motivos y resumen) y 8 del controlador.
- Interruptor `vice.sincronizacion.habilitada` (por defecto `false`): en solo lectura no se
  muestran los botones y el servidor rechaza cualquier `POST` de sincronización. 3 pruebas
  en `InconsistenciaSoloLecturaTests`.
- Se creó `application-local.properties` (local, ignorado) con la conexión de
  `Consultas.getUrl()` y `vice.sincronizacion.habilitada=false`. Desde el Mac el puerto de
  SQL Server no respondió: la base solo es accesible desde la red del cliente, a la que se
  entra por RDP (no hay VPN).
- Por eso la prueba se hará **en el servidor**: carpeta local `despliegue/` (ignorada por
  Git) con `vicecompany.jar`, `application-local.properties` (con `server.port=8090` para no
  chocar con Tomcat y `server.address=127.0.0.1` para que solo se abra desde el propio
  servidor), `iniciar.bat` y `LEEME.txt`. Se comprobó que el paquete arranca.
- El servidor tiene **Java 8** (1.8.0_321), que usa la app JSP y no se debe cambiar. Se agregó
  `jdk21.zip` (Temurin 21.0.13+9 para Windows x64, checksum SHA-256 verificado contra
  Adoptium); `iniciar.bat` lo descomprime la primera vez en `jdk\` y lo usa solo para
  ViceCompany, sin tocar el PATH ni el Java del sistema. Memoria limitada con `-Xmx512m`.

### 2026-11-06 (noche) — Prueba 1 en el servidor (solo lectura): resultado

- ViceCompany corrió en el servidor (`F:\ViceCompany\despliegue`, Java 21 portable, puerto
  8090). El primer `iniciar.bat` falló al renombrar `jdk-21...` a `jdk` ("Access is denied",
  probablemente el antivirus revisando los archivos); se corrigió el `.bat` para no renombrar.
- Conectó a la base y mostró **5482 inconsistencias, igual que el JSP** en ese momento.
- Resumen de candidatos (suma 5482):

  | Motivo | Tickets |
  |---|---|
  | Vice vacío (sin dato en AD); Cía vacío (sin dato en AD) | 2217 |
  | Vice y Cía con valor de relleno (sin dato en AD) | 1913 |
  | Vice vacío (sin dato en AD) | 952 |
  | Cía vacío (sin dato en AD) | 361 |
  | Cía con valor de relleno (sin dato en AD) | 18 |
  | Vice vacío y Cía con relleno (sin dato en AD) | 16 |
  | Ticket sin campos adicionales | 5 |

- **Conclusión: ninguno es corregible.** No hay ningún caso "distinto del AD": el JSP ya
  mantenía sincronizado todo lo que tenía dato. Las 5482 son de usuarios sin Vice/Cía real en
  `UserAdditionalFields` (problema de datos del AD/importación de usuarios, no del ticket).
- Se cerró el JSP para que no actualice. Plan: esperar tickets nuevos de usuarios con datos
  en el AD (aparecerán como "¿Se corrige? Sí") y usar uno para la prueba 2.

### 2026-11-06 (noche) — Prueba 2 en el servidor (un ticket): exitosa

- Para tener un candidato se borró a propósito la Cía del ticket **164830** (solicitante con
  Vice y Cía reales en el AD). Apareció en el resumen como "Cía vacío — ¿Se corrige? Sí";
  total 5483.
- Con `vice.sincronizacion.habilitada=true` se pulsó "Sincronizar este ticket":
  **"0 tickets con Vice y 1 con Cía actualizados"**. La Cía volvió a "Seguros Universal",
  tomada del atributo del usuario (AD-Cía), y el ticket quedó "Sincronizado". La Vice, que
  ya coincidía, no se tocó.
- Queda validado el `UPDATE` por ticket en producción.
- Al terminar se dejó todo como estaba: `vice.sincronizacion.habilitada=false`, ViceCompany
  detenido y el JSP abierto de nuevo (sigue sincronizando los tickets nuevos). En el servidor
  queda `F:\ViceCompany\despliegue` listo para volver a usarse.

## Conceptos aprendidos

**Refactorizar**: cambiar cómo está escrito u organizado el código sin cambiar lo que hace.
Viene de "factorizar" en matemáticas: `6x + 9 = 3(2x + 3)` vale lo mismo, pero se sacó el
factor común. En código, es sacar lo repetido a un solo lugar. En IntelliJ: menú Refactor,
`⌃T`, o `⇧F6` para renombrar en todo el proyecto.

**`pom.xml` (Project Object Model)**: la ficha del proyecto para Maven. Define nombre,
versión, versión de Java, dependencias (como `requirements.txt` o `pyproject.toml`) y cómo
construir el `.jar`. Hereda de `spring-boot-starter-parent`, por eso las dependencias no
llevan versión. `SNAPSHOT` = versión en desarrollo.

**`SecurityConfig`**: el portero de la app. `@Configuration` marca la clase para que Spring
la lea al arrancar; cada `@Bean` crea una pieza que Spring guarda y entrega donde se necesite:
- `filterChain`: qué rutas son públicas (`permitAll`) y cuáles piden sesión; usa nuestra
  página de login y redirige a `/home`.
- `passwordEncoder`: las contraseñas se guardan hasheadas (bcrypt), no en texto plano.
- `userDetailsService`: de dónde salen los usuarios. Hoy, en memoria; es lo que se
  reemplazará al conectar SQL Server.

**Íconos verdes en el margen de IntelliJ**: marcan beans de Spring. Al hacer clic, muestran
dónde se usa o de dónde viene cada bean. Otras ayudas visuales: escudo en rutas de Spring
Security, pistas de parámetros en gris, autor según Git, y el bombillo (`⌥↩`) con sugerencias.

**Sangría en Java**: no es obligatoria como en Python. Los bloques los definen `{ }` y las
instrucciones terminan en `;`. Se indenta por convención; `⌥⌘L` reformatea el archivo.

**Thymeleaf**: motor de plantillas, como los templates de Django, pero con atributos `th:`
dentro del HTML en lugar de `{% %}`:
- `${...}` lee datos (`th:if="${param.error}"`).
- `@{...}` arma URLs (`th:href="@{/css/login.css}"`).
- `th:action` en un formulario agrega solo el token CSRF (como `{% csrf_token %}`).

**`templates/` vs `static/`**:
- HTML (esqueleto), CSS (apariencia) y JS (comportamiento) arman juntos la vista.
- `templates/` guarda el HTML que Thymeleaf procesa en el servidor.
- `static/` guarda CSS, JS e imágenes que van tal cual al navegador. Se sirven desde la raíz
  (`static/css/login.css` → `/css/login.css`), sin `collectstatic`.
- Una ruta estática nueva (por ejemplo `/js/**`) debe agregarse a `permitAll()` si se usa en
  la página de login.

**Equivalentes de Django en Spring**:
- ORM → Spring Data JPA (Hibernate). Con una base ajena, usar `ddl-auto=none` o `validate`.
- Django REST Framework → `@RestController` (ya incluido en `starter-webmvc`).
- Django Admin → no hay uno incluido; se construye a mano (Thymeleaf) o con JHipster.
  "Spring Boot Admin" solo monitorea la app, no administra datos.
- Ver y editar la base desde el IDE: ventana Database de IntelliJ Ultimate.

**JetBrains**: IntelliJ y PyCharm comparten la misma base (IntelliJ Platform), por eso se
usan igual. Cambia el soporte de cada lenguaje: JDK/Maven/Spring en uno, intérprete/venv/pip
en el otro.

**`JdbcClient`**: ejecuta SQL escrito a mano, como `connection.cursor().execute()` en
Django pero con la conexión y el cierre de recursos a cargo de Spring. Se eligió en vez de
JPA porque la base es de otro producto y las consultas ya existían.

**`@Transactional`**: todo el método se confirma o se deshace junto, como
`transaction.atomic()` en Django.

**Post/Redirect/Get**: un `POST` que modifica datos termina con `redirect:`, para que
recargar la página no repita la acción. Los "flash attributes" sobreviven a esa redirección,
como `messages.success()` en Django.

**`@MockitoBean`**: en una prueba, reemplaza un bean real (el repositorio) por uno simulado,
para probar el controlador sin base de datos.

**Memoria de Claude Code**: no recuerda conversaciones anteriores por sí mismo. Lo que pasa
de una sesión a otra: sus notas de memoria, `CLAUDE.md` (lo lee solo), esta bitácora y el
historial de Git. Para retomar una conversación completa: `claude --continue` o `claude --resume`.

## Pendiente / próximos pasos

- [x] Prueba 1 (solo lectura): hecha el 2026-11-06, ver historial.
- [ ] Investigar por qué 4000+ usuarios no tienen Vice/Cía real en `UserAdditionalFields`
  (¿faltan en el AD o la importación de usuarios de ServiceDesk no los trae?). Posible
  reporte por usuario (no por ticket) para corregirlos en el origen.
- [ ] Mientras el JSP esté cerrado, los tickets nuevos no se sincronizan: reabrirlo o activar
  ViceCompany cuando termine la prueba 2.
- [x] Prueba 2 (un ticket 164830): exitosa el 2026-11-06; servidor restaurado.
- [ ] Siguiente paso propuesto: reemplazar el JSP con una tarea `@Scheduled` en ViceCompany
  (sincronizar cada hora sin página abierta) y correrlo como servicio de Windows.
- [ ] Guardar en el repositorio una plantilla de `iniciar.bat` (sin credenciales).
- [ ] Ajuste visual: el encabezado fijo de la tabla tapa parte de la fila de arriba.
- [ ] Crear en SQL Server un usuario para ViceCompany (no `sa`): lectura en las tablas de la
  consulta y `UPDATE` solo en `Request_Additional_Fields_1`.
- [ ] Decidir si la sincronización debe correr sola (`@Scheduled`) además del botón.
- [ ] Revisar la clase de la app JSP que cuenta inconsistencias (`CountI`) por si la cuenta
  difiere de la lista.
- [ ] Login: decidir si sigue con usuarios propios o se conecta a otra fuente. Las tablas de
  usuarios de ServiceDesk Plus no son recomendables para validar contraseñas.
  Falta definir:
  - Si ya existe tabla de usuarios o hay que crearla.
  - Host/puerto/nombre de la base de datos.
  - Si las contraseñas están en texto plano o hasheadas.
  - Si la conexión será local (misma red que la VM) o se expondrá hacia internet.
- [ ] Considerar hacer privado el repositorio en GitHub antes de agregar entidades JPA con
  los nombres reales de tablas y columnas. Las credenciales nunca van en el repositorio.
- [ ] Definir el despliegue definitivo en el servidor (servicio de Windows, usuario de base
  propio, contraseña fuera del archivo). Por ahora se ejecuta a mano con `iniciar.bat`.
- [ ] Reemplazar los fondos CSS/SVG por las fotos reales si se consiguen.
- [ ] Posibles mejoras visuales menores (brillo del badge "911", etc.).
