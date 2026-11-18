using System.Globalization;
using System.Security.Claims;
using CaptaBot.Panel.Modelos;

namespace CaptaBot.Panel.Ayudas;

/// <summary>Datos del usuario que inició sesión, guardados en la cookie (HU06).</summary>
public static class Sesion
{
    public const string RolAdministrador = "ADMINISTRADOR";

    public static IEnumerable<Claim> Claims(Usuario usuario) =>
    [
        new Claim(ClaimTypes.NameIdentifier, usuario.Id.ToString(CultureInfo.InvariantCulture)),
        new Claim(ClaimTypes.Name, usuario.Nombre),
        new Claim(ClaimTypes.Email, usuario.Correo),
        new Claim(ClaimTypes.Role, usuario.Rol)
    ];

    public static long UsuarioId(this ClaimsPrincipal usuario) =>
        long.Parse(usuario.FindFirstValue(ClaimTypes.NameIdentifier)
                   ?? throw new InvalidOperationException("No hay una sesión iniciada"),
            CultureInfo.InvariantCulture);

    public static bool EsAdministrador(this ClaimsPrincipal usuario) => usuario.IsInRole(RolAdministrador);
}
