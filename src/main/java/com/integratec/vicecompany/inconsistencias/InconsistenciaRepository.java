package com.integratec.vicecompany.inconsistencias;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inconsistencias de Vice/Cía en la base de ServiceDesk Plus.
 * buscarTodas() usa la misma consulta que Consultas.getInconsistencies() de la app JSP;
 * sincronizar() hace la actualización que el JSP hacía al abrir la página.
 */
@Repository
public class InconsistenciaRepository {

    // CREATEDTIME son milisegundos desde 1970 (UTC). Sumarlos a '19691231 20:00'
    // da la fecha en hora local UTC-4, igual que en la app JSP.
    private static final String SQL = """
            SELECT wo.WORKORDERID,
                   aau.USER_ID,
                   CONVERT(VARCHAR(11), DATEADD(SECOND, wo.CREATEDTIME / 1000, '19691231 20:00'), 106) AS FECHA,
                   aau.FIRST_NAME,
                   raf1.UDF_CHAR20,
                   raf1.UDF_CHAR19,
                   requaf.UDF_CHAR2,
                   requaf.UDF_CHAR1
              FROM WorkOrder wo
              LEFT JOIN SDUser sdu ON wo.REQUESTERID = sdu.USERID
              LEFT JOIN AaaUser aau ON sdu.USERID = aau.USER_ID
              LEFT JOIN Request_Additional_Fields_1 raf1 ON wo.WORKORDERID = raf1.WORKORDERID
              LEFT JOIN UserAdditionalFields requaf ON sdu.USERID = requaf.INSTANCE_ID
             WHERE raf1.UDF_CHAR20 IS NULL
                OR raf1.UDF_CHAR19 IS NULL
                OR raf1.UDF_CHAR20 IN ('', '(null)', 'vicepresidencia')
                OR raf1.UDF_CHAR19 IN ('', '(null)', 'company')
                OR ISNULL(raf1.UDF_CHAR20, '') <> ISNULL(requaf.UDF_CHAR2, '')
                OR ISNULL(raf1.UDF_CHAR19, '') <> ISNULL(requaf.UDF_CHAR1, '')
             ORDER BY wo.WORKORDERID DESC
            """;

    // Copia Vice/Cía del solicitante (UserAdditionalFields) a los campos adicionales del
    // ticket, como hacía el JSP fila por fila. Diferencia: no copia valores vacíos ni de
    // relleno ('vicepresidencia', 'company'), que dejaban el ticket inconsistente para siempre.
    private static final String ACTUALIZAR_VICE = """
            UPDATE raf1
               SET raf1.UDF_CHAR20 = requaf.UDF_CHAR2
              FROM Request_Additional_Fields_1 raf1
              JOIN WorkOrder wo ON wo.WORKORDERID = raf1.WORKORDERID
              JOIN UserAdditionalFields requaf ON wo.REQUESTERID = requaf.INSTANCE_ID
             WHERE LTRIM(RTRIM(ISNULL(requaf.UDF_CHAR2, ''))) NOT IN ('', '(null)', 'vicepresidencia')
               AND ISNULL(raf1.UDF_CHAR20, '') <> requaf.UDF_CHAR2
            """;

    private static final String ACTUALIZAR_CIA = """
            UPDATE raf1
               SET raf1.UDF_CHAR19 = requaf.UDF_CHAR1
              FROM Request_Additional_Fields_1 raf1
              JOIN WorkOrder wo ON wo.WORKORDERID = raf1.WORKORDERID
              JOIN UserAdditionalFields requaf ON wo.REQUESTERID = requaf.INSTANCE_ID
             WHERE LTRIM(RTRIM(ISNULL(requaf.UDF_CHAR1, ''))) NOT IN ('', '(null)', 'company')
               AND ISNULL(raf1.UDF_CHAR19, '') <> requaf.UDF_CHAR1
            """;

    private final JdbcClient jdbc;

    // Spring entrega el JdbcClient ya configurado con los datos de application.properties.
    public InconsistenciaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Inconsistencia> buscarTodas() {
        return jdbc.sql(SQL).query(InconsistenciaRepository::mapear).list();
    }

    /**
     * Actualiza los tickets. @Transactional: si un UPDATE falla, se deshacen los dos
     * (como envolver el código en transaction.atomic() en Django).
     */
    @Transactional
    public ResultadoSincronizacion sincronizar() {
        int vice = jdbc.sql(ACTUALIZAR_VICE).update();
        int cia = jdbc.sql(ACTUALIZAR_CIA).update();
        return new ResultadoSincronizacion(vice, cia);
    }

    /** Cuántos tickets recibieron Vice y cuántos Cía. */
    public record ResultadoSincronizacion(int vice, int cia) {
    }

    private static Inconsistencia mapear(ResultSet rs, int fila) throws SQLException {
        return new Inconsistencia(
                rs.getLong("WORKORDERID"),
                rs.getObject("USER_ID", Long.class),
                rs.getString("FECHA"),
                rs.getString("FIRST_NAME"),
                nd(rs.getString("UDF_CHAR20")),
                nd(rs.getString("UDF_CHAR19")),
                nd(rs.getString("UDF_CHAR2")),
                nd(rs.getString("UDF_CHAR1")));
    }

    /** Valor vacío o nulo se muestra como "N/D", igual que en la app JSP. */
    private static String nd(String valor) {
        return (valor == null || valor.isBlank()) ? "N/D" : valor;
    }
}
