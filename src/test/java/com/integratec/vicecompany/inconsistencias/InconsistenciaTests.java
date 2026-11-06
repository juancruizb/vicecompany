package com.integratec.vicecompany.inconsistencias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.integratec.vicecompany.inconsistencias.Inconsistencia.Caso;

/** Cada caso que la consulta de la app JSP marca como inconsistente. Sin base de datos. */
class InconsistenciaTests {

    private static final String VICE = "VICEPRESIDENCIA TECNOLOGIA";
    private static final String CIA = "Seguros Universal";

    private static Inconsistencia ticket(long id, String vice, String cia, String adVice, String adCia) {
        return new Inconsistencia(id, 1L, "06 Nov 2026", "Prueba", true, vice, cia, adVice, adCia);
    }

    @Test
    void camposVaciosConDatoEnAdSeCorrigen() {
        var i = ticket(1, null, "", VICE, CIA);
        assertThat(i.casoVice()).isEqualTo(Caso.VACIO);
        assertThat(i.casoCia()).isEqualTo(Caso.VACIO);
        assertThat(i.corregible()).isTrue();
        assertThat(i.motivo()).isEqualTo("Vice vacío; Cía vacío");
    }

    @Test
    void textoNullEntreParentesisCuentaComoVacio() {
        assertThat(ticket(1, "(null)", CIA, VICE, CIA).casoVice()).isEqualTo(Caso.VACIO);
    }

    @Test
    void valoresDeRellenoEnElTicketSeCorrigen() {
        var i = ticket(1, "vicepresidencia", "company", VICE, CIA);
        assertThat(i.casoVice()).isEqualTo(Caso.RELLENO);
        assertThat(i.casoCia()).isEqualTo(Caso.RELLENO);
        assertThat(i.corregible()).isTrue();
    }

    @Test
    void valorDistintoDelAdSeCorrige() {
        var i = ticket(1, "VICEPRESIDENCIA TECNICA", CIA, VICE, CIA);
        assertThat(i.casoVice()).isEqualTo(Caso.DISTINTO);
        assertThat(i.casoCia()).isEqualTo(Caso.OK);
        assertThat(i.motivo()).isEqualTo("Vice distinto del AD");
        assertThat(i.corregible()).isTrue();
    }

    @Test
    void siElAdTieneRellenoNoSeCorrige() {
        // Filas 2, 4 y 8 de gui.png: el AD dice "vicepresidencia" / "company".
        var i = ticket(1, null, null, "vicepresidencia", "company");
        assertThat(i.corregible()).isFalse();
        assertThat(i.motivo()).isEqualTo("Vice vacío (sin dato en AD); Cía vacío (sin dato en AD)");
    }

    @Test
    void siSoloUnCampoDelAdFaltaNoQuedaCorregido() {
        var i = ticket(1, null, null, VICE, null);
        assertThat(i.adViceValido()).isTrue();
        assertThat(i.adCiaValido()).isFalse();
        assertThat(i.corregible()).isFalse();
    }

    @Test
    void ticketSinFilaDeCamposAdicionalesNoSeCorrige() {
        var i = new Inconsistencia(1, 1L, "06 Nov 2026", "Prueba", false, null, null, VICE, CIA);
        assertThat(i.corregible()).isFalse();
        assertThat(i.motivo()).isEqualTo("Ticket sin campos adicionales");
    }

    @Test
    void comparaComoSqlServerSinMayusculasNiEspaciosFinales() {
        var i = ticket(1, "vicepresidencia tecnologia  ", "SEGUROS UNIVERSAL", VICE, CIA);
        assertThat(i.sincronizado()).isTrue();
        assertThat(i.motivo()).isEqualTo("Sincronizado");
    }

    @Test
    void muestraND() {
        var i = ticket(1, null, " ", VICE, CIA);
        assertThat(i.viceTexto()).isEqualTo("N/D");
        assertThat(i.ciaTexto()).isEqualTo("N/D");
    }

    @Test
    void resumenAgrupaPorMotivoYOrdenaPorCantidad() {
        var lista = List.of(
                ticket(10, null, null, VICE, CIA),
                ticket(11, null, null, VICE, CIA),
                ticket(12, null, null, VICE, CIA),
                ticket(13, null, null, VICE, CIA),
                ticket(20, null, null, "vicepresidencia", "company"));

        var resumen = ResumenCandidatos.agrupar(lista);

        assertThat(resumen).hasSize(2);
        assertThat(resumen.get(0).motivo()).isEqualTo("Vice vacío; Cía vacío");
        assertThat(resumen.get(0).cantidad()).isEqualTo(4);
        assertThat(resumen.get(0).corregible()).isTrue();
        assertThat(resumen.get(0).ejemplos()).containsExactly(10L, 11L, 12L);
        assertThat(resumen.get(1).corregible()).isFalse();
        assertThat(resumen.get(1).ejemplos()).containsExactly(20L);
    }
}
