(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    const body = document.body;
    const hamburger = document.getElementById('adminHamburger');
    const closeBtn = document.getElementById('adminSidebarClose');
    const overlay = document.getElementById('adminSidebarOverlay');

    function setSidebar(open) {
      body.classList.toggle('sidebar-open', open);
      if (hamburger) hamburger.setAttribute('aria-expanded', open ? 'true' : 'false');
      if (overlay) overlay.hidden = !open;
    }

    function toggleSidebar() {
      setSidebar(!body.classList.contains('sidebar-open'));
    }

    if (hamburger) hamburger.addEventListener('click', toggleSidebar);
    if (closeBtn) closeBtn.addEventListener('click', function () { setSidebar(false); });
    if (overlay) overlay.addEventListener('click', function () { setSidebar(false); });

    if (window.matchMedia('(min-width: 992px)').matches) setSidebar(true);

    window.addEventListener('resize', function () {
      if (window.matchMedia('(min-width: 992px)').matches) setSidebar(true);
      else setSidebar(false);
    });
  });
})();
