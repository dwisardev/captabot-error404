/*
 * Chat de CaptaBot (HU02). Para insertarlo en una página:
 *   <link rel="stylesheet" href="https://SU-API/chat/captabot-chat.css">
 *   <script src="https://SU-API/chat/captabot-chat.js" data-api="https://SU-API"></script>
 * Sin data-api, usa la misma dirección de la página (cuando la sirve la propia API).
 */
(function () {
    "use strict";

    var script = document.currentScript;
    var api = (script && script.getAttribute("data-api") || "").replace(/\/+$/, "");
    var CLAVE_SESION = "captabot-sesion";
    var sesionId = leerSesion();
    var ocupado = false;
    var iniciado = false;

    // ---------------------------------------------------------------- interfaz
    var boton = crear("button", "cb-boton");
    boton.type = "button";
    boton.setAttribute("aria-label", "Abrir el chat de ayuda");
    boton.setAttribute("aria-expanded", "false");
    boton.innerHTML = icono("M7.9 20A9 9 0 1 0 4 16.1L2 22Z", "M8 12h.01", "M12 12h.01", "M16 12h.01");

    var panel = crear("section", "cb-panel");
    panel.hidden = true;
    panel.setAttribute("aria-label", "Chat con el asistente");

    var cabecera = crear("div", "cb-cabecera");
    var avatar = crear("div", "cb-avatar");
    avatar.innerHTML = icono("M12 8V4H8", "M4 8h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2Z", "M2 14h2", "M20 14h2", "M15 13v2", "M9 13v2");
    var titulo = crear("div");
    titulo.appendChild(crear("strong", null, "CaptaBot"));
    titulo.appendChild(crear("span", null, "En línea · responde al instante"));
    var cerrar = crear("button", "cb-cerrar");
    cerrar.innerHTML = icono("M18 6 6 18", "m6 6 12 12");
    cerrar.type = "button";
    cerrar.setAttribute("aria-label", "Cerrar el chat");
    cabecera.appendChild(avatar);
    cabecera.appendChild(titulo);
    cabecera.appendChild(cerrar);

    var mensajes = crear("div", "cb-mensajes");
    mensajes.setAttribute("role", "log");
    mensajes.setAttribute("aria-live", "polite");
    var opciones = crear("div", "cb-opciones");
    var formulario = crear("form", "cb-entrada");
    var entrada = crear("input");
    entrada.type = "text";
    entrada.maxLength = 500;
    entrada.placeholder = "Escribe tu mensaje…";
    entrada.setAttribute("aria-label", "Tu mensaje");
    var enviar = crear("button");
    enviar.type = "submit";
    enviar.setAttribute("aria-label", "Enviar mensaje");
    enviar.innerHTML = icono("m22 2-7 20-4-9-9-4Z", "M22 2 11 13");
    formulario.appendChild(entrada);
    formulario.appendChild(enviar);
    var pie = crear("div", "cb-pie", "Escribe «menú» para volver a empezar");

    panel.appendChild(cabecera);
    panel.appendChild(mensajes);
    panel.appendChild(opciones);
    panel.appendChild(formulario);
    panel.appendChild(pie);
    document.body.appendChild(panel);
    document.body.appendChild(boton);

    boton.addEventListener("click", function () { mostrar(panel.hidden); });
    cerrar.addEventListener("click", function () { mostrar(false); boton.focus(); });
    panel.addEventListener("keydown", function (e) {
        if (e.key === "Escape") { mostrar(false); boton.focus(); }
    });
    formulario.addEventListener("submit", function (e) {
        e.preventDefault();
        var texto = entrada.value.trim();
        if (texto) {
            entrada.value = "";
            conversar(texto, true);
        }
    });

    // ---------------------------------------------------------- conversación
    function conversar(texto, mostrar) {
        if (ocupado) return;
        if (mostrar) agregar(texto, "cb-cliente");
        limpiar(opciones);
        ocupar(true);
        var escribiendo = indicadorEscribiendo();

        fetch(api + "/api/chat/mensajes", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ sesionId: sesionId, texto: texto })
        })
            .then(function (r) {
                if (!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function (datos) {
                sesionId = datos.sesionId;
                guardarSesion(sesionId);
                (datos.mensajes || []).forEach(function (m, i) {
                    if (m.indexOf("Revisa tus datos:") === 0) resumen(m);
                    else agregar(m, "cb-bot" + (datos.registrado && i === 0 ? " cb-exito" : ""));
                });
                (datos.opciones || []).forEach(function (o) {
                    var b = crear("button", "cb-opcion" + claseOpcion(o), o);
                    b.type = "button";
                    b.addEventListener("click", function () { conversar(o, true); });
                    opciones.appendChild(b);
                });
                mensajes.scrollTop = mensajes.scrollHeight;
            })
            .catch(function () {
                agregar("No pude conectarme en este momento. Inténtalo de nuevo en unos segundos.", "cb-bot");
            })
            .then(function () {
                escribiendo.remove();
                ocupar(false);
                entrada.focus();
            });
    }

    // --------------------------------------------------------------- apoyo
    function crear(etiqueta, clase, texto) {
        var el = document.createElement(etiqueta);
        if (clase) el.className = clase;
        if (texto !== undefined) el.textContent = texto;
        return el;
    }

    function agregar(texto, clase) {
        var burbuja = crear("div", "cb-burbuja " + clase, texto);
        mensajes.appendChild(burbuja);
        mensajes.scrollTop = mensajes.scrollHeight;
        return burbuja;
    }

    // Tres puntos animados; se quita cuando llega la respuesta.
    function indicadorEscribiendo() {
        var el = crear("div", "cb-escribiendo");
        el.setAttribute("role", "status");
        el.setAttribute("aria-label", "CaptaBot está escribiendo");
        el.innerHTML = "<i></i><i></i><i></i>";
        mensajes.appendChild(el);
        mensajes.scrollTop = mensajes.scrollHeight;
        return el;
    }

    // «Revisa tus datos:» con líneas «• Etiqueta: valor» se muestra como una tarjeta de resumen.
    function resumen(texto) {
        var tarjeta = crear("div", "cb-resumen");
        var titulo = crear("div", "cb-resumen-titulo");
        titulo.innerHTML = icono("M20 6 9 17l-5-5");
        titulo.appendChild(document.createTextNode("Revisa tus datos"));
        var lista = crear("dl");
        var pie = [];
        texto.split("\n").slice(1).forEach(function (linea) {
            var l = linea.trim();
            if (!l) return;
            var m = /^•\s*([^:]+):\s*(.*)$/.exec(l);
            if (m) {
                var fila = crear("div", "cb-fila");
                fila.appendChild(crear("dt", null, m[1]));
                fila.appendChild(crear("dd", null, m[2]));
                lista.appendChild(fila);
            } else {
                pie.push(l);
            }
        });
        tarjeta.appendChild(titulo);
        tarjeta.appendChild(lista);
        if (pie.length) tarjeta.appendChild(crear("div", "cb-resumen-pie", pie.join(" ")));
        mensajes.appendChild(tarjeta);
        mensajes.scrollTop = mensajes.scrollHeight;
    }

    function claseOpcion(texto) {
        if (texto === "Sí, registrar") return " cb-principal";
        if (texto === "Corregir datos") return " cb-secundaria";
        return "";
    }

    function mostrar(abrir) {
        panel.hidden = !abrir;
        boton.classList.toggle("cb-abierto", abrir);
        boton.setAttribute("aria-expanded", abrir ? "true" : "false");
        boton.setAttribute("aria-label", abrir ? "Cerrar el chat de ayuda" : "Abrir el chat de ayuda");
        if (abrir) {
            entrada.focus();
            if (!iniciado) {
                iniciado = true;
                conversar("", false);
            }
        }
    }

    // Íconos SVG en línea (estilo Lucide): cada argumento es el trazado «d» de una figura.
    function icono() {
        var trazos = Array.prototype.slice.call(arguments).map(function (d) { return '<path d="' + d + '"/>'; }).join("");
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + trazos + "</svg>";
    }

    function limpiar(el) {
        while (el.firstChild) el.removeChild(el.firstChild);
    }

    function ocupar(valor) {
        ocupado = valor;
        entrada.disabled = valor;
        enviar.disabled = valor;
    }

    function leerSesion() {
        try { return sessionStorage.getItem(CLAVE_SESION); } catch (e) { return null; }
    }

    function guardarSesion(valor) {
        try { sessionStorage.setItem(CLAVE_SESION, valor); } catch (e) { /* sin almacenamiento: sigue en memoria */ }
    }
})();
