let rapportCourant = null;

document.addEventListener('DOMContentLoaded', () => {
  const mois = document.getElementById('rapportMois');
  mois.value = new Date().toISOString().slice(0, 7);
  document.getElementById('rapport-form').addEventListener('submit', (event) => {
    event.preventDefault();
    chargerRapport();
  });
  document.getElementById('download-csv').addEventListener('click', exporterCsv);
  document.getElementById('download-pdf').addEventListener('click', exporterPdf);
  chargerRapport();
});

async function chargerRapport() {
  const valeur = document.getElementById('rapportMois').value;
  if (!/^\d{4}-\d{2}$/.test(valeur)) {
    afficherMessage('Choisissez un mois valide.', 'error');
    return;
  }
  const [annee, mois] = valeur.split('-');
  try {
    rapportCourant = await apiFetch(`/rapports/mensuel?annee=${annee}&mois=${mois}`);
    afficherRapport();
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
}

function afficherRapport() {
  const resultat = document.getElementById('rapport-result');
  resultat.replaceChildren();
  if (!rapportCourant) return;
  const totaux = Object.entries(rapportCourant.donnees || {}).sort(([a], [b]) => a.localeCompare(b));
  let total = 0;
  const titre = document.createElement('h3');
  titre.textContent = `Rapport de ${String(rapportCourant.mois).padStart(2, '0')}/${rapportCourant.annee}`;
  const table = document.createElement('table');
  const head = document.createElement('tr');
  head.append(new TableCell('Date', 'th'), new TableCell('Total dépensé (XOF)', 'th'));
  table.append(head);
  totaux.forEach(([date, montant]) => {
    const ligne = document.createElement('tr');
    ligne.append(new TableCell(date), new TableCell(formatageMonnaie(montant)));
    table.append(ligne);
    total += Number(montant);
  });
  if (!totaux.length) {
    const vide = document.createElement('p');
    vide.textContent = 'Aucune dépense enregistrée sur cette période.';
    resultat.append(titre, vide);
    return;
  }
  const resume = document.createElement('p');
  resume.textContent = `Total du mois : ${formatageMonnaie(total)}`;
  resultat.append(titre, resume, table);
}

function TableCell(text, tag = 'td') {
  const cell = document.createElement(tag);
  cell.textContent = text;
  return cell;
}

function exporterCsv() {
  if (!rapportCourant) return afficherMessage('Générez d’abord un rapport.', 'error');
  const lignes = [['Date', 'Total dépensé (XOF)']];
  Object.entries(rapportCourant.donnees || {}).sort(([a], [b]) => a.localeCompare(b))
    .forEach(([date, montant]) => lignes.push([date, String(montant)]));
  const csv = '\uFEFF' + lignes.map((ligne) => ligne.map((valeur) => `"${String(valeur).replaceAll('"', '""')}"`).join(';')).join('\r\n');
  telecharger(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `rapport-${rapportCourant.annee}-${String(rapportCourant.mois).padStart(2, '0')}.csv`);
}

function exporterPdf() {
  if (!rapportCourant) return afficherMessage('Générez d’abord un rapport.', 'error');
  window.print();
}

function telecharger(blob, nom) {
  const lien = document.createElement('a');
  const url = URL.createObjectURL(blob);
  lien.href = url;
  lien.download = nom;
  lien.click();
  URL.revokeObjectURL(url);
}
