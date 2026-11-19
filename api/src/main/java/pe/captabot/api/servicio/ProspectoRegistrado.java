package pe.captabot.api.servicio;

import pe.captabot.api.modelo.Prospecto;

/** Evento que se publica al registrar un prospecto; lo usa la automatización de avisos (HU13). */
public record ProspectoRegistrado(Prospecto prospecto) {
}
