package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/** Acceso a la tabla prospectos. Los filtros de la lista se arman en ProspectoService. */
public interface ProspectoRepository extends JpaRepository<Prospecto, Long>, JpaSpecificationExecutor<Prospecto> {

    /** HU13: prospectos que siguen en una etapa sin recordatorio, registrados entre desde y hasta. */
    @Query("""
            select p from Prospecto p
            where p.etapa = :etapa and p.calificacion in :calificaciones and p.recordatorioEn is null
              and p.creadoEn > :desde and p.creadoEn <= :hasta
            order by p.creadoEn""")
    List<Prospecto> pendientesDeRecordatorio(@Param("etapa") Etapa etapa,
                                             @Param("calificaciones") Collection<Calificacion> calificaciones,
                                             @Param("desde") OffsetDateTime desde,
                                             @Param("hasta") OffsetDateTime hasta);

    /** HU13: clientes de Telegram sin mensaje de seguimiento cuya venta sigue abierta. */
    @Query("""
            select p from Prospecto p
            where p.telegramChatId is not null and p.seguimientoClienteEn is null and p.etapa not in :cerradas
              and p.creadoEn > :desde and p.creadoEn <= :hasta
            order by p.creadoEn""")
    List<Prospecto> pendientesDeSeguimiento(@Param("cerradas") Collection<Etapa> cerradas,
                                            @Param("desde") OffsetDateTime desde,
                                            @Param("hasta") OffsetDateTime hasta);
}
