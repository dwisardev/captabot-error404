using CaptaBot.Panel.Ayudas;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Mvc;

namespace CaptaBot.Panel.Controllers;

/// <summary>HU11 y HU12: prospectos por semana, embudo y tasa de conversión.</summary>
public sealed class ReportesController(ICaptabotApi api) : Controller
{
    internal const int Semanas = 8;

    [HttpGet]
    public async Task<IActionResult> Index(DateOnly? desde, DateOnly? hasta, string? vista, CancellationToken ct)
    {
        if (desde is not null && hasta is not null && desde > hasta)
        {
            ModelState.AddModelError(string.Empty, "La fecha inicial no puede ser posterior a la fecha final.");
            desde = null;
            hasta = null;
        }
        var modelo = await CargarAsync(desde, hasta, vista != "total", ct);
        return View(modelo);
    }

    /// <summary>Descarga los tres reportes en un archivo de Excel.</summary>
    [HttpGet]
    public async Task<IActionResult> Excel(DateOnly? desde, DateOnly? hasta, CancellationToken ct)
    {
        if (desde is not null && hasta is not null && desde > hasta)
        {
            desde = null;
            hasta = null;
        }
        var modelo = await CargarAsync(desde, hasta, porCalificacion: true, ct);
        var archivo = ExportadorExcel.Crear(Hojas(modelo));
        var nombre = $"reporte-captabot-{DateTimeOffset.UtcNow.ToOffset(Etiquetas.HoraPeru):yyyy-MM-dd}.xlsx";
        return File(archivo, ExportadorExcel.TipoContenido, nombre);
    }

    internal static IReadOnlyList<HojaExcel> Hojas(ReportesViewModel modelo)
    {
        var periodo = DescribirPeriodo(modelo.Desde, modelo.Hasta);

        var semanal = new List<IReadOnlyList<object?>>
        {
            new object?[] { "Semana (lunes)", "Total", "Calientes", "Tibios", "Fríos" }
        };
        semanal.AddRange(modelo.Semanas.Select(s =>
            (IReadOnlyList<object?>)new object?[] { Etiquetas.Fecha(s.Semana), s.Total, s.Calientes, s.Tibios, s.Frios }));

        var embudo = new List<IReadOnlyList<object?>>
        {
            new object?[] { "Etapa", "Prospectos", "Periodo" }
        };
        embudo.AddRange(modelo.Embudo.Select(e =>
            (IReadOnlyList<object?>)new object?[] { Etiquetas.TextoEtapa(e.Etapa), e.Total, periodo }));

        var conversion = new List<IReadOnlyList<object?>>
        {
            new object?[] { "Periodo", "Prospectos", "Cerrados ganados", "Tasa de conversión (%)" },
            new object?[] { periodo, modelo.Conversion.TotalProspectos, modelo.Conversion.Ganados, modelo.Conversion.TasaPorcentaje }
        };

        return
        [
            new HojaExcel("Por semana", semanal),
            new HojaExcel("Embudo", embudo),
            new HojaExcel("Conversión", conversion)
        ];
    }

    internal static string DescribirPeriodo(DateOnly? desde, DateOnly? hasta) => (desde, hasta) switch
    {
        (null, null) => "Todo el periodo",
        ({ } d, null) => $"Desde el {Etiquetas.Fecha(d)}",
        (null, { } h) => $"Hasta el {Etiquetas.Fecha(h)}",
        ({ } d, { } h) => $"Del {Etiquetas.Fecha(d)} al {Etiquetas.Fecha(h)}"
    };

    private async Task<ReportesViewModel> CargarAsync(DateOnly? desde, DateOnly? hasta, bool porCalificacion,
        CancellationToken ct)
    {
        var semanas = api.ReporteSemanalAsync(Semanas, ct);
        var embudo = api.EmbudoAsync(desde, hasta, ct);
        var conversion = api.ConversionAsync(desde, hasta, ct);
        await Task.WhenAll(semanas, embudo, conversion);
        return new ReportesViewModel(semanas.Result, embudo.Result, conversion.Result, desde, hasta, porCalificacion);
    }
}
