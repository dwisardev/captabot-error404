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
    boton.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M4 3h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 1-2zm3 6v2h2V9H7zm4 0v2h2V9h-2zm4 0v2h2V9h-2z"/></svg>';

    var panel = crear("section", "cb-panel");
    panel.hidden = true;
    panel.setAttribute("aria-label", "Chat con el asistente");

    var cabecera = crear("div", "cb-cabecera");
    var avatar = crear("div", "cb-avatar", "C");
    var titulo = crear("div");
    titulo.appendChild(crear("strong", null, "CaptaBot"));
    titulo.appendChild(crear("span", null, "Asistente virtual · responde al instante"));
    var cerrar = crear("button", "cb-cerrar", "×");
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
    var enviar = crear("button", null, "Enviar");
    enviar.type = "submit";
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

    boton.addEventListener("click", function () {
        panel.hidden = !panel.hidden;
        if (!panel.hidden) {
            entrada.focus();
            if (!iniciado) {
                iniciado = true;
                conversar("", false);
            }
        }
    });
    cerrar.addEventListener("click", function () { panel.hidden = true; });
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
        var escribiendo = agregar("CaptaBot está escribiendo…", "cb-escribiendo");

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
                    agregar(m, "cb-bot" + (datos.registrado && i === 0 ? " cb-exito" : ""));
                });
                (datos.opciones || []).forEach(function (o) {
                    var b = crear("button", "cb-opcion", o);
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
        var burbuja = crear("div", clase.indexOf("cb-escribiendo") === 0 ? clase : "cb-burbuja " + clase, texto);
        mensajes.appendChild(burbuja);
        mensajes.scrollTop = mensajes.scrollHeight;
        return burbuja;
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
