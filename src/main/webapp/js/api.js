const API_BASE = (() => {
  const pathname = window.location.pathname;
  const basePath = pathname.includes('/api') ? pathname.substring(0, pathname.indexOf('/api')) : pathname.replace(/\/[^/]*$/, '');
  return `${basePath}/api`;
})();

async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const cookieCsrf = document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith('csrf_token='));
  if (cookieCsrf && !headers.has('X-CSRF-Token')) {
    headers.set('X-CSRF-Token', decodeURIComponent(cookieCsrf.substring('csrf_token='.length)));
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: 'same-origin',
    headers
  });

  const contentType = response.headers.get('content-type') || '';
  let data = null;
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    const texte = await response.text();
    if (texte) {
      const correspondance = texte.match(/<p><b>message<\/b>\s*([^<]+)/i);
      data = { erreur: correspondance ? correspondance[1] : texte };
    }
  }

  if (!response.ok) {
    const message = data?.erreur || 'Erreur serveur.';
    throw new Error(message);
  }

  if (data && Object.prototype.hasOwnProperty.call(data, 'succes')) {
    if (!data.succes) throw new Error(data.erreur || 'La demande a échoué.');
    return data.donnees;
  }
  return data;
}

function afficherMessage(message, type = 'success') {
  const zone = document.querySelector('#message');
  if (!zone) return;
  zone.hidden = false;
  zone.className = `alert ${type}`;
  zone.textContent = message;
}

function chargerSession() {
  const session = JSON.parse(sessionStorage.getItem('session') || '{}');
  return session;
}

function enregistrerSession(utilisateur) {
  sessionStorage.setItem('session', JSON.stringify(utilisateur || {}));
}

function deconnecterSession() {
  apiFetch('/auth/deconnexion', { method: 'POST' })
    .catch(() => {})
    .finally(() => {
      sessionStorage.removeItem('session');
      window.location.href = 'index.html';
    });
}

document.addEventListener('DOMContentLoaded', () => {
  const currentPage = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.navbar a[href]').forEach((lien) => {
    const pageLien = lien.getAttribute('href').split('/').pop();
    if (pageLien === currentPage) {
      lien.setAttribute('aria-current', 'page');
    }
  });

  document.querySelectorAll('#logout-link').forEach((lien) => {
    lien.addEventListener('click', (event) => {
      event.preventDefault();
      deconnecterSession();
    });
  });
});

function renduDate(dateIso) {
  if (!dateIso) return '';
  const date = new Date(dateIso);
  return date.toLocaleDateString('fr-FR');
}

function dateLocaleIso(date = new Date()) {
  const decalage = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - decalage).toISOString().slice(0, 10);
}

function formatageMonnaie(valeur) {
  const nombre = Number(valeur || 0);
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'XOF',
    maximumFractionDigits: 2
  }).format(nombre);
}
