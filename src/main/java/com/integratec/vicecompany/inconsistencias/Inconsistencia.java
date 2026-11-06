package com.integratec.vicecompany.inconsistencias;

/**
 * Un ticket con su Vicepresidencia/Compañía y la de su solicitante en el directorio (AD).
 * Un record es una clase de solo datos (como un dataclass(frozen=True) de Python).
 * Los valores se guardan tal como vienen de la base (pueden ser null).
 *
 * @param tieneCamposAdicionales false si el ticket no tiene fila en Request_Additional_Fields_1
 *                               (el UPDATE no tiene dónde escribir)
 * @param vice   Vicepresidencia del ticket (Request_Additional_Fields_1.UDF_CHAR20)
 * @param cia    Compañía del ticket (Request_Additional_Fields_1.UDF_CHAR19)
 * @param adVice Vicepresidencia del usuario en el directorio (UserAdditionalFields.UDF_CHAR2)
 * @param adCia  Compañía del usuario en el directorio (UserAdditionalFields.UDF_CHAR1)
 */
public record Inconsistencia(
        long ticket,
        Long userId,
        String fecha,
        String solicitante,
        boolean tieneCamposAdicionales,
        String vice,
        String cia,
        String adVice,
        String adCia) {

    static final String RELLENO_VICE = "vicepresidencia";
    static final String RELLENO_CIA = "company";

    /** Estado de un campo del ticket comparado con el AD. */
    public enum Caso {
        OK("bien"),
        VACIO("vacío"),
        RELLENO("con valor de relleno"),
        DISTINTO("distinto del AD");

        private final String texto;

        Caso(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }
    }

    public Caso casoVice() {
        return caso(vice, adVice, RELLENO_VICE);
    }

    public Caso casoCia() {
        return caso(cia, adCia, RELLENO_CIA);
    }

    /** El AD tiene un valor real que copiar (no vacío ni de relleno). */
    public boolean adViceValido() {
        return valido(adVice, RELLENO_VICE);
    }

    public boolean adCiaValido() {
        return valido(adCia, RELLENO_CIA);
    }

    public boolean sincronizado() {
        return casoVice() == Caso.OK && casoCia() == Caso.OK;
    }

    /** La sincronización lo deja bien: hay fila donde escribir y cada campo malo tiene dato en el AD. */
    public boolean corregible() {
        return tieneCamposAdicionales
                && (casoVice() == Caso.OK || adViceValido())
                && (casoCia() == Caso.OK || adCiaValido());
    }

    /** Texto como "Vice vacío; Cía distinto del AD (sin dato en AD)". */
    public String motivo() {
        if (!tieneCamposAdicionales) {
            return "Ticket sin campos adicionales";
        }
        if (sincronizado()) {
            return "Sincronizado";
        }
        StringBuilder motivo = new StringBuilder();
        agregar(motivo, "Vice", casoVice(), adViceValido());
        agregar(motivo, "Cía", casoCia(), adCiaValido());
        return motivo.toString();
    }

    // Para mostrar: vacío o nulo se ve como "N/D", igual que en la app JSP.
    public String viceTexto() {
        return nd(vice);
    }

    public String ciaTexto() {
        return nd(cia);
    }

    public String adViceTexto() {
        return nd(adVice);
    }

    public String adCiaTexto() {
        return nd(adCia);
    }

    private static void agregar(StringBuilder motivo, String campo, Caso caso, boolean adValido) {
        if (caso == Caso.OK) {
            return;
        }
        if (!motivo.isEmpty()) {
            motivo.append("; ");
        }
        motivo.append(campo).append(' ').append(caso.texto());
        if (!adValido) {
            motivo.append(" (sin dato en AD)");
        }
    }

    // Igual que SQL Server: sin distinguir mayúsculas y sin espacios al final.
    static Caso caso(String delTicket, String delAd, String relleno) {
        if (vacio(delTicket)) {
            return Caso.VACIO;
        }
        String t = delTicket.trim();
        if (t.equalsIgnoreCase(relleno)) {
            return Caso.RELLENO;
        }
        String ad = (delAd == null) ? "" : delAd.trim();
        return t.equalsIgnoreCase(ad) ? Caso.OK : Caso.DISTINTO;
    }

    static boolean valido(String valor, String relleno) {
        return !vacio(valor) && !valor.trim().equalsIgnoreCase(relleno);
    }

    static boolean vacio(String valor) {
        return valor == null || valor.isBlank() || valor.trim().equalsIgnoreCase("(null)");
    }

    private static String nd(String valor) {
        return (valor == null || valor.isBlank()) ? "N/D" : valor;
    }
}
