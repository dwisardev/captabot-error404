package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/** Acceso a la tabla prospectos. Los filtros de la lista se arman en ProspectoService. */
public interface ProspectoRepository extends JpaRepository<Prospecto, Long>, JpaSpecificationExecutor<Prospecto> {

    // ------------------------------------------------------------------ automatización (HU13)

    /** Prospectos que siguen en una etapa sin recordatorio, registrados entre desde y hasta. */
    @Query("""
            select p from Prospecto p
            where p.etapa = :etapa and p.calificacion in :calificaciones and p.recordatorioEn is null
              and p.creadoEn > :desde and p.creadoEn <= :hasta
            order by p.creadoEn""")
    List<Prospecto> pendientesDeRecordatorio(@Param("etapa") Etapa etapa,
                                             @Param("calificaciones") Collection<Calificacion> calificaciones,
                                             @Param("desde") OffsetDateTime desde,
                                             @Param("hasta") OffsetDateTime hasta);

    /** Clientes de Telegram o WhatsApp sin mensaje de seguimiento cuya venta sigue abierta. */
    @Query("""
            select p from Prospecto p
            where (p.telegramChatId is not null or p.whatsappId is not null)
              and p.seguimientoClienteEn is null and p.etapa not in :cerradas
              and p.creadoEn > :desde and p.creadoEn <= :hasta
            order by p.creadoEn""")
    List<Prospecto> pendientesDeSeguimiento(@Param("cerradas") Collection<Etapa> cerradas,
                                            @Param("desde") OffsetDateTime desde,
                                            @Param("hasta") OffsetDateTime hasta);

    // Se actualiza solo la columna, para no pisar un cambio de etapa hecho al mismo tiempo desde el panel.

    @Transactional
    @Modifying
    @Query("update Prospecto p set p.avisoEnviadoEn = :momento where p.id = :id")
    int marcarAvisoEnviado(@Param("id") Long id, @Param("momento") OffsetDateTime momento);

    @Transactional
    @Modifying
    @Query("update Prospecto p set p.recordatorioEn = :momento where p.id = :id")
    int marcarRecordatorio(@Param("id") Long id, @Param("momento") OffsetDateTime momento);

    @Transactional
    @Modifying
    @Query("update Prospecto p set p.seguimientoClienteEn = :momento where p.id = :id")
    int marcarSeguimiento(@Param("id") Long id, @Param("momento") OffsetDateTime momento);

    @Query("select count(p) from Prospecto p where p.avisoEnviadoEn >= :desde")
    long avisosDesde(@Param("desde") OffsetDateTime desde);

    @Query("select count(p) from Prospecto p where p.recordatorioEn >= :desde")
    long recordatoriosDesde(@Param("desde") OffsetDateTime desde);

    @Query("select count(p) from Prospecto p where p.seguimientoClienteEn >= :desde")
    long seguimientosDesde(@Param("desde") OffsetDateTime desde);

    // ------------------------------------------------------------------ indicadores y avisos del panel

    @Query("select p.canal, count(p) from Prospecto p group by p.canal")
    List<Object[]> contarPorCanal();

    @Query("select p.calificacion, count(p) from Prospecto p group by p.calificacion")
    List<Object[]> contarPorCalificacion();

    long countByCalificacionAndEtapa(Calificacion calificacion, Etapa etapa);

    long countByCreadoEnGreaterThanEqual(OffsetDateTime desde);

    /** Para la campana del panel: los registrados después del id indicado, del más nuevo al más antiguo. */
    List<Prospecto> findTop20ByIdGreaterThanOrderByIdDesc(Long id);

    @Query("select coalesce(max(p.id), 0) from Prospecto p")
    long ultimoId();
}
