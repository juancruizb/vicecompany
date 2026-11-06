package com.integratec.vicecompany.inconsistencias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.integratec.vicecompany.inconsistencias.InconsistenciaRepository.ResultadoSincronizacion;

/** La tarea programada, sin Spring ni base de datos: el repositorio es un mock. */
class SincronizacionAutomaticaTests {

    private final InconsistenciaRepository repository = mock(InconsistenciaRepository.class);

    @Test
    void conCronGuionEstaApagada() {
        var tarea = new SincronizacionAutomatica(repository, true, "-");
        assertThat(tarea.activa()).isFalse();
        assertThat(tarea.ultima()).isEmpty();
    }

    @Test
    void enSoloLecturaNoEscribe() {
        var tarea = new SincronizacionAutomatica(repository, false, "0 0 * * * *");

        tarea.ejecutar();

        then(repository).should(never()).sincronizar();
        assertThat(tarea.ultima()).get()
                .satisfies(e -> {
                    assertThat(e.exitosa()).isFalse();
                    assertThat(e.resultado()).contains("solo lectura");
                });
    }

    @Test
    void habilitadaSincronizaTodoYGuardaElResultado() {
        given(repository.sincronizar()).willReturn(new ResultadoSincronizacion(4, 7));
        var tarea = new SincronizacionAutomatica(repository, true, "0 0 * * * *");

        tarea.ejecutar();

        then(repository).should().sincronizar();
        assertThat(tarea.activa()).isTrue();
        assertThat(tarea.ultima()).get()
                .satisfies(e -> {
                    assertThat(e.exitosa()).isTrue();
                    assertThat(e.resultado()).isEqualTo("4 tickets con Vice y 7 con Cía actualizados");
                });
    }

    @Test
    void siLaBaseFallaLoRegistraSinRomperLaTarea() {
        given(repository.sincronizar()).willThrow(new DataAccessResourceFailureException("sin conexión"));
        var tarea = new SincronizacionAutomatica(repository, true, "0 0 * * * *");

        tarea.ejecutar();

        assertThat(tarea.ultima()).get()
                .satisfies(e -> assertThat(e.exitosa()).isFalse());
    }
}
