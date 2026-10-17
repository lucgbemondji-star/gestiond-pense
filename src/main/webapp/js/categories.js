let categories = [];

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('categorie-form');
  form.addEventListener('submit', enregistrerCategorie);
  document.getElementById('annuler-categorie').addEventListener('click', reinitialiserCategorie);
  await chargerCategories();
});

async function chargerCategories() {
  try {
    categories = await apiFetch('/categories');
    const select = document.getElementById('categorieParent');
    const valeur = select.value;
    select.replaceChildren(new Option('Aucune', ''));
    categories.forEach((categorie) => select.add(new Option(categorie.nom, categorie.id)));
    select.value = valeur;

    const liste = document.getElementById('categories-list');
    liste.replaceChildren();
    categories.forEach((categorie) => {
      const ligne = document.createElement('div');
      ligne.className = 'card';
      const nom = document.createElement('strong');
      nom.textContent = categorie.nom;
      const origine = document.createElement('span');
      origine.className = 'badge';
      origine.textContent = categorie.utilisateurId == null ? 'Par défaut' : 'Personnelle';
      ligne.append(nom, origine);
      if (categorie.utilisateurId != null) {
        const modifier = document.createElement('button');
        modifier.type = 'button';
        modifier.className = 'secondary';
        modifier.textContent = 'Modifier';
        modifier.addEventListener('click', () => editerCategorie(categorie));
        const supprimer = document.createElement('button');
        supprimer.type = 'button';
        supprimer.className = 'danger';
        supprimer.textContent = 'Supprimer';
        supprimer.addEventListener('click', () => supprimerCategorie(categorie));
        ligne.append(modifier, supprimer);
      }
      liste.append(ligne);
    });
    if (!categories.length) liste.textContent = 'Aucune catégorie disponible.';
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

async function enregistrerCategorie(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = new FormData(form);
  const id = values.get('id');
  try {
    await apiFetch(id ? `/categories/${id}` : '/categories', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify({
        nom: values.get('nom'),
        parentId: values.get('parentId') ? Number(values.get('parentId')) : null
      })
    });
    afficherMessage(id ? 'Catégorie modifiée.' : 'Catégorie créée.', 'success');
    reinitialiserCategorie();
    await chargerCategories();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function editerCategorie(categorie) {
  const form = document.getElementById('categorie-form');
  form.elements.id.value = categorie.id;
  form.elements.nom.value = categorie.nom;
  form.elements.parentId.value = categorie.parentId || '';
  document.getElementById('annuler-categorie').hidden = false;
  form.scrollIntoView({ behavior: 'smooth' });
}

async function supprimerCategorie(categorie) {
  if (!confirm(`Supprimer la catégorie « ${categorie.nom} » ?`)) return;
  try {
    await apiFetch(`/categories/${categorie.id}`, { method: 'DELETE' });
    afficherMessage('Catégorie supprimée.', 'success');
    await chargerCategories();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function reinitialiserCategorie() {
  const form = document.getElementById('categorie-form');
  form.reset();
  form.elements.id.value = '';
  document.getElementById('annuler-categorie').hidden = true;
}
