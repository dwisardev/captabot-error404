using CaptaBot.Panel.Ayudas;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Mvc;

namespace CaptaBot.Panel.Controllers;

/// <summary>HU08, HU09 y HU10: lista de prospectos, cambio de etapa y notas de seguimiento.</summary>
public sealed class ProspectosController(ICaptabotApi api) : Controller
{
    internal const string ClaveExito = "Exito";
    internal const string ClaveError = "Error";

    [HttpGet]
    public async Task<IActionResult> Index([FromQuery] FiltroProspectos filtro, CancellationToken ct)
    {
        // Valores desconocidos en la URL se ignoran en vez de causar un error.
        if (!Etiquetas.EsCalificacion(filtro.Calificacion)) filtro.Calificacion = null;
        if (!Etiquetas.EsEtapa(filtro.Etapa)) filtro.Etapa = null;
        if (filtro.Pagina < 1) filtro.Pagina = 1;

        if (filtro.Desde is not null && filtro.Hasta is not null && filtro.Desde > filtro.Hasta)
        {
            ModelState.AddModelError(string.Empty, "La fecha inicial no puede ser posterior a la fecha final.");
            var vacio = new PaginaResultado<Prospecto>([], 0, CaptabotApiClient.TamanoPagina, 0, 0);
            return View(new ProspectosViewModel(filtro, vacio));
        }

        var resultado = await api.ListarProspectosAsync(filtro, ct);
        return View(new ProspectosViewModel(filtro, resultado));
    }

    [HttpGet]
    public async Task<IActionResult> Detalle(long id, CancellationToken ct)
    {
        var prospecto = await api.ObtenerProspectoAsync(id, ct);
        if (prospecto is null)
        {
            Response.StatusCode = StatusCodes.Status404NotFound;
            return View("NoEncontrado");
        }
        var historial = api.HistorialAsync(id, ct);
        var notas = api.NotasAsync(id, ct);
        await Task.WhenAll(historial, notas);
        return View(new ProspectoDetalleViewModel(prospecto, historial.Result, notas.Result, User.UsuarioId()));
    }

    [HttpPost]
    public async Task<IActionResult> CambiarEtapa(long id, string etapa, CancellationToken ct)
    {
        if (!Etiquetas.EsEtapa(etapa))
        {
            TempData[ClaveError] = "Elige una etapa válida.";
            return RedirectToAction(nameof(Detalle), new { id });
        }
        try
        {
            await api.CambiarEtapaAsync(id, etapa, User.UsuarioId(), ct);
            TempData[ClaveExito] = $"La etapa cambió a «{Etiquetas.TextoEtapa(etapa)}».";
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            TempData[ClaveError] = ex.Message;
        }
        return RedirectToAction(nameof(Detalle), new { id });
    }

    [HttpPost]
    public async Task<IActionResult> AgregarNota(long id, string? contenido, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(contenido))
        {
            TempData[ClaveError] = "Escribe la nota antes de guardarla.";
            return RedirectToAction(nameof(Detalle), new { id });
        }
        try
        {
            await api.AgregarNotaAsync(id, User.UsuarioId(), contenido.Trim(), ct);
            TempData[ClaveExito] = "Nota guardada.";
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            TempData[ClaveError] = ex.Message;
        }
        return RedirectToAction(nameof(Detalle), new { id });
    }

    [HttpPost]
    public async Task<IActionResult> EditarNota(long id, long notaId, string? contenido, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(contenido))
        {
            TempData[ClaveError] = "La nota no puede quedar vacía.";
            return RedirectToAction(nameof(Detalle), new { id });
        }
        try
        {
            await api.EditarNotaAsync(id, notaId, User.UsuarioId(), contenido.Trim(), ct);
            TempData[ClaveExito] = "Nota actualizada.";
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            TempData[ClaveError] = ex.Message;
        }
        return RedirectToAction(nameof(Detalle), new { id });
    }
}
