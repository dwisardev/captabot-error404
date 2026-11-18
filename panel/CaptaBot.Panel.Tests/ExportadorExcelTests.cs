using System.IO.Compression;
using System.Xml.Linq;
using CaptaBot.Panel.Ayudas;

namespace CaptaBot.Panel.Tests;

/// <summary>HU11: el reporte se exporta a un archivo de Excel válido.</summary>
public class ExportadorExcelTests
{
    private static string Leer(ZipArchive zip, string ruta)
    {
        using var lector = new StreamReader(zip.GetEntry(ruta)!.Open());
        return lector.ReadToEnd();
    }

    [Fact]
    public void Crear_GeneraUnLibroConTextoYNumeros()
    {
        var hoja = new HojaExcel("Por semana", new List<IReadOnlyList<object?>>
        {
            new object?[] { "Semana", "Total" },
            new object?[] { "Diseño & web <1>", 3L }
        });

        var bytes = ExportadorExcel.Crear([hoja]);

        using var zip = new ZipArchive(new MemoryStream(bytes));
        Assert.NotNull(zip.GetEntry("[Content_Types].xml"));
        Assert.NotNull(zip.GetEntry("_rels/.rels"));
        Assert.NotNull(zip.GetEntry("xl/workbook.xml"));
        var xml = Leer(zip, "xl/worksheets/sheet1.xml");
        var documento = XDocument.Parse(xml);
        Assert.Contains("Diseño &amp; web &lt;1&gt;", xml);
        Assert.Contains("<c r=\"B2\"><v>3</v></c>", xml);
        Assert.Equal(2, documento.Descendants().Count(e => e.Name.LocalName == "row"));
    }

    [Fact]
    public void Crear_ConVariasHojas_LasNombraEnElLibro()
    {
        var filas = new List<IReadOnlyList<object?>> { new object?[] { "x" } };

        var bytes = ExportadorExcel.Crear([new HojaExcel("Embudo", filas), new HojaExcel("Conversión: total", filas)]);

        using var zip = new ZipArchive(new MemoryStream(bytes));
        Assert.NotNull(zip.GetEntry("xl/worksheets/sheet2.xml"));
        var libro = Leer(zip, "xl/workbook.xml");
        Assert.Contains("name=\"Embudo\"", libro);
        Assert.Contains("name=\"Conversión total\"", libro);
    }

    [Theory]
    [InlineData(0, "A")]
    [InlineData(4, "E")]
    [InlineData(25, "Z")]
    [InlineData(26, "AA")]
    public void Columna_ConvierteElIndiceEnLetras(int indice, string letras) =>
        Assert.Equal(letras, ExportadorExcel.Columna(indice));
}
