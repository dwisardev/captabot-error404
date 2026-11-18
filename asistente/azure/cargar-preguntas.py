#!/usr/bin/env python3
"""
Carga las preguntas frecuentes de CaptaBot en Azure AI Language (custom question answering) y las publica.

Uso (desde la carpeta del repositorio):
    python3 asistente/azure/cargar-preguntas.py              # crea, carga y publica el proyecto
    python3 asistente/azure/cargar-preguntas.py --reiniciar  # borra el proyecto y lo vuelve a crear
    python3 asistente/azure/cargar-preguntas.py --simular    # muestra lo que enviaría, sin llamar a Azure

Lee AZURE_LANGUAGE_ENDPOINT, AZURE_LANGUAGE_KEY y AZURE_LANGUAGE_PROYECTO de las variables de entorno
o del archivo .env de la carpeta del repositorio. Solo usa la biblioteca estándar de Python 3.
"""
import csv
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

VERSION = "2021-10-01"
CARPETA = Path(__file__).resolve().parent
RAIZ = CARPETA.parent.parent
ARCHIVO_PREGUNTAS = CARPETA / "preguntas-frecuentes.tsv"
RESPUESTA_POR_DEFECTO = "No encontré una respuesta a tu pregunta."


def configuracion():
    valores = {}
    archivo_env = RAIZ / ".env"
    if archivo_env.exists():
        for linea in archivo_env.read_text(encoding="utf-8").splitlines():
            linea = linea.strip()
            if linea and not linea.startswith("#") and "=" in linea:
                clave, valor = linea.split("=", 1)
                valores[clave.strip()] = valor.strip().strip('"').strip("'")
    leer = lambda nombre, defecto="": os.environ.get(nombre) or valores.get(nombre) or defecto
    return (leer("AZURE_LANGUAGE_ENDPOINT").rstrip("/"), leer("AZURE_LANGUAGE_KEY"),
            leer("AZURE_LANGUAGE_PROYECTO", "captabot-faq"))


def preguntas_agrupadas():
    """Agrupa las filas del .tsv por respuesta: cada respuesta con todas sus formas de preguntar."""
    grupos = {}
    with ARCHIVO_PREGUNTAS.open(encoding="utf-8", newline="") as archivo:
        for fila in csv.DictReader(archivo, delimiter="\t"):
            grupos.setdefault(fila["Answer"], []).append(fila["Question"])
    return [{"op": "add", "value": {"id": i, "answer": respuesta, "source": "captabot", "questions": preguntas}}
            for i, (respuesta, preguntas) in enumerate(grupos.items(), start=1)]


class Azure:
    def __init__(self, endpoint, clave, simular=False):
        self.endpoint, self.clave, self.simular = endpoint, clave, simular

    def llamar(self, metodo, ruta, cuerpo=None, consulta=None):
        parametros = {"api-version": VERSION, **(consulta or {})}
        url = f"{self.endpoint}{ruta}?{urllib.parse.urlencode(parametros)}" if ruta.startswith("/") else ruta
        if self.simular:
            print(f"\n{metodo} {url}")
            if cuerpo is not None:
                print(json.dumps(cuerpo, ensure_ascii=False, indent=2)[:1500])
            return 200, {}, {}
        datos = None if cuerpo is None else json.dumps(cuerpo).encode("utf-8")
        solicitud = urllib.request.Request(url, data=datos, method=metodo, headers={
            "Ocp-Apim-Subscription-Key": self.clave, "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(solicitud, timeout=60) as r:
                texto = r.read().decode("utf-8")
                return r.status, dict(r.headers), (json.loads(texto) if texto else {})
        except urllib.error.HTTPError as e:
            detalle = e.read().decode("utf-8", errors="replace")
            raise SystemExit(f"Azure respondió {e.code} en {metodo} {url}\n{detalle}")

    def esperar(self, cabeceras, que):
        """Las operaciones largas devuelven Operation-Location: se consulta hasta que terminen."""
        ubicacion = next((v for k, v in cabeceras.items() if k.lower() == "operation-location"), None)
        if self.simular or not ubicacion:
            return
        for _ in range(60):
            _, _, estado = self.llamar("GET", ubicacion)
            situacion = str(estado.get("status", "")).lower()
            if situacion == "succeeded":
                print(f"   {que}: listo")
                return
            if situacion in ("failed", "cancelled"):
                raise SystemExit(f"{que} falló: {json.dumps(estado, ensure_ascii=False)}")
            time.sleep(3)
        raise SystemExit(f"{que} tardó demasiado; revisa el proyecto en el portal de Azure")


def main():
    simular = "--simular" in sys.argv
    endpoint, clave, proyecto = configuracion()
    if not simular and (not endpoint or not clave):
        raise SystemExit("Faltan AZURE_LANGUAGE_ENDPOINT y AZURE_LANGUAGE_KEY (en el archivo .env o como variables).")
    azure = Azure(endpoint or "https://SU-RECURSO.cognitiveservices.azure.com", clave, simular)
    ruta = f"/language/query-knowledgebases/projects/{urllib.parse.quote(proyecto)}"

    if "--reiniciar" in sys.argv:
        print(f"0. Borrando el proyecto {proyecto}…")
        _, cabeceras, _ = azure.llamar("DELETE", ruta)
        azure.esperar(cabeceras, "Borrado")

    print(f"1. Creando el proyecto {proyecto} en español…")
    azure.llamar("PATCH", ruta, {
        "description": "Preguntas frecuentes del asistente CaptaBot",
        "language": "es",
        "multilingualResource": False,
        "settings": {"defaultAnswer": RESPUESTA_POR_DEFECTO},
    })

    qnas = preguntas_agrupadas()
    print(f"2. Cargando {len(qnas)} respuestas con {sum(len(q['value']['questions']) for q in qnas)} formas de preguntar…")
    _, cabeceras, _ = azure.llamar("PATCH", ruta + "/qnas", qnas)
    azure.esperar(cabeceras, "Carga de preguntas")

    print("3. Publicando el proyecto (deployment «production»)…")
    _, cabeceras, _ = azure.llamar("PUT", ruta + "/deployments/production")
    azure.esperar(cabeceras, "Publicación")

    print("4. Probando una pregunta…")
    _, _, respuesta = azure.llamar("POST", "/language/:query-knowledgebases",
                                   {"question": "¿A qué hora atienden?", "top": 1},
                                   {"projectName": proyecto, "deploymentName": "production"})
    if not simular:
        mejor = (respuesta.get("answers") or [{}])[0]
        print(f"   Respuesta: {mejor.get('answer')} (confianza {mejor.get('confidenceScore')})")
    print("\nListo. Reinicien la API y revisen /api/health: debe decir \"preguntasFrecuentes\": \"azure-ai-language\".")


if __name__ == "__main__":
    main()
