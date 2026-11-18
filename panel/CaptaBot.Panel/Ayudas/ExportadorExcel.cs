using System.Globalization;
using System.IO.Compression;
using System.Security;
using System.Text;

namespace CaptaBot.Panel.Ayudas;

/// <summary>Una hoja del libro: la primera fila son los encabezados.</summary>
public sealed record HojaExcel(string Nombre, IReadOnlyList<IReadOnlyList<object?>> Filas);

/// <summary>
/// Crea un archivo .xlsx sencillo sin librerías externas (HU11: exportar a Excel).
/// Los números se guardan como números y el resto como texto.
/// </summary>
public static class ExportadorExcel
{
    public const string TipoContenido = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static byte[] Crear(IReadOnlyList<HojaExcel> hojas)
    {
        if (hojas.Count == 0)
        {
            throw new ArgumentException("El libro necesita al menos una hoja", nameof(hojas));
        }
        using var memoria = new MemoryStream();
        using (var zip = new ZipArchive(memoria, ZipArchiveMode.Create, leaveOpen: true))
        {
            Escribir(zip, "[Content_Types].xml", TiposDeContenido(hojas.Count));
            Escribir(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>");
            Escribir(zip, "xl/workbook.xml", Libro(hojas));
            Escribir(zip, "xl/_rels/workbook.xml.rels", RelacionesDelLibro(hojas.Count));
            for (var i = 0; i < hojas.Count; i++)
            {
                Escribir(zip, $"xl/worksheets/sheet{i + 1}.xml", Hoja(hojas[i]));
            }
        }
        return memoria.ToArray();
    }

    internal static string Columna(int indice)
    {
        var letras = "";
        for (var n = indice + 1; n > 0; n = (n - 1) / 26)
        {
            letras = (char)('A' + (n - 1) % 26) + letras;
        }
        return letras;
    }

    private static string TiposDeContenido(int cantidadHojas)
    {
        var sb = new StringBuilder();
        sb.Append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.Append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        sb.Append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.Append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.Append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (var i = 1; i <= cantidadHojas; i++)
        {
            sb.Append($"<Override PartName=\"/xl/worksheets/sheet{i}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        sb.Append("</Types>");
        return sb.ToString();
    }

    private static string Libro(IReadOnlyList<HojaExcel> hojas)
    {
        var sb = new StringBuilder();
        sb.Append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.Append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        sb.Append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (var i = 0; i < hojas.Count; i++)
        {
            var nombre = SecurityElement.Escape(NombreDeHoja(hojas[i].Nombre));
            sb.Append($"<sheet name=\"{nombre}\" sheetId=\"{i + 1}\" r:id=\"rId{i + 1}\"/>");
        }
        sb.Append("</sheets></workbook>");
        return sb.ToString();
    }

    private static string RelacionesDelLibro(int cantidadHojas)
    {
        var sb = new StringBuilder();
        sb.Append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.Append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (var i = 1; i <= cantidadHojas; i++)
        {
            sb.Append($"<Relationship Id=\"rId{i}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet{i}.xml\"/>");
        }
        sb.Append("</Relationships>");
        return sb.ToString();
    }

    private static string Hoja(HojaExcel hoja)
    {
        var sb = new StringBuilder();
        sb.Append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.Append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (var f = 0; f < hoja.Filas.Count; f++)
        {
            var numeroFila = f + 1;
            sb.Append($"<row r=\"{numeroFila}\">");
            var celdas = hoja.Filas[f];
            for (var c = 0; c < celdas.Count; c++)
            {
                var referencia = Columna(c) + numeroFila;
                sb.Append(Celda(referencia, celdas[c]));
            }
            sb.Append("</row>");
        }
        sb.Append("</sheetData></worksheet>");
        return sb.ToString();
    }

    private static string Celda(string referencia, object? valor) => valor switch
    {
        null => $"<c r=\"{referencia}\"/>",
        int or long or decimal or double or float =>
            $"<c r=\"{referencia}\"><v>{Convert.ToString(valor, CultureInfo.InvariantCulture)}</v></c>",
        _ => $"<c r=\"{referencia}\" t=\"inlineStr\"><is><t>{SecurityElement.Escape(Convert.ToString(valor, CultureInfo.InvariantCulture))}</t></is></c>"
    };

    /// <summary>Excel no acepta ciertos caracteres en el nombre de una hoja y limita su largo a 31.</summary>
    private static string NombreDeHoja(string nombre)
    {
        var limpio = new string(nombre.Where(ch => !"[]:*?/\\".Contains(ch)).ToArray()).Trim();
        if (limpio.Length == 0)
        {
            limpio = "Hoja";
        }
        return limpio.Length > 31 ? limpio[..31] : limpio;
    }

    private static void Escribir(ZipArchive zip, string ruta, string contenido)
    {
        var entrada = zip.CreateEntry(ruta, CompressionLevel.Optimal);
        using var flujo = new StreamWriter(entrada.Open(), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        flujo.Write(contenido);
    }
}
