package pe.captabot.api.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Cada cambio de etapa de un prospecto, con el usuario y la fecha (HU09). Tabla: historial_etapas. */
@Entity
@Table(name = "historial_etapas")
public class HistorialEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prospecto_id", nullable = false)
    private Long prospectoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_anterior", length = 20)
    private Etapa etapaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_nueva", nullable = false, length = 20)
    private Etapa etapaNueva;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "cambiado_en", nullable = false, updatable = false)
    private OffsetDateTime cambiadoEn;

    @PrePersist
    void alCrear() {
        if (cambiadoEn == null) {
            cambiadoEn = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProspectoId() { return prospectoId; }
    public void setProspectoId(Long prospectoId) { this.prospectoId = prospectoId; }

    public Etapa getEtapaAnterior() { return etapaAnterior; }
    public void setEtapaAnterior(Etapa etapaAnterior) { this.etapaAnterior = etapaAnterior; }

    public Etapa getEtapaNueva() { return etapaNueva; }
    public void setEtapaNueva(Etapa etapaNueva) { this.etapaNueva = etapaNueva; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public OffsetDateTime getCambiadoEn() { return cambiadoEn; }
    public void setCambiadoEn(OffsetDateTime cambiadoEn) { this.cambiadoEn = cambiadoEn; }
}
