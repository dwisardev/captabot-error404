using System.Text;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Html;

namespace CaptaBot.Panel.Ayudas;

public static class HtmlAyuda
{
    /// <summary>Genera las opciones de un select marcando la elegida.</summary>
    public static IHtmlContent Opciones(IEnumerable<(string Valor, string Texto)> opciones, string? seleccionado)
    {
        var html = new StringBuilder();
        foreach (var (valor, texto) in opciones)
        {
            html.Append("<option value=\"").Append(HtmlEncoder.Default.Encode(valor)).Append('"');
            if (valor == seleccionado)
            {
                html.Append(" selected");
            }
            html.Append('>').Append(HtmlEncoder.Default.Encode(texto)).Append("</option>");
        }
        return new HtmlString(html.ToString());
    }
}
