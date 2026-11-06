package com.integratec.vicecompany.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.integratec.vicecompany.inconsistencias.Inconsistencia;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository;
import com.integratec.vicecompany.inconsistencias.SincronizacionAutomatica;

/** Con vice.sincronizacion.habilitada=false nada puede escribir en la base. */
@WebMvcTest(controllers = InconsistenciaController.class,
        properties = "vice.sincronizacion.habilitada=false")
@WithMockUser
class InconsistenciaSoloLecturaTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InconsistenciaRepository repository;

    @MockitoBean
    private SincronizacionAutomatica automatica;

    private static Inconsistencia ejemplo() {
        return new Inconsistencia(164738L, 42L, "06 Nov 2026", "Maria Prueba", true,
                null, null, "VICEPRESIDENCIA TECNOLOGIA", "Seguros Universal");
    }

    @Test
    void laListaNoMuestraBotonesDeSincronizar() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of(ejemplo()));

        mvc.perform(get("/inconsistencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Modo solo lectura")))
                .andExpect(content().string(containsString("Resumen de candidatos")))
                .andExpect(content().string(not(containsString("Sincronizar todo"))));
    }

    @Test
    void elTicketNoMuestraBotonDeSincronizar() throws Exception {
        given(repository.buscarTicket(164738L)).willReturn(Optional.of(ejemplo()));

        mvc.perform(get("/inconsistencias").param("ticket", "164738"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Sincronizar este ticket"))));
    }

    @Test
    void unPostEnviadoAManoNoModificaNada() throws Exception {
        mvc.perform(post("/inconsistencias/sincronizar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeError", containsString("solo lectura")));
        mvc.perform(post("/inconsistencias/sincronizar").param("ticket", "164738").with(csrf()))
                .andExpect(status().is3xxRedirection());

        then(repository).should(never()).sincronizar();
        then(repository).should(never()).sincronizarTicket(anyLong());
    }
}
