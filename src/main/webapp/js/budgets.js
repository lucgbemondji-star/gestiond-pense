let categoriesBudget = [];

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('budget-form');
  form.addEventListener('submit', enregistrerBudget);
  document.getElementById('annuler-budget').addEventListener('click', reinitialiserBudget);
  document.getElementById('budgetDateDebut').value = dateLocaleIso();
  try {
    categoriesBudget = await apiFetch('/categories');
    const select = document.getElementById('budgetCategorie');
    select.replaceChildren(new Option('Choisir une catégorie', ''));
    categoriesBudget.forEach((categorie) => select.add(new Option(categorie.nom, categorie.id)));
    await chargerBudgets();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
});

async function chargerBudgets() {
  try {
    const budgets = await apiFetch('/budgets');
    const liste = document.getElementById('budgets-list');
    liste.replaceChildren();
    for (const budget of budgets) {
      const etat = await apiFetch(`/budgets/${budget.id}/etat`);
      const carte = document.createElement('article');
      carte.className = 'card';
      const titre = document.createElement('h3');
      titre.textContent = `${nomCategorieBudget(budget.categorieId)} · ${budget.periode === 'HEBDO' ? 'Hebdomadaire' : 'Mensuel'}`;
      const montant = document.createElement('p');
      montant.textContent = `${formatageMonnaie(etat.montantConsomme)} consommés sur ${formatageMonnaie(budget.plafondXof)} — reste ${formatageMonnaie(etat.reste)}`;
      const barre = document.createElement('progress');
      barre.max = 100;
      barre.value = Math.min(100, Number(etat.pourcentage || 0));
      const jours = document.createElement('p');
      jours.textContent = `${Number(etat.pourcentage || 0).toFixed(1)} % consommé · ${etat.joursRestants} jour(s) restant(s)`;
      const modifier = document.createElement('button');
      modifier.type = 'button';
      modifier.className = 'secondary';
      modifier.textContent = 'Modifier';
      modifier.addEventListener('click', () => editerBudget(budget));
      const supprimer = document.createElement('button');
      supprimer.type = 'button';
      supprimer.className = 'danger';
      supprimer.textContent = 'Supprimer';
      supprimer.addEventListener('click', () => supprimerBudget(budget));
      carte.append(titre, montant, barre, jours, modifier, supprimer);
      liste.append(carte);
    }
    if (!budgets.length) liste.textContent = 'Aucun budget. Créez-en un pour suivre vos dépenses par catégorie.';
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function nomCategorieBudget(id) {
  return categoriesBudget.find((categorie) => Number(categorie.id) === Number(id))?.nom || 'Catégorie';
}

async function enregistrerBudget(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = new FormData(form);
  const id = values.get('id');
  try {
    await apiFetch(id ? `/budgets/${id}` : '/budgets', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify({
        categorieId: Number(values.get('categorieId')),
        periode: values.get('periode'),
        plafondXof: Number(values.get('plafondXof')),
        dateDebut: values.get('dateDebut')
      })
    });
    afficherMessage(id ? 'Budget modifié.' : 'Budget créé.', 'success');
    reinitialiserBudget();
    await chargerBudgets();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function editerBudget(budget) {
  const form = document.getElementById('budget-form');
  form.elements.id.value = budget.id;
  form.elements.categorieId.value = budget.categorieId;
  form.elements.periode.value = budget.periode;
  form.elements.plafondXof.value = budget.plafondXof;
  form.elements.dateDebut.value = budget.dateDebut;
  document.getElementById('budget-form-title').textContent = 'Modifier un budget';
  document.getElementById('annuler-budget').hidden = false;
  form.scrollIntoView({ behavior: 'smooth' });
}

async function supprimerBudget(budget) {
  if (!confirm(`Supprimer le budget « ${nomCategorieBudget(budget.categorieId)} » ?`)) return;
  try {
    await apiFetch(`/budgets/${budget.id}`, { method: 'DELETE' });
    afficherMessage('Budget supprimé.', 'success');
    await chargerBudgets();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function reinitialiserBudget() {
  const form = document.getElementById('budget-form');
  form.reset();
  form.elements.id.value = '';
  form.elements.dateDebut.value = dateLocaleIso();
  document.getElementById('budget-form-title').textContent = 'Créer un budget';
  document.getElementById('annuler-budget').hidden = true;
}
