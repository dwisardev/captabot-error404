using CaptaBot.Panel.Ayudas;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CaptaBot.Panel.Controllers;

/// <summary>HU07: solo el administrador crea, edita y desactiva usuarios.</summary>
[Authorize(Roles = Sesion.RolAdministrador)]
public sealed class UsuariosController(ICaptabotApi api) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken ct) => View(await api.ListarUsuariosAsync(ct));

    [HttpGet]
    public IActionResult Nuevo() => View("Formulario", new UsuarioFormulario());

    [HttpPost]
    public async Task<IActionResult> Nuevo(UsuarioFormulario formulario, CancellationToken ct)
    {
        formulario.Id = null;
        if (string.IsNullOrWhiteSpace(formulario.Contrasena))
        {
            ModelState.AddModelError(nameof(UsuarioFormulario.Contrasena), "La contraseña es obligatoria");
        }
        ValidarRol(formulario);
        if (!ModelState.IsValid)
        {
            return View("Formulario", formulario);
        }
        try
        {
            var usuario = await api.CrearUsuarioAsync(formulario.ASolicitud(), ct);
            TempData[ProspectosController.ClaveExito] = $"Se creó el usuario {usuario.Nombre}.";
            return RedirectToAction(nameof(Index));
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            AgregarErrores(ex);
            return View("Formulario", formulario);
        }
    }

    [HttpGet]
    public async Task<IActionResult> Editar(long id, CancellationToken ct)
    {
        var usuario = await api.ObtenerUsuarioAsync(id, ct);
        if (usuario is null)
        {
            Response.StatusCode = StatusCodes.Status404NotFound;
            return View("NoEncontrado");
        }
        return View("Formulario", new UsuarioFormulario
        {
            Id = usuario.Id,
            Nombre = usuario.Nombre,
            Correo = usuario.Correo,
            Rol = usuario.Rol
        });
    }

    [HttpPost]
    public async Task<IActionResult> Editar(long id, UsuarioFormulario formulario, CancellationToken ct)
    {
        formulario.Id = id;
        ValidarRol(formulario);
        if (!ModelState.IsValid)
        {
            return View("Formulario", formulario);
        }
        try
        {
            var usuario = await api.ActualizarUsuarioAsync(id, formulario.ASolicitud(), ct);
            TempData[ProspectosController.ClaveExito] = $"Se actualizaron los datos de {usuario.Nombre}.";
            return RedirectToAction(nameof(Index));
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            AgregarErrores(ex);
            return View("Formulario", formulario);
        }
    }

    [HttpPost]
    public async Task<IActionResult> CambiarEstado(long id, bool activo, CancellationToken ct)
    {
        if (!activo && id == User.UsuarioId())
        {
            TempData[ProspectosController.ClaveError] = "No puedes desactivar tu propio usuario.";
            return RedirectToAction(nameof(Index));
        }
        try
        {
            var usuario = await api.CambiarEstadoUsuarioAsync(id, activo, ct);
            TempData[ProspectosController.ClaveExito] = activo
                ? $"{usuario.Nombre} puede volver a iniciar sesión."
                : $"{usuario.Nombre} ya no puede iniciar sesión.";
        }
        catch (ApiException ex) when (ex.EsErrorDelUsuario)
        {
            TempData[ProspectosController.ClaveError] = ex.Message;
        }
        return RedirectToAction(nameof(Index));
    }

    private void ValidarRol(UsuarioFormulario formulario)
    {
        if (!Etiquetas.EsRol(formulario.Rol))
        {
            ModelState.AddModelError(nameof(UsuarioFormulario.Rol), "Elige un rol válido");
        }
    }

    /// <summary>Muestra los errores de la API junto al campo que corresponde.</summary>
    private void AgregarErrores(ApiException ex)
    {
        if (ex.Detalles.Count == 0)
        {
            var campo = ex.Message.Contains("correo", StringComparison.OrdinalIgnoreCase)
                ? nameof(UsuarioFormulario.Correo)
                : string.Empty;
            ModelState.AddModelError(campo, ex.Message);
            return;
        }
        foreach (var (campo, mensaje) in ex.Detalles)
        {
            var propiedad = campo.Length == 0 ? string.Empty : char.ToUpperInvariant(campo[0]) + campo[1..];
            ModelState.AddModelError(propiedad, mensaje);
        }
    }
}
