package com.integratec.vicecompany.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository;

@Controller
public class InconsistenciaController {

    private static final Logger log = LoggerFactory.getLogger(InconsistenciaController.class);

    private final InconsistenciaRepository repository;
    private final String servicedeskUrl;

    public InconsistenciaController(InconsistenciaRepository repository,
                                    @Value("${vice.servicedesk.url}") String servicedeskUrl) {
        this.repository = repository;
        this.servicedeskUrl = servicedeskUrl;
    }

    /** Solo muestra; a diferencia del JSP, abrir la página no modifica la base. */
    @GetMapping("/inconsistencias")
    public String inconsistencias(Model model) {
        model.addAttribute("servicedeskUrl", servicedeskUrl);
        try {
            model.addAttribute("inconsistencias", repository.buscarTodas());
        } catch (DataAccessException e) {
            log.error("No se pudo consultar la base de ServiceDesk Plus", e);
            model.addAttribute("error", "No se pudo conectar a la base de datos. Revisa application-local.properties.");
        }
        return "inconsistencias";
    }

    /**
     * Corrige los tickets. Va por POST (con token CSRF) para que solo se ejecute al pulsar
     * el botón. Después redirige al GET (patrón Post/Redirect/Get, igual que en Django).
     */
    @PostMapping("/inconsistencias/sincronizar")
    public String sincronizar(RedirectAttributes redirect) {
        try {
            var resultado = repository.sincronizar();
            redirect.addFlashAttribute("mensaje", "Sincronización completada: "
                    + resultado.vice() + " tickets con Vice y " + resultado.cia() + " con Cía actualizados.");
        } catch (DataAccessException e) {
            log.error("Falló la sincronización de Vice/Cía", e);
            redirect.addFlashAttribute("mensajeError", "La sincronización falló; no se modificó ningún ticket.");
        }
        return "redirect:/inconsistencias";
    }
}
