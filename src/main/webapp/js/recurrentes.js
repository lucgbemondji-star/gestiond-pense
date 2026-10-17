let recurrentes = [];
let comptesRecurrentes = [];

document.addEventListener('DOMContentLoaded', async () => {
  document.getElementById('recurrent-date').value = dateLocaleIso();
  document.getElementById('recurrent-form').addEventListener('submit', enregistrerRecurrente);
  document.getElementById('annuler-recurrent').addEventListener('click', reinitialiserRecurrente);
  try {
    const [comptes, categories] = await Promise.all([apiFetch('/comptes'), apiFetch('/categories')]);
    comptesRecurrentes = comptes;
    remplirSelectRecurrent(document.getElementById('recurrent-compte'), comptes, 'Choisir un compte');
    remplirSelectRecurrent(document.getElementById('recurrent-categorie'), categories, 'Choisir une catégorie');
    await chargerRecurrentes();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
});

function remplirSelectRecurrent(select, elements, invite) {
  select.replaceChildren(new Option(invite, ''));
  elements.forEach((element) => select.add(new Option(element.nom, element.id)));
}

async function chargerRecurrentes() {
  recurrentes = await apiFetch('/depenses-recurrentes');
  const corps = document.getElementById('recurrentes-body');
  corps.replaceChildren();
  recurrentes.forEach((depense) => {
    const ligne = document.createElement('tr');
    const valeurs = [
      depense.libelle,
      formatageMonnaie(depense.montant),
      depense.frequence === 'HEBDO' ? 'Hebdomadaire' : 'Mensuelle',
      depense.prochaineDate,
      comptesRecurrentes.find((compte) => Number(compte.id) === Number(depense.compteId))?.nom || 'Compte supprimé',
      depense.active ? 'Active' : 'Suspendue'
    ];
    valeurs.forEach((valeur) => {
      const cellule = document.createElement('td');
      cellule.textContent = valeur || '';
      ligne.append(cellule);
    });
    const actions = document.createElement('td');
    const modifier = document.createElement('button');
    modifier.type = 'button';
    modifier.className = 'secondary';
    modifier.textContent = 'Modifier';
    modifier.addEventListener('click', () => modifierRecurrente(depense));
    const supprimer = document.createElement('button');
    supprimer.type = 'button';
    supprimer.className = 'danger';
    supprimer.textContent = 'Supprimer';
    supprimer.addEventListener('click', () => supprimerRecurrente(depense));
    actions.append(modifier, supprimer);
    ligne.append(actions);
    corps.append(ligne);
  });
  if (!corps.childElementCount) {
    const ligne = document.createElement('tr');
    const cellule = document.createElement('td');
    cellule.colSpan = 7;
    cellule.textContent = 'Aucune dépense récurrente enregistrée.';
    ligne.append(cellule);
    corps.append(ligne);
  }
}

async function enregistrerRecurrente(event) {
  event.preventDefault();
  const donnees = new FormData(event.currentTarget);
  const id = donnees.get('id');
  const payload = {
    libelle: donnees.get('libelle'),
    montant: Number(donnees.get('montant')),
    categorieId: Number(donnees.get('categorieId')),
    compteId: Number(donnees.get('compteId')),
    frequence: donnees.get('frequence'),
    prochaineDate: donnees.get('prochaineDate'),
    active: donnees.get('active') === 'on'
  };
  try {
    await apiFetch(id ? `/depenses-recurrentes/${id}` : '/depenses-recurrentes', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });
    afficherMessage(id ? 'Dépense récurrente modifiée.' : 'Dépense récurrente créée.', 'success');
    reinitialiserRecurrente();
    await chargerRecurrentes();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function modifierRecurrente(depense) {
  const formulaire = document.getElementById('recurrent-form');
  ['id', 'libelle', 'montant', 'categorieId', 'compteId', 'frequence', 'prochaineDate'].forEach((nom) => {
    formulaire.elements[nom].value = depense[nom] ?? '';
  });
  formulaire.elements.namedItem('active').checked = Boolean(depense.active);
  document.getElementById('recurrent-form-title').textContent = 'Modifier une dépense récurrente';
  document.getElementById('annuler-recurrent').hidden = false;
  formulaire.scrollIntoView({ behavior: 'smooth' });
}

async function supprimerRecurrente(depense) {
  if (!confirm(`Supprimer la dépense récurrente « ${depense.libelle} » ?`)) return;
  try {
    await apiFetch(`/depenses-recurrentes/${depense.id}`, { method: 'DELETE' });
    afficherMessage('Dépense récurrente supprimée.', 'success');
    await chargerRecurrentes();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function reinitialiserRecurrente() {
  const formulaire = document.getElementById('recurrent-form');
  formulaire.reset();
  formulaire.elements.id.value = '';
  formulaire.elements.namedItem('active').checked = true;
  document.getElementById('recurrent-date').value = dateLocaleIso();
  document.getElementById('recurrent-form-title').textContent = 'Créer une dépense récurrente';
  document.getElementById('annuler-recurrent').hidden = true;
}
