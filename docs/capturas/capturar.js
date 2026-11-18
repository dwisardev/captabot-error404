// Toma las capturas de pantalla del sistema para el informe y el manual de usuario.
// Recorre el chat de la página web (con la confirmación de datos de HU04) y el panel
// con los datos de prueba de Docker Compose.
//
// Uso (con el sistema levantado con `docker compose up`):
//   npm i --no-save puppeteer-core
//   node docs/capturas/capturar.js docs/capturas
// Variables opcionales: WEB (http://localhost:8080), PANEL (http://localhost:5080),
// CHROME (ruta de Google Chrome o Chromium).
const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer-core");

const destino = process.argv[2] || "capturas";
const WEB = process.env.WEB || "http://localhost:8080";
const PANEL = process.env.PANEL || "http://localhost:5080";
const CHROME = process.env.CHROME || "/usr/bin/google-chrome";
const espera = (ms) => new Promise((r) => setTimeout(r, ms));

fs.mkdirSync(destino, { recursive: true });

async function nuevaPagina(navegador) {
    const pagina = await nuevaPagina(navegador);
    const cdp = await pagina.createCDPSession();
    await cdp.send("Emulation.setLocaleOverride", { locale: "es-PE" });
    await pagina.setExtraHTTPHeaders({ "Accept-Language": "es-PE,es;q=0.9" });
    return pagina;
}

async function guardar(pagina, nombre, completa = false) {
    await espera(400);
    await pagina.screenshot({ path: path.join(destino, nombre), fullPage: completa });
    console.log("captura:", nombre);
}

async function respuestaDelBot(pagina) {
    await pagina.waitForFunction(() => !document.querySelector(".cb-escribiendo"), { timeout: 20000 });
    await espera(300);
}

async function elegir(pagina, texto) {
    await pagina.waitForFunction(
        (t) => [...document.querySelectorAll(".cb-opcion")].some((b) => b.textContent === t),
        { timeout: 20000 }, texto);
    await pagina.evaluate(
        (t) => [...document.querySelectorAll(".cb-opcion")].find((b) => b.textContent === t).click(), texto);
    await respuestaDelBot(pagina);
}

async function escribir(pagina, texto) {
    await pagina.type(".cb-entrada input", texto);
    await pagina.keyboard.press("Enter");
    await respuestaDelBot(pagina);
}

async function enviarFormulario(pagina, boton) {
    await Promise.all([pagina.waitForNavigation({ waitUntil: "networkidle0" }), pagina.click(boton)]);
}

(async () => {
    const navegador = await puppeteer.launch({
        executablePath: CHROME,
        args: ["--no-sandbox", "--lang=es-PE"],
        // Idioma del navegador en español (Perú): fechas como dd/mm/aaaa
        env: { ...process.env, LANG: "es_PE.UTF-8", LANGUAGE: "es_PE:es" },
        defaultViewport: { width: 1366, height: 900 },
    });
    try {
        // ------------------------------------------------ chat en la página de la PYME
        const web = await nuevaPagina(navegador);
        await web.goto(WEB, { waitUntil: "networkidle0" });
        await guardar(web, "01-pagina-pyme.png");

        await web.click(".cb-boton");
        await respuestaDelBot(web);
        await elegir(web, "¿Cuánto cuesta?");
        await guardar(web, "02-chat-pregunta-frecuente.png");

        await elegir(web, "Sí");
        await elegir(web, "Sí, acepto");
        await escribir(web, "Valeria Ríos");
        await escribir(web, "945 123 678");
        await elegir(web, "Diseño web");
        await escribir(web, "3500");
        await elegir(web, "Alta: esta semana");
        await guardar(web, "03-chat-resumen.png");

        await elegir(web, "Sí, registrar");
        await guardar(web, "04-chat-registrado.png");

        // ------------------------------------------------ chat en un celular
        const movil = await nuevaPagina(navegador);
        await movil.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true });
        await movil.goto(WEB, { waitUntil: "networkidle0" });
        await movil.click(".cb-boton");
        await respuestaDelBot(movil);
        await elegir(movil, "¿Cuál es su horario?");
        await guardar(movil, "05-chat-celular.png");

        // ------------------------------------------------ panel administrativo
        const panel = await nuevaPagina(navegador);
        await panel.goto(PANEL + "/Cuenta/Login", { waitUntil: "networkidle0" });
        await guardar(panel, "06-panel-inicio-sesion.png");

        await panel.type("#Correo", "admin@captabot.pe");
        await panel.type("#Contrasena", "Demo2026!");
        await enviarFormulario(panel, "button[type=submit]");

        await panel.goto(PANEL + "/Prospectos", { waitUntil: "networkidle0" });
        await guardar(panel, "07-panel-prospectos.png", true);

        const detalle = await panel.evaluate(() =>
            [...document.querySelectorAll("a.enlace-fuerte")].find((a) => a.textContent.includes("Valeria")).href);
        await panel.goto(detalle, { waitUntil: "networkidle0" });
        await panel.select("#etapa", "CONTACTADO");
        await enviarFormulario(panel, "form.en-linea button[type=submit]");
        await panel.type("#contenido", "Se llamó a la clienta: necesita una tienda virtual. Enviar la cotización el viernes.");
        await enviarFormulario(panel, "form.nueva-nota button[type=submit]");
        await guardar(panel, "08-panel-detalle.png", true);

        await panel.goto(PANEL + "/Reportes", { waitUntil: "networkidle0" });
        await guardar(panel, "09-panel-reportes.png", true);

        await panel.goto(PANEL + "/Usuarios", { waitUntil: "networkidle0" });
        await guardar(panel, "10-panel-usuarios.png");
    } finally {
        await navegador.close();
    }
})().catch((e) => {
    console.error(e);
    process.exit(1);
});
