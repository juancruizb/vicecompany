# ViceCompany

Proyecto piloto para aprender IntelliJ IDEA y Spring Boot. El autor viene de Python/Django
(PyCharm) y de una app JSP/Servlets en Eclipse.

## Al empezar cada sesión

- Leer `BITACORA.md`: tiene el estado actual, el historial y los pendientes.
- Responder en el idioma en que pregunte el usuario: en español si pregunta en español, en inglés si pregunta en inglés.
- Explicar con comparaciones de Python/Django cuando ayuden, y con ejemplos de los archivos
  de este proyecto.

## Al terminar un trabajo

- Actualizar `BITACORA.md` (historial y pendientes).
- Hacer commit y push a GitHub (`origin main`): todo el trabajo debe quedar allí.

## Stack y comandos

- Spring Boot 4.1.2, Java 21, Maven con `./mvnw` (no hace falta Maven instalado).
- Thymeleaf para las vistas y Spring Security para el login por formulario.
- Ejecutar: `./mvnw spring-boot:run` → http://localhost:8080/login (operador / 911911)
- Pruebas: `./mvnw test`
- Construir el `.jar`: `./mvnw package`

## Estructura

- `src/main/java/com/integratec/vicecompany/config/SecurityConfig.java`: reglas de acceso,
  usuario en memoria y codificador de contraseñas.
- `src/main/java/com/integratec/vicecompany/web/PageController.java`: rutas `/login` y `/home`.
- `src/main/java/com/integratec/vicecompany/web/InconsistenciaController.java`: página
  `/inconsistencias` (lista, `?ticket=N`, `POST /inconsistencias/sincronizar`).
- `inconsistencias/InconsistenciaRepository.java`: consultas SQL con `JdbcClient` (la de
  inconsistencias es la del JSP, líneas 151–156 de `Consultas.java`).
- `inconsistencias/Inconsistencia.java`: un ticket; calcula motivo y si es corregible.
- `inconsistencias/ResumenCandidatos.java`: agrupa por motivo para el resumen.
- `inconsistencias/SincronizacionAutomatica.java` + `config/SchedulingConfig.java`: tarea
  `@Scheduled` que reemplaza al JSP.
- `scripts/iniciar.bat`: plantilla del arranque en el servidor (sin credenciales).
- `application-local.properties` (raíz, ignorado por Git): credenciales de la base. Plantilla
  en `application-local.properties.example`.
- `referencia/` (ignorada por Git): código de la app JSP original, solo para consulta.
- `despliegue/` (ignorada por Git): paquete para copiar al servidor por RDP (jar, propiedades
  con credenciales, `iniciar.bat`). La base solo es accesible desde la red del servidor.
- `src/main/resources/templates/`: HTML procesado por Thymeleaf.
- `src/main/resources/static/`: CSS, JS e imágenes servidos tal cual.

## Propiedades propias

- `vice.sincronizacion.habilitada` (por defecto `false`): si es `false`, nada escribe en la base.
- `vice.sincronizacion.cron` (por defecto `-`, apagada): `0 0 * * * *` = cada hora.
- `vice.servicedesk.url`: base de los enlaces a tickets y usuarios de ServiceDesk Plus.

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
  `referencia/` o `despliegue/`. Revisar el diff antes de cada commit.
- Criterio acordado: la Vice/Cía del ticket es la **actual** del usuario en el AD. No copiar
  valores vacíos ni de relleno (`vicepresidencia`/`company`).
- Toda prueba de escritura en producción: primero un solo ticket (`?ticket=N`), después el lote.
