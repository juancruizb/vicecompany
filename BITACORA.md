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

**Memoria de Claude Code**: no recuerda conversaciones anteriores por sí mismo. Lo que pasa
de una sesión a otra: sus notas de memoria, `CLAUDE.md` (lo lee solo), esta bitácora y el
historial de Git. Para retomar una conversación completa: `claude --continue` o `claude --resume`.

## Pendiente / próximos pasos

- [ ] Conectar el login a la base de datos SQL Server real (reemplazar usuario en memoria).
  Falta definir:
  - Si ya existe tabla de usuarios o hay que crearla.
  - Host/puerto/nombre de la base de datos.
  - Si las contraseñas están en texto plano o hasheadas.
  - Si la conexión será local (misma red que la VM) o se expondrá hacia internet.
- [ ] Revisar el código DAO/JDBC de la app JSP existente para entender la consulta SQL y
  las condiciones de actualización que maneja.
- [ ] Construir el `.jar` ejecutable (`./mvnw package`) y definir cómo se desplegará en la VM.
- [ ] Reemplazar los fondos CSS/SVG por las fotos reales si se consiguen.
- [ ] Posibles mejoras visuales menores (brillo del badge "911", etc.).
