let comptes = [];

document.addEventListener('DOMContentLoaded', async () => {
  const formulaire = document.getElementById('compte-form');
  formulaire.addEventListener('submit', enregistrerCompte);
  document.getElementById('annuler-compte').addEventListener('click', reinitialiserCompte);
  await chargerComptes();
});

async function chargerComptes() {
  try {
    comptes = await apiFetch('/comptes');
    const liste = document.getElementById('comptes-list');
    liste.replaceChildren();
    comptes.forEach((compte) => {
      const carte = document.createElement('article');
      carte.className = 'card';
      const titre = document.createElement('h3');
      titre.textContent = compte.nom;
      const solde = document.createElement('p');
      solde.textContent = `Solde courant : ${formatageMonnaie(compte.soldeCourant ?? compte.soldeInitial)} (calculé en XOF)`;
      const type = document.createElement('p');
      type.textContent = `${compte.type} · devise du compte : ${compte.devise}`;
      const modifier = document.createElement('button');
      modifier.type = 'button';
      modifier.className = 'secondary';
      modifier.textContent = 'Modifier';
      modifier.addEventListener('click', () => editerCompte(compte));
      const supprimer = document.createElement('button');
      supprimer.type = 'button';
      supprimer.className = 'danger';
      supprimer.textContent = 'Supprimer';
      supprimer.addEventListener('click', () => supprimerCompte(compte));
      carte.append(titre, solde, type, modifier, supprimer);
      liste.append(carte);
    });
    if (!comptes.length) liste.textContent = 'Aucun compte. Ajoutez-en un pour pouvoir enregistrer vos dépenses.';
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

async function enregistrerCompte(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = new FormData(form);
  const id = values.get('id');
  const payload = {
    nom: values.get('nom'),
    type: values.get('type'),
    devise: values.get('devise').trim().toUpperCase(),
    soldeInitial: Number(values.get('soldeInitial'))
  };
  try {
    await apiFetch(id ? `/comptes/${id}` : '/comptes', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });
    afficherMessage(id ? 'Compte modifié.' : 'Compte créé.', 'success');
    reinitialiserCompte();
    await chargerComptes();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function editerCompte(compte) {
  const form = document.getElementById('compte-form');
  Object.entries(compte).forEach(([key, value]) => {
    if (form.elements[key]) form.elements[key].value = value ?? '';
  });
  form.elements.soldeInitial.value = compte.soldeInitial ?? 0;
  document.getElementById('compte-form-title').textContent = 'Modifier un compte';
  document.getElementById('annuler-compte').hidden = false;
  form.scrollIntoView({ behavior: 'smooth' });
}

async function supprimerCompte(compte) {
  if (!confirm(`Supprimer le compte « ${compte.nom} » ?`)) return;
  try {
    await apiFetch(`/comptes/${compte.id}`, { method: 'DELETE' });
    afficherMessage('Compte supprimé.', 'success');
    await chargerComptes();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function reinitialiserCompte() {
  const form = document.getElementById('compte-form');
  form.reset();
  form.elements.id.value = '';
  document.getElementById('compte-form-title').textContent = 'Ajouter un compte';
  document.getElementById('annuler-compte').hidden = true;
}
