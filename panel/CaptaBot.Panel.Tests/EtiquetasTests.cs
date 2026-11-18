using CaptaBot.Panel.Ayudas;

namespace CaptaBot.Panel.Tests;

public class EtiquetasTests
{
    [Theory]
    [InlineData("NUEVO", "Nuevo")]
    [InlineData("CERRADO_GANADO", "Cerrado – ganado")]
    [InlineData("CERRADO_PERDIDO", "Cerrado – perdido")]
    [InlineData("DESCONOCIDA", "DESCONOCIDA")]
    public void TextoEtapa_MuestraUnNombreLegible(string valor, string esperado) =>
        Assert.Equal(esperado, Etiquetas.TextoEtapa(valor));

    [Theory]
    [InlineData("CALIENTE", "Caliente", "chip chip-caliente")]
    [InlineData("TIBIO", "Tibio", "chip chip-tibio")]
    [InlineData("FRIO", "Frío", "chip chip-frio")]
    public void Calificacion_TieneTextoYColor(string valor, string texto, string clase)
    {
        Assert.Equal(texto, Etiquetas.TextoCalificacion(valor));
        Assert.Equal(clase, Etiquetas.ClaseCalificacion(valor));
    }

    [Theory]
    [InlineData("CHAT_WEB", "Chat web")]
    [InlineData("TELEGRAM", "Telegram")]
    [InlineData("OTRO", "OTRO")]
    public void TextoCanal_IndicaPorDondeLlego(string valor, string esperado) =>
        Assert.Equal(esperado, Etiquetas.TextoCanal(valor));

    [Fact]
    public void FechaHora_SeMuestraEnHoraDePeru()
    {
        var utc = new DateTimeOffset(2026, 11, 18, 3, 0, 0, TimeSpan.Zero);

        Assert.Equal("17/11/2026 22:00", Etiquetas.FechaHora(utc));
    }

    [Fact]
    public void Soles_UsaSeparadorDeMilesYDosDecimales() =>
        Assert.Equal("S/ 6,000.00", Etiquetas.Soles(6000m));

    [Fact]
    public void WhatsApp_AgregaElCodigoDePeru() =>
        Assert.Equal("https://wa.me/51987654321", Etiquetas.WhatsApp("987654321"));

    [Fact]
    public void Opciones_MarcaLaElegidaYEscapaElTexto()
    {
        var html = HtmlAyuda.Opciones([("A", "Uno & dos"), ("B", "Tres")], "B").ToString();

        Assert.Equal("<option value=\"A\">Uno &amp; dos</option><option value=\"B\" selected>Tres</option>", html);
    }
}
