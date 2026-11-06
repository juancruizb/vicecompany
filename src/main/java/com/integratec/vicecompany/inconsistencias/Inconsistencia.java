package com.integratec.vicecompany.inconsistencias;

/**
 * Un ticket cuya Vicepresidencia/Compañía no coincide con la del solicitante en el directorio.
 * Un record es una clase de solo datos (como un dataclass(frozen=True) de Python).
 *
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
        String vice,
        String cia,
        String adVice,
        String adCia) {
}
