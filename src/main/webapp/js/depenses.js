let categoriesDisponibles = [];
let comptesDisponibles = [];
let depensesChargees = [];
let pageDepenses = 1;
const depensesParPage = 25;
let suggestionTimer;

document.addEventListener('DOMContentLoaded', async () => {
  const formulaire = document.getElementById('depense-form');
  document.getElementById('dateDepense').value = dateLocaleIso();
  formulaire.addEventListener('submit', enregistrerDepense);
  document.getElementById('annuler-modification').addEventListener('click', reinitialiserFormulaire);
  document.getElementById('filtres-depenses').addEventListener('submit', (event) => {
    event.preventDefault();
    pageDepenses = 1;
    chargerDepenses();
  });
  document.getElementById('reinitialiser-filtres').addEventListener('click', () => {
    pageDepenses = 1;
    setTimeout(chargerDepenses, 0);
  });
  document.getElementById('libelle').addEventListener('input', suggererCategorie);
  document.getElementById('categorie').addEventListener('change', memoriserCorrection);
  await chargerChoix();
  await chargerDepenses();
});

async function chargerChoix() {
  try {
    [categoriesDisponibles, comptesDisponibles] = await Promise.all([
      apiFetch('/categories'),
      apiFetch('/comptes')
    ]);
    remplirSelect(document.getElementById('categorie'), categoriesDisponibles, 'Choisir une catégorie');
    remplirSelect(document.getElementById('filtre-categorie'), categoriesDisponibles, 'Toutes');
    remplirSelect(document.getElementById('compte'), comptesDisponibles, 'Choisir un compte');
    remplirSelect(document.getElementById('filtre-compte'), comptesDisponibles, 'Tous');
    if (!comptesDisponibles.length) {
      afficherMessage('Créez d’abord un compte dans la page « Comptes » pour pouvoir saisir une dépense.', 'error');
    }
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function remplirSelect(select, elements, libelleVide) {
  select.replaceChildren();
  const optionVide = document.createElement('option');
  optionVide.value = '';
  optionVide.textContent = libelleVide;
  select.append(optionVide);
  elements.forEach((element) => {
    const option = document.createElement('option');
    option.value = element.id;
    option.textContent = element.nom;
    select.append(option);
  });
}

async function chargerDepenses() {
  try {
    const parametres = new URLSearchParams(new FormData(document.getElementById('filtres-depenses')));
    depensesChargees = await apiFetch(`/depenses?${parametres}`);
    afficherPageDepenses();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function afficherPageDepenses() {
    const corps = document.getElementById('depenses-body');
    corps.replaceChildren();
    const nombrePages = Math.max(1, Math.ceil(depensesChargees.length / depensesParPage));
    pageDepenses = Math.min(pageDepenses, nombrePages);
    const debut = (pageDepenses - 1) * depensesParPage;
    depensesChargees.slice(debut, debut + depensesParPage).forEach((depense) => {
      const ligne = document.createElement('tr');
      [
        depense.libelle,
        categorieNom(depense.categorieId),
        `${formatageMonnaie(depense.montantXof)}${depense.devise && depense.devise !== 'XOF' ? ` (${depense.montant} ${depense.devise})` : ''}`,
        depense.dateDepense || '',
        compteNom(depense.compteId)
      ].forEach((valeur) => {
        const cellule = document.createElement('td');
        cellule.textContent = valeur || '';
        ligne.append(cellule);
      });
      const actions = document.createElement('td');
      const modifier = document.createElement('button');
      modifier.type = 'button';
      modifier.className = 'secondary';
      modifier.textContent = 'Modifier';
      modifier.addEventListener('click', () => modifierDepense(depense));
      const supprimer = document.createElement('button');
      supprimer.type = 'button';
      supprimer.className = 'danger';
      supprimer.textContent = 'Supprimer';
      supprimer.addEventListener('click', () => supprimerDepense(depense));
      actions.append(modifier, supprimer);
      ligne.append(actions);
      corps.append(ligne);
    });
    if (!corps.childElementCount) {
      const ligne = document.createElement('tr');
      const cellule = document.createElement('td');
      cellule.colSpan = 6;
      cellule.textContent = 'Aucune dépense ne correspond à ces filtres.';
      ligne.append(cellule);
      corps.append(ligne);
    }
    afficherPagination(nombrePages);
}

function afficherPagination(nombrePages) {
  const zone = document.getElementById('pagination-depenses');
  zone.replaceChildren();
  const precedent = document.createElement('button');
  precedent.type = 'button';
  precedent.className = 'secondary';
  precedent.textContent = 'Précédent';
  precedent.disabled = pageDepenses <= 1;
  precedent.addEventListener('click', () => {
    if (pageDepenses > 1) {
      pageDepenses--;
      afficherPageDepenses();
    }
  });
  const indication = document.createElement('span');
  indication.textContent = `Page ${pageDepenses} sur ${nombrePages} — ${depensesChargees.length} dépense(s)`;
  const suivant = document.createElement('button');
  suivant.type = 'button';
  suivant.className = 'secondary';
  suivant.textContent = 'Suivant';
  suivant.disabled = pageDepenses >= nombrePages;
  suivant.addEventListener('click', () => {
    if (pageDepenses < nombrePages) {
      pageDepenses++;
      afficherPageDepenses();
    }
  });
  zone.append(precedent, indication, suivant);
}

function categorieNom(id) {
  return categoriesDisponibles.find((categorie) => Number(categorie.id) === Number(id))?.nom || 'Catégorie supprimée';
}

function compteNom(id) {
  return comptesDisponibles.find((compte) => Number(compte.id) === Number(id))?.nom || 'Compte supprimé';
}

async function suggererCategorie() {
  clearTimeout(suggestionTimer);
  const libelle = document.getElementById('libelle').value.trim();
  if (libelle.length < 2) {
    document.getElementById('suggestion-categorie').textContent = '';
    return;
  }
  suggestionTimer = setTimeout(async () => {
    try {
      const suggestion = await apiFetch('/categorisation/suggestion', {
        method: 'POST',
        body: JSON.stringify({ libelle })
      });
      const categorie = categoriesDisponibles.find((item) => Number(item.id) === Number(suggestion.categorieId));
      if (categorie) {
        document.getElementById('categorie').value = categorie.id;
        document.getElementById('suggestion-categorie').textContent =
          `${suggestion.message} ${categorie.nom} (${Math.round(suggestion.confiance * 100)} %).`;
      }
    } catch (error) {
      document.getElementById('suggestion-categorie').textContent = error.message;
    }
  }, 350);
}

async function memoriserCorrection() {
  const libelle = document.getElementById('libelle').value.trim();
  const categorieId = document.getElementById('categorie').value;
  if (!libelle || !categorieId) return;
  try {
    await apiFetch('/categorisation/suggestion', {
      method: 'POST',
      body: JSON.stringify({ libelle, categorieId: Number(categorieId) })
    });
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

async function enregistrerDepense(event) {
  event.preventDefault();
  const formulaire = event.currentTarget;
  const donnees = new FormData(formulaire);
  const id = donnees.get('id');
  const payload = {
    libelle: donnees.get('libelle'),
    categorieId: Number(donnees.get('categorieId')),
    compteId: Number(donnees.get('compteId')),
    montant: Number(donnees.get('montant')),
    devise: donnees.get('devise'),
    dateDepense: donnees.get('dateDepense'),
    note: donnees.get('note')
  };
  try {
    if (!id) {
      const historiques = await apiFetch('/depenses');
      const doublon = historiques.some((depense) =>
        depense.dateDepense === payload.dateDepense
        && depense.libelle?.trim().toLocaleLowerCase('fr-FR') === payload.libelle.trim().toLocaleLowerCase('fr-FR')
        && Number(depense.montant) === payload.montant
        && depense.devise === payload.devise);
      if (doublon && !confirm('Une dépense avec le même libellé, montant et date existe déjà. Enregistrer quand même ?')) return;
    }
    await apiFetch(id ? `/depenses/${id}` : '/depenses', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });
    afficherMessage(id ? 'Dépense modifiée.' : 'Dépense enregistrée.', 'success');
    reinitialiserFormulaire();
    await chargerDepenses();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function modifierDepense(depense) {
  const formulaire = document.getElementById('depense-form');
  Object.entries({
    id: depense.id,
    libelle: depense.libelle,
    categorieId: depense.categorieId,
    compteId: depense.compteId,
    montant: depense.montant,
    devise: depense.devise,
    dateDepense: depense.dateDepense,
    note: depense.note || ''
  }).forEach(([nom, valeur]) => { formulaire.elements[nom].value = valeur ?? ''; });
  document.getElementById('depense-form-title').textContent = 'Modifier une dépense';
  document.getElementById('annuler-modification').hidden = false;
  formulaire.scrollIntoView({ behavior: 'smooth' });
}

async function supprimerDepense(depense) {
  if (!confirm(`Supprimer la dépense « ${depense.libelle} » ?`)) return;
  try {
    await apiFetch(`/depenses/${depense.id}`, { method: 'DELETE' });
    afficherMessage('Dépense supprimée.', 'success');
    await chargerDepenses();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function reinitialiserFormulaire() {
  const formulaire = document.getElementById('depense-form');
  formulaire.reset();
  formulaire.elements.id.value = '';
  formulaire.elements.dateDepense.value = dateLocaleIso();
  document.getElementById('depense-form-title').textContent = 'Ajouter une dépense';
  document.getElementById('annuler-modification').hidden = true;
  document.getElementById('suggestion-categorie').textContent = '';
}
