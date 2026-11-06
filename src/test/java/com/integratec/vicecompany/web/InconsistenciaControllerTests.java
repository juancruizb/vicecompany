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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.integratec.vicecompany.inconsistencias.Inconsistencia;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository;
import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository.ResultadoSincronizacion;

/** Prueba la página sin base de datos real: el repositorio se reemplaza por un mock. */
@WebMvcTest(InconsistenciaController.class)
@WithMockUser
class InconsistenciaControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InconsistenciaRepository repository;

    @Test
    void muestraLasInconsistencias() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of(
                new Inconsistencia(164738L, 42L, "06 Nov 2026", "Maria Prueba",
                        "N/D", "N/D", "VICEPRESIDENCIA TECNOLOGIA", "Seguros Universal")));

        mvc.perform(get("/inconsistencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 inconsistencias encontradas")))
                .andExpect(content().string(containsString("woID=164738")))
                .andExpect(content().string(containsString("VICEPRESIDENCIA TECNOLOGIA")));
    }

    @Test
    void abrirLaPaginaNoModificaTickets() throws Exception {
        given(repository.buscarTodas()).willReturn(List.of());

        mvc.perform(get("/inconsistencias")).andExpect(status().isOk());

        then(repository).should(never()).sincronizar();
    }

    @Test
    void sincronizarActualizaYRedirige() throws Exception {
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
