package pe.captabot.api.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Chat de Telegram de un vendedor que recibe los avisos de prospectos (HU13). Tabla: avisos_telegram. */
@Entity
@Table(name = "avisos_telegram")
public class AvisoTelegram {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected AvisoTelegram() {
    }

    public AvisoTelegram(Long chatId, String nombre) {
        this.chatId = chatId;
        this.nombre = nombre;
    }

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    public Long getChatId() { return chatId; }

    public String getNombre() { return nombre; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
}
