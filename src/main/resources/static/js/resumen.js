// Repliega el "Resumen de candidatos" unos segundos después de cargar la página,
// para dejar a la vista la tabla de tickets. Se vuelve a abrir con su botón.
(function () {
    const SEGUNDOS = 5;
    const resumen = document.getElementById('resumen');
    if (!resumen) {
        return;
    }
    let tocado = false;
    // Si el usuario abre o cierra el resumen antes de tiempo, no lo repliega por él.
    resumen.querySelector('summary').addEventListener('click', function () {
        tocado = true;
    });
    setTimeout(function () {
        if (!tocado) {
            resumen.open = false;
        }
    }, SEGUNDOS * 1000);
})();
