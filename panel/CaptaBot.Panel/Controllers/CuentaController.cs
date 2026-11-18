using System.Security.Claims;
using CaptaBot.Panel.Ayudas;
using CaptaBot.Panel.Modelos;
using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CaptaBot.Panel.Controllers;

/// <summary>HU06: iniciar y cerrar sesión.</summary>
[AllowAnonymous]
public sealed class CuentaController(ICaptabotApi api) : Controller
{
    internal const string MensajeCredenciales = "Correo o contraseña incorrectos.";
    internal const string MensajeSinApi = "No se pudo conectar con la API de CaptaBot. Revisa que esté iniciada.";

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        if (User.Identity?.IsAuthenticated == true)
        {
            return RedirectToAction("Index", "Prospectos");
        }
        return View(new LoginViewModel { ReturnUrl = returnUrl });
    }

    [HttpPost]
    public async Task<IActionResult> Login(LoginViewModel modelo, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return View(modelo);
        }

        Usuario? usuario;
        try
        {
            usuario = await api.IniciarSesionAsync(modelo.Correo.Trim(), modelo.Contrasena, ct);
        }
        catch (HttpRequestException)
        {
            ModelState.AddModelError(string.Empty, MensajeSinApi);
            return View(modelo);
        }

        if (usuario is null)
        {
            ModelState.AddModelError(string.Empty, MensajeCredenciales);
            modelo.Contrasena = "";
            return View(modelo);
        }

        var identidad = new ClaimsIdentity(Sesion.Claims(usuario), CookieAuthenticationDefaults.AuthenticationScheme);
        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(identidad));

        if (!string.IsNullOrEmpty(modelo.ReturnUrl) && Url.IsLocalUrl(modelo.ReturnUrl))
        {
            return LocalRedirect(modelo.ReturnUrl);
        }
        return RedirectToAction("Index", "Prospectos");
    }

    [HttpPost]
    public async Task<IActionResult> Salir()
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        return RedirectToAction(nameof(Login));
    }

    [HttpGet]
    public IActionResult AccesoDenegado() => View();

    [IgnoreAntiforgeryToken]
    public IActionResult Error() => View("ErrorApi", "Ocurrió un error inesperado. Inténtalo de nuevo.");
}
