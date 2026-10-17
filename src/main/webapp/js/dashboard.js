document.addEventListener('DOMContentLoaded', async () => {
  try {
    const dashboard = await apiFetch('/dashboard');
    renderDashboard(dashboard);
  } catch (error) {
    afficherMessage(error.message, 'error');
  }
});

function renderDashboard(data) {
  document.getElementById('total-mois').textContent = formatageMonnaie(data?.resume?.totalMois);
  document.getElementById('budget-consomme').textContent = formatageMonnaie(data?.resume?.budgetUtilise);
  document.getElementById('dernieres-depenses').textContent = String((data?.dernieresDepenses || []).length);

  const chartCtx = document.getElementById('chart-categories');
  if (chartCtx && data?.repartitionParCategorie?.length && window.Chart) {
    new Chart(chartCtx, {
      type: 'pie',
      data: {
        labels: data.repartitionParCategorie.map((item) => item.nom),
        datasets: [{
          data: data.repartitionParCategorie.map((item) => item.montant),
          backgroundColor: ['#4f46e5', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316']
        }]
      }
    });
  } else if (chartCtx) {
    chartCtx.insertAdjacentHTML('afterend', '<p class="empty-state">Aucune dépense enregistrée pour ce mois.</p>');
  }

  const evolution = data?.evolutionMois || [];
  const evolutionCanvas = document.getElementById('chart-evolution');
  if (evolutionCanvas && evolution.length && window.Chart) {
    new Chart(evolutionCanvas, {
      type: 'line',
      data: {
        labels: evolution.map((point) => point.mois),
        datasets: [{ label: 'Dépenses XOF', data: evolution.map((point) => Number(point.total)), borderColor: '#153c7c', tension: 0.25 }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  const budgets = document.getElementById('budgets-list');
  budgets.replaceChildren();
  (data?.etatBudgets || []).forEach((budget) => {
    const ligne = document.createElement('div');
    ligne.className = 'card';
    const titre = document.createElement('strong');
    titre.textContent = `Catégorie ${budget.categorieId} : ${formatageMonnaie(budget.montantConsomme)} / ${formatageMonnaie(budget.plafond)}`;
    const barre = document.createElement('progress');
    barre.max = 100;
    barre.value = Math.min(100, Number(budget.pourcentage || 0));
    const reste = document.createElement('div');
    reste.textContent = `${Number(budget.pourcentage || 0).toFixed(1)} % consommé — reste ${formatageMonnaie(budget.reste)}`;
    ligne.append(titre, barre, reste);
    budgets.append(ligne);
  });
  if (!budgets.childElementCount) budgets.textContent = 'Aucun budget enregistré.';

  const recents = document.getElementById('recent-expenses-list');
  recents.replaceChildren();
  (data?.dernieresDepenses || []).forEach((depense) => {
    const item = document.createElement('li');
    item.textContent = `${depense.dateDepense || ''} — ${depense.libelle || 'Dépense'} — ${formatageMonnaie(depense.montantXof)}`;
    recents.append(item);
  });
  if (!recents.childElementCount) recents.textContent = 'Aucune dépense récente.';
}
