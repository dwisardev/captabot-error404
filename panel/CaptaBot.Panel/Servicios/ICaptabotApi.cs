using CaptaBot.Panel.Modelos;

namespace CaptaBot.Panel.Servicios;

/// <summary>Operaciones de la API de CaptaBot que usa el panel.</summary>
public interface ICaptabotApi
{
    /// <summary>Devuelve el usuario, o null si el correo o la contraseña son incorrectos (HU06).</summary>
    Task<Usuario?> IniciarSesionAsync(string correo, string contrasena, CancellationToken ct = default);

    Task<PaginaResultado<Prospecto>> ListarProspectosAsync(FiltroProspectos filtro, CancellationToken ct = default);

    /// <summary>Devuelve null si el prospecto no existe.</summary>
    Task<Prospecto?> ObtenerProspectoAsync(long id, CancellationToken ct = default);

    Task<Prospecto> CambiarEtapaAsync(long id, string etapa, long usuarioId, CancellationToken ct = default);

    Task<IReadOnlyList<CambioEtapa>> HistorialAsync(long id, CancellationToken ct = default);

    Task<IReadOnlyList<Nota>> NotasAsync(long id, CancellationToken ct = default);

    Task<Nota> AgregarNotaAsync(long id, long usuarioId, string contenido, CancellationToken ct = default);

    Task<Nota> EditarNotaAsync(long id, long notaId, long usuarioId, string contenido, CancellationToken ct = default);

    Task<IReadOnlyList<Usuario>> ListarUsuariosAsync(CancellationToken ct = default);

    /// <summary>Devuelve null si el usuario no existe.</summary>
    Task<Usuario?> ObtenerUsuarioAsync(long id, CancellationToken ct = default);

    Task<Usuario> CrearUsuarioAsync(UsuarioSolicitud solicitud, CancellationToken ct = default);

    Task<Usuario> ActualizarUsuarioAsync(long id, UsuarioSolicitud solicitud, CancellationToken ct = default);

    Task<Usuario> CambiarEstadoUsuarioAsync(long id, bool activo, CancellationToken ct = default);

    Task<IReadOnlyList<ReporteSemanal>> ReporteSemanalAsync(int semanas, CancellationToken ct = default);

    Task<IReadOnlyList<EtapaEmbudo>> EmbudoAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default);

    Task<TasaConversion> ConversionAsync(DateOnly? desde, DateOnly? hasta, CancellationToken ct = default);
}
