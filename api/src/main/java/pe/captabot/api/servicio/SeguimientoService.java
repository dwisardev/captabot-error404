package pe.captabot.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.HistorialEtapa;
import pe.captabot.api.modelo.NotaSeguimiento;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Usuario;
import pe.captabot.api.repositorio.HistorialEtapaRepository;
import pe.captabot.api.repositorio.NotaSeguimientoRepository;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.repositorio.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cambio de etapa (HU09) y notas de seguimiento (HU10). */
@Service
public class SeguimientoService {

    private final ProspectoRepository prospectos;
    private final UsuarioRepository usuarios;
    private final HistorialEtapaRepository historial;
    private final NotaSeguimientoRepository notas;

    public SeguimientoService(ProspectoRepository prospectos, UsuarioRepository usuarios,
                              HistorialEtapaRepository historial, NotaSeguimientoRepository notas) {
        this.prospectos = prospectos;
        this.usuarios = usuarios;
        this.historial = historial;
        this.notas = notas;
    }

    /** Cambia la etapa y guarda en el historial quién y cuándo lo hizo. */
    @Transactional
    public Prospecto cambiarEtapa(Long prospectoId, Etapa nueva, Long usuarioId) {
        Prospecto prospecto = buscarProspecto(prospectoId);
        validarUsuario(usuarioId);
        if (prospecto.getEtapa() == nueva) {
            throw new IllegalArgumentException("El prospecto ya está en la etapa " + nueva);
        }
        HistorialEtapa cambio = new HistorialEtapa();
        cambio.setProspectoId(prospectoId);
        cambio.setEtapaAnterior(prospecto.getEtapa());
        cambio.setEtapaNueva(nueva);
        cambio.setUsuarioId(usuarioId);
        historial.save(cambio);

        prospecto.setEtapa(nueva);
        return prospectos.save(prospecto);
    }

    @Transactional(readOnly = true)
    public List<HistorialEtapa> historial(Long prospectoId) {
        buscarProspecto(prospectoId);
        return historial.findByProspectoIdOrderByCambiadoEnDescIdDesc(prospectoId);
    }

    /** Notas de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<NotaSeguimiento> notas(Long prospectoId) {
        buscarProspecto(prospectoId);
        return notas.findByProspectoIdOrderByCreadoEnDescIdDesc(prospectoId);
    }

    @Transactional
    public NotaSeguimiento agregarNota(Long prospectoId, Long usuarioId, String contenido) {
        buscarProspecto(prospectoId);
        validarUsuario(usuarioId);
        NotaSeguimiento nota = new NotaSeguimiento();
        nota.setProspectoId(prospectoId);
        nota.setUsuarioId(usuarioId);
        nota.setContenido(contenido.trim());
        return notas.save(nota);
    }

    /** Solo el autor puede editar su nota. Las notas no se borran. */
    @Transactional
    public NotaSeguimiento editarNota(Long prospectoId, Long notaId, Long usuarioId, String contenido) {
        NotaSeguimiento nota = notas.findByIdAndProspectoId(notaId, prospectoId)
                .orElseThrow(() -> new NoEncontradoException("No existe la nota " + notaId));
        if (!Objects.equals(nota.getUsuarioId(), usuarioId)) {
            throw new AccesoDenegadoException("Solo el autor puede editar la nota");
        }
        nota.setContenido(contenido.trim());
        nota.setEditadoEn(OffsetDateTime.now());
        return notas.save(nota);
    }

    /** Nombres de los usuarios para mostrar al autor de cada nota o cambio. */
    @Transactional(readOnly = true)
    public Map<Long, String> nombresDeUsuarios(Collection<Long> ids) {
        List<Long> sinNulos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (sinNulos.isEmpty()) {
            return Map.of();
        }
        return usuarios.findAllById(sinNulos).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre, (a, b) -> a));
    }

    private Prospecto buscarProspecto(Long id) {
        return prospectos.findById(id).orElseThrow(() -> new ProspectoNoEncontradoException(id));
    }

    private void validarUsuario(Long usuarioId) {
        if (usuarioId == null || !usuarios.existsById(usuarioId)) {
            throw new IllegalArgumentException("No existe el usuario " + usuarioId);
        }
    }
}
