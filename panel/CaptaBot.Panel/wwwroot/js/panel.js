/* Interruptor de tema claro/oscuro y menú lateral del panel (solo presentación). */
(function () {
    "use strict";
    var raiz = document.documentElement;

    document.querySelectorAll("[data-cambiar-tema]").forEach(function (boton) {
        boton.addEventListener("click", function () {
            var nuevo = raiz.getAttribute("data-tema") === "claro" ? "oscuro" : "claro";
            raiz.setAttribute("data-tema", nuevo);
            try { localStorage.setItem("captabot-tema", nuevo); } catch (e) { /* no se guarda; el tema sigue aplicado */ }
        });
    });

    var cuerpo = document.body;
    var abrir = document.getElementById("abrir-menu");
    var velo = document.getElementById("velo-menu");
    function menu(visible) {
        cuerpo.classList.toggle("menu-abierto", visible);
        if (abrir) abrir.setAttribute("aria-expanded", visible ? "true" : "false");
    }
    if (abrir) abrir.addEventListener("click", function () { menu(!cuerpo.classList.contains("menu-abierto")); });
    if (velo) velo.addEventListener("click", function () { menu(false); });
    document.addEventListener("keydown", function (e) { if (e.key === "Escape") menu(false); });
})();
