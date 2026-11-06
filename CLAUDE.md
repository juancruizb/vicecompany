# ViceCompany

Proyecto piloto para aprender IntelliJ IDEA y Spring Boot. El autor viene de Python/Django
(PyCharm) y de una app JSP/Servlets en Eclipse.

## Al empezar cada sesión

- Leer `BITACORA.md`: tiene el estado actual, el historial y los pendientes.
- Responder en español.
- Explicar con comparaciones de Python/Django cuando ayuden, y con ejemplos de los archivos
  de este proyecto.

## Al terminar un trabajo

- Actualizar `BITACORA.md` (historial y pendientes).
- Hacer commit y push a GitHub (`origin main`): todo el trabajo debe quedar allí.

## Stack y comandos

- Spring Boot 4.1.2, Java 21, Maven con `./mvnw` (no hace falta Maven instalado).
- Thymeleaf para las vistas y Spring Security para el login por formulario.
- Ejecutar: `./mvnw spring-boot:run` → http://localhost:8080/login
- Pruebas: `./mvnw test`
- Construir el `.jar`: `./mvnw package`

## Estructura

- `src/main/java/com/integratec/vicecompany/config/SecurityConfig.java`: reglas de acceso,
  usuario en memoria y codificador de contraseñas.
- `src/main/java/com/integratec/vicecompany/web/PageController.java`: rutas `/login` y `/home`.
- `src/main/java/com/integratec/vicecompany/web/InconsistenciaController.java` e
  `inconsistencias/InconsistenciaRepository.java`: página `/inconsistencias` y sincronización
  de Vice/Cía (consultas SQL con `JdbcClient`).
- `application-local.properties` (raíz, ignorado por Git): credenciales de la base. Plantilla
  en `application-local.properties.example`.
- `referencia/` (ignorada por Git): código de la app JSP original, solo para consulta.
- `src/main/resources/templates/`: HTML procesado por Thymeleaf.
- `src/main/resources/static/`: CSS, JS e imágenes servidos tal cual.

## Cuidados

- No poner CSS/JS pesado inline en las plantillas Thymeleaf: llena el buffer, la respuesta
  se envía antes de tiempo y Spring Security no puede crear la sesión para el token CSRF.
  Usar archivos en `static/`.
- Cualquier ruta estática nueva (por ejemplo `/js/**`) debe agregarse a `permitAll()` en
  `SecurityConfig` si se usa en la página de login.
- La base SQL Server (`servicedesk`) es la de ManageEngine ServiceDesk Plus, en producción:
  nunca cambiar su estructura (si se usa JPA, `ddl-auto=none`). Toda escritura debe ir por
  `POST` explícito, nunca al abrir una página.
- El repositorio de GitHub es público: no subir credenciales, IPs internas ni el contenido de
  `referencia/`.
