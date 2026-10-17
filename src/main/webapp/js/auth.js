document.addEventListener('DOMContentLoaded', () => {
  const authForm = document.getElementById('auth-form');
  if (!authForm) return;

  const action = authForm.dataset.action || 'login';

  authForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = new FormData(authForm);

    const payload = action === 'register'
      ? {
          nom: form.get('nom'),
          email: form.get('email'),
          motDePasse: form.get('password')
        }
      : {
          email: form.get('email'),
          motDePasse: form.get('password')
        };

    try {
      await apiFetch(action === 'register' ? '/auth/inscription' : '/auth/connexion', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      window.location.href = 'dashboard.html';
    } catch (error) {
      afficherMessage(error.message, 'error');
    }
  });
});
