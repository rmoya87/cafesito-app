/**
 * GA4 para páginas estáticas (landing, legal).
 * Mismo measurement ID que la SPA (ga4.ts). No se carga en localhost.
 * Uso: definir window.__GA4_PAGE__ = { path: '/landing', title: 'Título' } antes de cargar este script.
 */
(function () {
  var id = 'G-BMZEQNRKR4';
  var host = typeof window !== 'undefined' && window.location && window.location.hostname;
  if (!host || host === 'localhost' || host === '127.0.0.1') return;
  var page = typeof window !== 'undefined' && window.__GA4_PAGE__;
  if (!page || !page.path) return;

  window.dataLayer = window.dataLayer || [];
  function gtag() { dataLayer.push(arguments); }
  window.gtag = gtag;
  gtag('js', new Date());
  gtag('config', id, { send_page_view: false });

  var loc = window.location.origin + window.location.pathname;
  gtag('event', 'page_view', {
    page_path: page.path,
    page_title: page.title || document.title || '',
    page_location: loc
  });

  var s = document.createElement('script');
  s.async = true;
  s.src = 'https://www.googletagmanager.com/gtag/js?id=' + encodeURIComponent(id);
  (document.head || document.documentElement).appendChild(s);
})();
