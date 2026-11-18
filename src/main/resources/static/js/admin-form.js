(function () {
  'use strict';

  function toggleTipoFields() {
    const tipo = document.getElementById('tipo');
    if (!tipo) return;

    const ponencia = document.getElementById('fieldsPonencia');
    const taller = document.getElementById('fieldsTaller');
    const selectPonente = document.getElementById('colaboradorPonente');
    const selectInstructor = document.getElementById('colaboradorInstructor');
    if (!ponencia || !taller || !selectPonente || !selectInstructor) return;

    const esTaller = tipo.value === 'TALLER';
    ponencia.hidden = esTaller;
    taller.hidden = !esTaller;

    if (esTaller) {
      selectPonente.removeAttribute('name');
      selectPonente.removeAttribute('required');
      selectInstructor.setAttribute('name', 'colaboradorId');
      selectInstructor.setAttribute('required', 'required');
    } else {
      selectInstructor.removeAttribute('name');
      selectInstructor.removeAttribute('required');
      selectPonente.setAttribute('name', 'colaboradorId');
      selectPonente.setAttribute('required', 'required');
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    const tipo = document.getElementById('tipo');
    if (tipo) tipo.addEventListener('change', toggleTipoFields);
    toggleTipoFields();
  });
})();
