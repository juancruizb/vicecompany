package com.integratec.vicecompany.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.integratec.vicecompany.inconsistencias.Inconsistencia;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository;
import com.integratec.vicecompany.inconsistencias.SincronizacionAutomatica;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository.ResultadoSincronizacion;

/** Prueba la página sin base de datos real: el repositorio se reemplaza por un mock. */
@WebMvcTest(controllers = InconsistenciaController.class,
        properties = "vice.sincronizacion.habilitada=true")
@WithMockUser
class InconsistenciaControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InconsistenciaRepository repository;

    @MockitoBean
    private SincronizacionAutomatica automatica;

    @Test
    void muestraLasInconsistencias() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of(
                new Inconsistencia(164738L, 42L, "06 Nov 2026", "Maria Prueba", true,
                        null, null, "VICEPRESIDENCIA TECNOLOGIA", "Seguros Universal")));

        mvc.perform(get("/inconsistencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 inconsistencias encontradas")))
                .andExpect(content().string(containsString("woID=164738")))
                .andExpect(content().string(containsString("VICEPRESIDENCIA TECNOLOGIA")))
                .andExpect(content().string(containsString("Resumen de candidatos")))
                .andExpect(content().string(containsString("<details class=\"resumen\" id=\"resumen\" open")))
                .andExpect(content().string(containsString("/js/resumen.js")))
                .andExpect(content().string(containsString("Vice vacío; Cía vacío")))
                .andExpect(content().string(containsString("Sincronizar todo")));
    }

    @Test
    void verUnTicketMuestraSoloEseYNoOfreceElLote() throws Exception {
        given(repository.buscarTicket(164738L)).willReturn(Optional.of(
                new Inconsistencia(164738L, 42L, "06 Nov 2026", "Maria Prueba", true,
                        null, null, "VICEPRESIDENCIA TECNOLOGIA", "Seguros Universal")));

        mvc.perform(get("/inconsistencias").param("ticket", "164738"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ticket 164738")))
                .andExpect(content().string(containsString("Sincronizar este ticket")))
                .andExpect(content().string(not(containsString("Sincronizar todo"))));

        then(repository).should(never()).buscarTodas();
    }

    @Test
    void ticketInexistente() throws Exception {
        given(repository.buscarTicket(999L)).willReturn(Optional.empty());

        mvc.perform(get("/inconsistencias").param("ticket", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No existe el ticket 999")))
                .andExpect(content().string(not(containsString("Sincronizar este ticket"))));
    }

    @Test
    void sincronizarUnTicketNoTocaLosDemas() throws Exception {
        given(repository.sincronizarTicket(164738L)).willReturn(new ResultadoSincronizacion(1, 1));

        mvc.perform(post("/inconsistencias/sincronizar").param("ticket", "164738").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inconsistencias?ticket=164738"))
                .andExpect(flash().attribute("mensaje", containsString("1 tickets con Vice y 1 con Cía")));

        then(repository).should(never()).sincronizar();
    }

    @Test
    void muestraElEstadoDeLaSincronizacionAutomatica() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of());
        given(automatica.activa()).willReturn(true);
        given(automatica.cron()).willReturn("0 0 * * * *");
        given(automatica.ultima()).willReturn(Optional.of(new SincronizacionAutomatica.Ejecucion(
                LocalDateTime.of(2026, 11, 6, 19, 0), true, "0 tickets con Vice y 2 con Cía actualizados")));

        mvc.perform(get("/inconsistencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sincronización automática activa")))
                .andExpect(content().string(containsString("06/11/2026 19:00")))
                .andExpect(content().string(containsString("0 tickets con Vice y 2 con Cía actualizados")));
    }

    @Test
    void abrirLaPaginaNoModificaTickets() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of());

        mvc.perform(get("/inconsistencias")).andExpect(status().isOk());

        then(repository).should(never()).sincronizar();
    }

    @Test
    void sincronizarTodoActualizaYRedirige() throws Exception {
        given(repository.sincronizar()).willReturn(new ResultadoSincronizacion(3, 2));

        mvc.perform(post("/inconsistencias/sincronizar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inconsistencias"))
                .andExpect(flash().attribute("mensaje", containsString("3 tickets con Vice y 2 con Cía")));
    }

    @Test
    void sincronizarSinTokenCsrfEsRechazado() throws Exception {
        mvc.perform(post("/inconsistencias/sincronizar")).andExpect(status().isForbidden());

        then(repository).should(never()).sincronizar();
    }

    @Test
    void muestraUnMensajeSiLaBaseNoResponde() throws Exception {
        given(repository.buscarTodas()).willThrow(new DataAccessResourceFailureException("sin conexión"));

        mvc.perform(get("/inconsistencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo conectar a la base de datos")));
    }
}
