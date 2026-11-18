using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.AspNetCore.Mvc.ViewFeatures;

namespace CaptaBot.Panel.Servicios;

/// <summary>Muestra una página clara cuando la API no responde o devuelve un error inesperado.</summary>
public sealed class ErroresApiFilter(IModelMetadataProvider metadatos) : IExceptionFilter
{
    public void OnException(ExceptionContext contexto)
    {
        var mensaje = contexto.Exception switch
        {
            HttpRequestException => "No se pudo conectar con la API de CaptaBot. Revisa que esté iniciada.",
            TaskCanceledException => "La API de CaptaBot tardó demasiado en responder. Inténtalo de nuevo.",
            ApiException api => api.Message,
            _ => null
        };
        if (mensaje is null)
        {
            return;
        }
        contexto.Result = new ViewResult
        {
            ViewName = "ErrorApi",
            StatusCode = StatusCodes.Status503ServiceUnavailable,
            ViewData = new ViewDataDictionary(metadatos, contexto.ModelState) { Model = mensaje }
        };
        contexto.ExceptionHandled = true;
    }
}
