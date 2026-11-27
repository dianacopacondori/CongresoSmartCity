(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-confirm]').forEach(function (element) {
      element.addEventListener('click', function (event) {
        if (!window.confirm(element.getAttribute('data-confirm'))) {
          event.preventDefault();
          event.stopPropagation();
        }
      });
    });
  });
})();
