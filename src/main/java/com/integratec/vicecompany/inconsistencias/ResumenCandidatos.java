package com.integratec.vicecompany.inconsistencias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrupa las inconsistencias por motivo, con algunos tickets de ejemplo de cada grupo,
 * para elegir candidatos y probar la sincronización ticket por ticket antes del lote.
 */
public final class ResumenCandidatos {

    static final int EJEMPLOS_POR_GRUPO = 3;

    public record Grupo(String motivo, boolean corregible, int cantidad, List<Long> ejemplos) {
    }

    private ResumenCandidatos() {
    }

    /** Como un GROUP BY motivo, corregible ... ORDER BY COUNT(*) DESC. */
    public static List<Grupo> agrupar(List<Inconsistencia> inconsistencias) {
        record Clave(String motivo, boolean corregible) {
        }
        Map<Clave, List<Long>> grupos = new LinkedHashMap<>();
        for (Inconsistencia i : inconsistencias) {
            grupos.computeIfAbsent(new Clave(i.motivo(), i.corregible()), k -> new ArrayList<>())
                    .add(i.ticket());
        }
        return grupos.entrySet().stream()
                .map(e -> new Grupo(e.getKey().motivo(), e.getKey().corregible(), e.getValue().size(),
                        e.getValue().stream().limit(EJEMPLOS_POR_GRUPO).toList()))
                .sorted(Comparator.comparingInt(Grupo::cantidad).reversed())
                .toList();
    }
}
