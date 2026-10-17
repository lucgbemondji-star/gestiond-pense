package com.depenses.servlet;

import com.depenses.dao.JdbcBudgetDao;
import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.model.Budget;
import com.depenses.model.Categorie;
import com.depenses.model.Depense;
import com.depenses.service.BudgetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/api/dashboard")
public class DashboardServlet extends ApiServlet {
    private final JdbcDepenseDao depenseDao = new JdbcDepenseDao();
    private final JdbcBudgetDao budgetDao = new JdbcBudgetDao();
    private final JdbcCategorieDao categorieDao = new JdbcCategorieDao();
    private final BudgetService budgetService = new BudgetService(budgetDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        List<Depense> depenses = depenseDao.listerParUtilisateur(utilisateurId);
        List<Budget> budgets = budgetDao.listerParUtilisateur(utilisateurId);

        BigDecimal totalMois = BigDecimal.ZERO;
        LocalDate aujourdHui = LocalDate.now();
        LocalDate debutMois = aujourdHui.withDayOfMonth(1);
        LocalDate finMois = aujourdHui.with(TemporalAdjusters.lastDayOfMonth());
        for (Depense depense : depenses) {
            if (depense.getDateDepense() != null && !depense.getDateDepense().isBefore(debutMois)
                    && !depense.getDateDepense().isAfter(finMois) && depense.getMontantXof() != null) {
                totalMois = totalMois.add(depense.getMontantXof());
            }
        }

        Map<Long, BigDecimal> parCategorie = new HashMap<>();
        for (Depense depense : depenses) {
            if (depense.getDateDepense() != null && !depense.getDateDepense().isBefore(debutMois)
                    && !depense.getDateDepense().isAfter(finMois) && depense.getMontantXof() != null) {
                parCategorie.merge(depense.getCategorieId(), depense.getMontantXof(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> repartition = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : parCategorie.entrySet()) {
            Categorie categorie = categorieDao.trouverParId(entry.getKey()).orElse(new Categorie());
            Map<String, Object> donnees = new HashMap<>();
            donnees.put("categorieId", entry.getKey());
            donnees.put("nom", categorie.getNom() == null ? "Autre" : categorie.getNom());
            donnees.put("montant", entry.getValue());
            repartition.add(donnees);
        }

        List<Map<String, Object>> evolution = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth mois = YearMonth.now().minusMonths(i);
            BigDecimal total = BigDecimal.ZERO;
            for (Depense depense : depenses) {
                if (depense.getDateDepense() != null && YearMonth.from(depense.getDateDepense()).equals(mois) && depense.getMontantXof() != null) {
                    total = total.add(depense.getMontantXof());
                }
            }
            Map<String, Object> point = new HashMap<>();
            point.put("mois", mois.toString());
            point.put("total", total);
            evolution.add(point);
        }

        List<Map<String, Object>> etatBudgets = new ArrayList<>();
        for (Budget budget : budgets) {
            BudgetService.EtatBudget etat = budgetService.calculerEtat(budget, depenses);
            Map<String, Object> donnee = new HashMap<>();
            donnee.put("budgetId", budget.getId());
            donnee.put("categorieId", budget.getCategorieId());
            donnee.put("plafond", budget.getPlafondXof());
            donnee.put("montantConsomme", etat.montantConsomme());
            donnee.put("pourcentage", etat.pourcentage());
            donnee.put("reste", etat.reste());
            etatBudgets.add(donnee);
        }

        List<Depense> dernieres = depenses.stream()
                .sorted(Comparator.comparing(Depense::getDateDepense, Comparator.nullsLast(LocalDate::compareTo)).reversed())
                .limit(5)
                .toList();

        Map<String, Object> resume = new HashMap<>();
        resume.put("totalMois", totalMois);
        resume.put("budgetUtilise", etatBudgets.stream().map(m -> (BigDecimal) m.get("montantConsomme")).reduce(BigDecimal.ZERO, BigDecimal::add));
        resume.put("count", depenses.size());

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("resume", resume);
        resultat.put("repartitionParCategorie", repartition);
        resultat.put("evolutionMois", evolution);
        resultat.put("etatBudgets", etatBudgets);
        resultat.put("dernieresDepenses", dernieres);
        reponseJson(response, true, resultat, null, 200);
    }
}
