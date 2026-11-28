(function () {
  'use strict';

  function toggleTipoFields() {
    const tipo = document.getElementById('tipo');
    if (!tipo) return;

    const ponencia = document.getElementById('fieldsPonencia');
    const taller = document.getElementById('fieldsTaller');
    if (!ponencia || !taller) return;

    const esTaller = tipo.value === 'TALLER';
    ponencia.hidden = esTaller;
    taller.hidden = !esTaller;
  }

  document.addEventListener('DOMContentLoaded', function () {
    const tipo = document.getElementById('tipo');
    if (tipo) tipo.addEventListener('change', toggleTipoFields);
    toggleTipoFields();
  });
})();
