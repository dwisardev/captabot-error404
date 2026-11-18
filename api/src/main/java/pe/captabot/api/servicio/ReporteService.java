package pe.captabot.api.servicio;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Reportes para el gerente: prospectos por semana (HU11), embudo y tasa de conversión (HU12). */
@Service
public class ReporteService {

    static final int SEMANAS_POR_DEFECTO = 8;
    static final int SEMANAS_MAXIMO = 52;

    /** Rango amplio para cuando no se filtra por fechas. */
    private static final LocalDate DESDE_SIN_FILTRO = LocalDate.of(2000, 1, 1);
    private static final LocalDate HASTA_SIN_FILTRO = LocalDate.of(2099, 12, 31);

    private static final String SQL_SEMANAL = """
            WITH semanas AS (
                SELECT (DATE_TRUNC('week', NOW() AT TIME ZONE 'America/Lima')::DATE - 7 * s) AS semana
                FROM GENERATE_SERIES(0, :semanas - 1) AS s
            )
            SELECT se.semana,
                   COUNT(p.id)                                           AS total,
                   COUNT(p.id) FILTER (WHERE p.calificacion = 'CALIENTE') AS calientes,
                   COUNT(p.id) FILTER (WHERE p.calificacion = 'TIBIO')    AS tibios,
                   COUNT(p.id) FILTER (WHERE p.calificacion = 'FRIO')     AS frios
            FROM semanas se
            LEFT JOIN prospectos p
                   ON DATE_TRUNC('week', p.creado_en AT TIME ZONE 'America/Lima')::DATE = se.semana
            GROUP BY se.semana
            ORDER BY se.semana
            """;

    private static final String SQL_EMBUDO = """
            SELECT e.etapa, COUNT(p.id) AS total
            FROM (VALUES ('NUEVO', 1), ('CONTACTADO', 2), ('COTIZADO', 3),
                         ('CERRADO_GANADO', 4), ('CERRADO_PERDIDO', 5)) AS e (etapa, orden)
            LEFT JOIN prospectos p
                   ON p.etapa = e.etapa AND p.creado_en >= :desde AND p.creado_en < :hasta
            GROUP BY e.etapa, e.orden
            ORDER BY e.orden
            """;

    private static final String SQL_CONVERSION = """
            SELECT COUNT(*)                                         AS total,
                   COUNT(*) FILTER (WHERE etapa = 'CERRADO_GANADO') AS ganados
            FROM prospectos
            WHERE creado_en >= :desde AND creado_en < :hasta
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ReporteService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Prospectos por semana de las últimas semanas, incluidas las semanas sin prospectos. */
    @Transactional(readOnly = true)
    public List<ReporteSemanal> porSemana(int semanas) {
        if (semanas < 1 || semanas > SEMANAS_MAXIMO) {
            throw new IllegalArgumentException("Las semanas deben estar entre 1 y " + SEMANAS_MAXIMO);
        }
        return jdbc.query(SQL_SEMANAL, new MapSqlParameterSource("semanas", semanas),
                (rs, i) -> new ReporteSemanal(
                        rs.getObject("semana", LocalDate.class),
                        rs.getLong("total"),
                        rs.getLong("calientes"),
                        rs.getLong("tibios"),
                        rs.getLong("frios")));
    }

    @Transactional(readOnly = true)
    public List<EtapaEmbudo> embudo(LocalDate desde, LocalDate hasta) {
        return jdbc.query(SQL_EMBUDO, rango(desde, hasta),
                (rs, i) -> new EtapaEmbudo(rs.getString("etapa"), rs.getLong("total")));
    }

    @Transactional(readOnly = true)
    public TasaConversion conversion(LocalDate desde, LocalDate hasta) {
        return jdbc.queryForObject(SQL_CONVERSION, rango(desde, hasta), (rs, i) -> {
            long total = rs.getLong("total");
            long ganados = rs.getLong("ganados");
            return new TasaConversion(total, ganados, calcularTasa(ganados, total));
        });
    }

    /** Porcentaje con dos decimales; 0 si no hay prospectos. */
    static BigDecimal calcularTasa(long ganados, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(ganados * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static MapSqlParameterSource rango(LocalDate desde, LocalDate hasta) {
        Fechas.validarRango(desde, hasta);
        OffsetDateTime inicio = Fechas.inicioDelDia(desde == null ? DESDE_SIN_FILTRO : desde);
        OffsetDateTime fin = Fechas.inicioDelDia((hasta == null ? HASTA_SIN_FILTRO : hasta).plusDays(1));
        return new MapSqlParameterSource().addValue("desde", inicio).addValue("hasta", fin);
    }
}
