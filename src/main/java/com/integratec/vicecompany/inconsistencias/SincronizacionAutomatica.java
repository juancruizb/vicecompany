package com.integratec.vicecompany.inconsistencias;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository.ResultadoSincronizacion;

/**
 * Reemplaza al JSP sync_vice_cia.jsp: sincroniza Vice/Cía de todos los tickets según un cron,
 * sin necesidad de tener una página abierta.
 *
 * Dos llaves, ambas en application-local.properties:
 * - vice.sincronizacion.cron: cuándo corre. "-" (por defecto) = apagada.
 * - vice.sincronizacion.habilitada: si puede escribir. En false, la tarea corre pero no hace nada.
 */
@Component
public class SincronizacionAutomatica {

    private static final Logger log = LoggerFactory.getLogger(SincronizacionAutomatica.class);
    private static final String APAGADA = "-";

    /** Resultado de la última ejecución, para mostrarlo en la página. */
    public record Ejecucion(LocalDateTime cuando, boolean exitosa, String resultado) {
    }

    private final InconsistenciaRepository repository;
    private final boolean habilitada;
    private final String cron;
    private volatile Ejecucion ultima;

    public SincronizacionAutomatica(InconsistenciaRepository repository,
                                    @Value("${vice.sincronizacion.habilitada:false}") boolean habilitada,
                                    @Value("${vice.sincronizacion.cron:-}") String cron) {
        this.repository = repository;
        this.habilitada = habilitada;
        this.cron = cron;
        if (activa()) {
            log.info("Sincronización automática activa (cron '{}', escritura {})",
                    cron, habilitada ? "habilitada" : "DESHABILITADA: no modificará tickets");
        }
    }

    // Formato cron de Spring: segundo minuto hora día mes día-semana.
    // "0 0 * * * *" = cada hora en punto. "-" = nunca.
    @Scheduled(cron = "${vice.sincronizacion.cron:-}")
    public void ejecutar() {
        if (!habilitada) {
            log.warn("Sincronización automática omitida: modo solo lectura (vice.sincronizacion.habilitada=false)");
            ultima = new Ejecucion(LocalDateTime.now(), false, "Omitida: modo solo lectura");
            return;
        }
        try {
            ResultadoSincronizacion r = repository.sincronizar();
            String resultado = r.vice() + " tickets con Vice y " + r.cia() + " con Cía actualizados";
            log.info("Sincronización automática: {}", resultado);
            ultima = new Ejecucion(LocalDateTime.now(), true, resultado);
        } catch (DataAccessException e) {
            log.error("Falló la sincronización automática; no se modificó ningún ticket", e);
            ultima = new Ejecucion(LocalDateTime.now(), false, "Falló; no se modificó ningún ticket (ver log)");
        }
    }

    public boolean activa() {
        return !APAGADA.equals(cron.trim());
    }

    public boolean escrituraHabilitada() {
        return habilitada;
    }

    public String cron() {
        return cron;
    }

    public Optional<Ejecucion> ultima() {
        return Optional.ofNullable(ultima);
    }
}
