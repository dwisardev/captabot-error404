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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Prospecto captado por el asistente virtual. Tabla: prospectos. */
@Entity
@Table(name = "prospectos")
public class Prospecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 9)
    private String telefono;

    @Column(name = "servicio_interes", nullable = false, length = 120)
    private String servicioInteres;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal presupuesto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgencia urgencia;

    @Column(nullable = false)
    private Integer puntaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Calificacion calificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Etapa etapa = Etapa.NUEVO;

    @Column(nullable = false, length = 30)
    private String canal = "CHAT_WEB";

    @Column(name = "vendedor_id")
    private Long vendedorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getServicioInteres() { return servicioInteres; }
    public void setServicioInteres(String servicioInteres) { this.servicioInteres = servicioInteres; }

    public BigDecimal getPresupuesto() { return presupuesto; }
    public void setPresupuesto(BigDecimal presupuesto) { this.presupuesto = presupuesto; }

    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia urgencia) { this.urgencia = urgencia; }

    public Integer getPuntaje() { return puntaje; }
    public void setPuntaje(Integer puntaje) { this.puntaje = puntaje; }

    public Calificacion getCalificacion() { return calificacion; }
    public void setCalificacion(Calificacion calificacion) { this.calificacion = calificacion; }

    public Etapa getEtapa() { return etapa; }
    public void setEtapa(Etapa etapa) { this.etapa = etapa; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public Long getVendedorId() { return vendedorId; }
    public void setVendedorId(Long vendedorId) { this.vendedorId = vendedorId; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
}
