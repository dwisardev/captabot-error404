using System.ComponentModel.DataAnnotations;

namespace CaptaBot.Panel.Modelos;

public sealed class LoginViewModel
{
    [Required(ErrorMessage = "Ingresa tu correo")]
    [EmailAddress(ErrorMessage = "El correo no es válido")]
    [Display(Name = "Correo")]
    public string Correo { get; set; } = "";

    [Required(ErrorMessage = "Ingresa tu contraseña")]
    [DataType(DataType.Password)]
    [Display(Name = "Contraseña")]
    public string Contrasena { get; set; } = "";

    public string? ReturnUrl { get; set; }
}

public sealed record ProspectosViewModel(FiltroProspectos Filtro, PaginaResultado<Prospecto> Resultado)
{
    /// <summary>Página que se muestra, empezando en 1.</summary>
    public int PaginaActual => Resultado.Pagina + 1;

    public bool HayAnterior => Resultado.Pagina > 0;

    public bool HaySiguiente => Resultado.Pagina + 1 < Resultado.TotalPaginas;
}

public sealed record ProspectoDetalleViewModel(
    Prospecto Prospecto,
    IReadOnlyList<CambioEtapa> Historial,
    IReadOnlyList<Nota> Notas,
    long UsuarioActualId);

public sealed class UsuarioFormulario
{
    public long? Id { get; set; }

    [Required(ErrorMessage = "El nombre es obligatorio")]
    [StringLength(100, ErrorMessage = "El nombre admite hasta 100 caracteres")]
    [Display(Name = "Nombre completo")]
    public string Nombre { get; set; } = "";

    [Required(ErrorMessage = "El correo es obligatorio")]
    [EmailAddress(ErrorMessage = "El correo no es válido")]
    [StringLength(150, ErrorMessage = "El correo admite hasta 150 caracteres")]
    [Display(Name = "Correo")]
    public string Correo { get; set; } = "";

    [DataType(DataType.Password)]
    [StringLength(100, MinimumLength = 8, ErrorMessage = "La contraseña debe tener al menos 8 caracteres")]
    [Display(Name = "Contraseña")]
    public string? Contrasena { get; set; }

    [Required(ErrorMessage = "Elige un rol")]
    [Display(Name = "Rol")]
    public string Rol { get; set; } = "VENDEDOR";

    public bool EsNuevo => Id is null;

    public UsuarioSolicitud ASolicitud() =>
        new(Nombre.Trim(), Correo.Trim(), string.IsNullOrWhiteSpace(Contrasena) ? null : Contrasena, Rol);
}

public sealed record ReportesViewModel(
    IReadOnlyList<ReporteSemanal> Semanas,
    IReadOnlyList<EtapaEmbudo> Embudo,
    TasaConversion Conversion,
    DateOnly? Desde,
    DateOnly? Hasta,
    bool PorCalificacion)
{
    public long MaximoSemanal => Math.Max(1, Semanas.Count == 0 ? 0 : Semanas.Max(s => s.Total));

    public long MaximoEmbudo => Math.Max(1, Embudo.Count == 0 ? 0 : Embudo.Max(e => e.Total));

    /// <summary>Alto de una barra en porcentaje del alto del gráfico.</summary>
    public static int Porcentaje(long valor, long maximo) => (int)Math.Round(100.0 * valor / maximo);
}
