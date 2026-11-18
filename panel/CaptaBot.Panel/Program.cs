using CaptaBot.Panel.Servicios;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Authorization;

var builder = WebApplication.CreateBuilder(args);

// Conexión con la API de CaptaBot (Java). Se configura en appsettings.json o con variables de entorno:
// CaptabotApi__UrlBase y CaptabotApi__ClaveApi.
builder.Services.Configure<CaptabotApiOptions>(builder.Configuration.GetSection(CaptabotApiOptions.Seccion));
builder.Services.AddHttpClient<ICaptabotApi, CaptabotApiClient>();

// HU06: inicio de sesión con cookie. Ninguna página se abre sin iniciar sesión.
builder.Services
    .AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(opciones =>
    {
        opciones.LoginPath = "/Cuenta/Login";
        opciones.LogoutPath = "/Cuenta/Salir";
        opciones.AccessDeniedPath = "/Cuenta/AccesoDenegado";
        opciones.ExpireTimeSpan = TimeSpan.FromHours(8);
        opciones.SlidingExpiration = true;
        opciones.Cookie.Name = "CaptaBot.Panel";
        opciones.Cookie.HttpOnly = true;
    });
builder.Services.AddAuthorization();

builder.Services.AddControllersWithViews(opciones =>
{
    opciones.Filters.Add(new AuthorizeFilter());
    opciones.Filters.Add(new AutoValidateAntiforgeryTokenAttribute());
    opciones.Filters.Add<ErroresApiFilter>();
});

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Cuenta/Error");
}

app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Prospectos}/{action=Index}/{id?}");

app.Run();

/// <summary>Visible para las pruebas de integración del panel.</summary>
public partial class Program
{
}
