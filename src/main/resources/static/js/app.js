// AgroPiura IoT — comportamiento mínimo compartido (sin dependencias).
(function () {
    'use strict';

    // Menú plegable del sidebar en pantallas angostas.
    var sidebar = document.querySelector('[data-sidebar]');
    var boton = document.querySelector('[data-menu-boton]');
    if (sidebar && boton) {
        var cambiar = function (abrir) {
            sidebar.setAttribute('data-abierto', abrir ? 'true' : 'false');
            boton.setAttribute('aria-expanded', abrir ? 'true' : 'false');
        };
        boton.addEventListener('click', function () {
            cambiar(boton.getAttribute('aria-expanded') !== 'true');
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && boton.getAttribute('aria-expanded') === 'true') {
                cambiar(false);
                boton.focus();
            }
        });
        document.addEventListener('click', function (e) {
            if (boton.getAttribute('aria-expanded') === 'true' && !sidebar.contains(e.target)) cambiar(false);
        });
    }

    // Confirmación nativa para acciones destructivas: <form data-confirmar="¿...?">
    document.querySelectorAll('form[data-confirmar]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirmar'))) e.preventDefault();
        });
    });

    // Evita doble envío y muestra estado de carga en el botón que envió.
    document.addEventListener('submit', function (e) {
        if (e.defaultPrevented) return;
        var form = e.target;
        if (form.method && form.method.toLowerCase() === 'get') return;
        var enviar = e.submitter || form.querySelector('button[type="submit"]');
        if (!enviar) return;
        requestAnimationFrame(function () {
            enviar.setAttribute('aria-busy', 'true');
            enviar.disabled = true;
        });
    });

    // Recargar datos: <button data-recargar>
    document.querySelectorAll('[data-recargar]').forEach(function (b) {
        b.addEventListener('click', function () { window.location.reload(); });
    });

    // Al volver con el botón "atrás" (bfcache) reactivar botones.
    window.addEventListener('pageshow', function (e) {
        if (!e.persisted) return;
        document.querySelectorAll('button[aria-busy="true"]').forEach(function (b) {
            b.disabled = false;
            b.removeAttribute('aria-busy');
        });
    });
})();
