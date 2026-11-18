namespace CaptaBot.Panel.Modelos;

// Datos tal como los devuelve la API de CaptaBot (JSON en camelCase).

public sealed record Prospecto(
    long Id,
    string Nombre,
    string Telefono,
    string ServicioInteres,
    decimal Presupuesto,
    string Urgencia,
    int Puntaje,
    string Calificacion,
    string Etapa,
    string Canal,
    long? VendedorId,
    DateTimeOffset CreadoEn);

/// <summary>Una página de resultados. En la API la primera página es la 0.</summary>
public sealed record PaginaResultado<T>(
    IReadOnlyList<T> Contenido,
    int Pagina,
    int Tamano,
    long TotalElementos,
    int TotalPaginas);

public sealed record Usuario(long Id, string Nombre, string Correo, string Rol, bool Activo, DateTimeOffset? CreadoEn);

public sealed record Nota(long Id, string Contenido, long AutorId, string Autor, DateTimeOffset CreadoEn,
    DateTimeOffset? EditadoEn);

public sealed record CambioEtapa(string? EtapaAnterior, string EtapaNueva, long? UsuarioId, string? Usuario,
    DateTimeOffset CambiadoEn);

public sealed record ReporteSemanal(DateOnly Semana, long Total, long Calientes, long Tibios, long Frios);

public sealed record EtapaEmbudo(string Etapa, long Total);

public sealed record TasaConversion(long TotalProspectos, long Ganados, decimal TasaPorcentaje);

public sealed record UsuarioSolicitud(string Nombre, string Correo, string? Contrasena, string Rol);

/// <summary>Filtros de la lista de prospectos (HU08). La página empieza en 1 en el panel.</summary>
public sealed class FiltroProspectos
{
    public string? Calificacion { get; set; }
    public string? Etapa { get; set; }
    public DateOnly? Desde { get; set; }
    public DateOnly? Hasta { get; set; }
    public int Pagina { get; set; } = 1;

    public bool TieneFiltros => Calificacion is not null || Etapa is not null || Desde is not null || Hasta is not null;
}
