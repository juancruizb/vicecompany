package com.integratec.vicecompany.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository.ResultadoSincronizacion;
import com.integratec.vicecompany.inconsistencias.ResumenCandidatos;

@Controller
public class InconsistenciaController {

    private static final Logger log = LoggerFactory.getLogger(InconsistenciaController.class);

    private final InconsistenciaRepository repository;
    private final String servicedeskUrl;
    private final boolean sincronizacionHabilitada;

    public InconsistenciaController(InconsistenciaRepository repository,
                                    @Value("${vice.servicedesk.url}") String servicedeskUrl,
                                    @Value("${vice.sincronizacion.habilitada:false}") boolean sincronizacionHabilitada) {
        this.repository = repository;
        this.servicedeskUrl = servicedeskUrl;
        this.sincronizacionHabilitada = sincronizacionHabilitada;
    }

    /**
     * Solo muestra; a diferencia del JSP, abrir la página no modifica la base.
     * Con ?ticket=N muestra solo ese ticket (como request.GET.get("ticket") en Django).
     */
    @GetMapping("/inconsistencias")
    public String inconsistencias(@RequestParam(required = false) Long ticket, Model model) {
        model.addAttribute("servicedeskUrl", servicedeskUrl);
        model.addAttribute("ticket", ticket);
        model.addAttribute("sincronizacionHabilitada", sincronizacionHabilitada);
        try {
            if (ticket == null) {
                var todas = repository.buscarTodas();
                model.addAttribute("inconsistencias", todas);
                model.addAttribute("resumen", ResumenCandidatos.agrupar(todas));
            } else {
                model.addAttribute("inconsistencias", repository.buscarTicket(ticket).map(List::of).orElse(List.of()));
            }
        } catch (DataAccessException e) {
            log.error("No se pudo consultar la base de ServiceDesk Plus", e);
            model.addAttribute("error", "No se pudo conectar a la base de datos. Revisa application-local.properties.");
        }
        return "inconsistencias";
    }

    /**
     * Corrige los tickets: uno solo si llega "ticket", todos si no. Va por POST (con token
     * CSRF) para que solo se ejecute al pulsar el botón. Después redirige al GET
     * (patrón Post/Redirect/Get, igual que en Django).
     */
    @PostMapping("/inconsistencias/sincronizar")
    public String sincronizar(@RequestParam(required = false) Long ticket, RedirectAttributes redirect) {
        if (ticket != null) {
            redirect.addAttribute("ticket", ticket);
        }
        if (!sincronizacionHabilitada) {
            log.warn("Sincronización rechazada: modo solo lectura (ticket={})", ticket);
            redirect.addFlashAttribute("mensajeError",
                    "Modo solo lectura: la sincronización está desactivada (vice.sincronizacion.habilitada=false).");
            return "redirect:/inconsistencias";
        }
        try {
            ResultadoSincronizacion resultado = (ticket == null)
                    ? repository.sincronizar()
                    : repository.sincronizarTicket(ticket);
            redirect.addFlashAttribute("mensaje", "Sincronización completada: "
                    + resultado.vice() + " tickets con Vice y " + resultado.cia() + " con Cía actualizados.");
        } catch (DataAccessException e) {
            log.error("Falló la sincronización de Vice/Cía (ticket={})", ticket, e);
            redirect.addFlashAttribute("mensajeError", "La sincronización falló; no se modificó ningún ticket.");
        }
        return "redirect:/inconsistencias";
    }
}
