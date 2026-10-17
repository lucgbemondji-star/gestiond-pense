let apercuImport = null;

document.addEventListener('DOMContentLoaded', async () => {
  document.getElementById('import-form').addEventListener('submit', previsualiserImport);
  document.getElementById('valider-import').addEventListener('click', validerImport);
  try {
    const [comptes, categories] = await Promise.all([apiFetch('/comptes'), apiFetch('/categories')]);
    remplirOptions(document.getElementById('import-compte'), comptes, 'Choisir un compte');
    remplirOptions(document.getElementById('import-categorie'), categories, 'Choisir une catégorie');
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
});

function remplirOptions(select, elements, invite) {
  select.replaceChildren(new Option(invite, ''));
  elements.forEach((element) => select.add(new Option(element.nom, element.id)));
}

async function previsualiserImport(event) {
  event.preventDefault();
  const fichier = document.querySelector('#import-form input[type="file"]').files[0];
  if (!fichier) {
    afficherMessage('Sélectionnez un fichier CSV.', 'error');
    return;
  }
  const formData = new FormData();
  formData.append('fichier', fichier);
  try {
    apercuImport = await apiFetch('/import/csv/apercu', {
      method: 'POST',
      body: formData,
      headers: {}
    });
    afficherApercu(apercuImport.lignes || []);
    document.getElementById('valider-import').hidden = false;
    afficherMessage(`${apercuImport.total} ligne(s) détectée(s). Vérifiez l’aperçu avant de confirmer.`, 'success');
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function afficherApercu(lignes) {
  const zone = document.getElementById('import-preview');
  zone.replaceChildren();
  const table = document.createElement('table');
  lignes.forEach((ligne) => {
    const tr = document.createElement('tr');
    ligne.forEach((valeur) => {
      const cellule = document.createElement('td');
      cellule.textContent = valeur;
      tr.append(cellule);
    });
    table.append(tr);
  });
  zone.append(table);
}

async function validerImport() {
  const compteId = document.getElementById('import-compte').value;
  const categorieId = document.getElementById('import-categorie').value;
  if (!apercuImport?.lignes?.length || !compteId || !categorieId) {
    afficherMessage('Choisissez un compte et une catégorie, puis prévisualisez un CSV valide.', 'error');
    return;
  }
  try {
    const resultat = await apiFetch('/import/csv/valider', {
      method: 'POST',
      body: JSON.stringify({
        lignes: apercuImport.lignes,
        compteId: Number(compteId),
        categorieId: Number(categorieId)
      })
    });
    const avertissement = Number(resultat.doublonsPotentiels || 0) > 0
      ? ` Attention : ${resultat.doublonsPotentiels} doublon(s) potentiel(s) ont été inclus.`
      : '';
    afficherMessage(`${resultat.total} dépense(s) importée(s).${avertissement}`, 'success');
    document.getElementById('valider-import').hidden = true;
    apercuImport = null;
    document.getElementById('import-preview').textContent = 'Import terminé. Consultez votre historique dans « Dépenses ».';
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}
