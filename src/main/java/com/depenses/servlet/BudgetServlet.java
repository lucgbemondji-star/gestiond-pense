package com.depenses.servlet;

import com.depenses.dao.JdbcBudgetDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.dao.JdbcCategorieDao;
import com.depenses.model.Budget;
import com.depenses.model.Depense;
import com.depenses.service.BudgetService;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/budgets", "/api/budgets/*"})
public class BudgetServlet extends ApiServlet {
    private final BudgetService service;
    private final JdbcBudgetDao budgetDao;
    private final JdbcDepenseDao depenseDao;
    private final JdbcCategorieDao categorieDao = new JdbcCategorieDao();

    public BudgetServlet() {
        this(new BudgetService(new JdbcBudgetDao()), new JdbcBudgetDao(), new JdbcDepenseDao());
    }

    private void verifierCategorie(Long categorieId, Long utilisateurId) {
        if (categorieId == null) {
            throw new ValidationException("La catégorie du budget est obligatoire.");
        }
        var categorie = categorieDao.trouverParId(categorieId)
                .orElseThrow(() -> new ValidationException("La catégorie du budget est introuvable."));
        if (categorie.getUtilisateurId() != null && !categorie.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("La catégorie du budget ne vous appartient pas.");
        }
    }

    public BudgetServlet(BudgetService service, JdbcBudgetDao budgetDao, JdbcDepenseDao depenseDao) {
        this.service = service;
        this.budgetDao = budgetDao;
        this.depenseDao = depenseDao;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (chemin.equals("/api/budgets") || chemin.equals("/api/budgets/")) {
            reponseJson(response, true, budgetDao.listerParUtilisateur(utilisateurId), null, 200);
            return;
        }
        if (chemin.matches("/api/budgets/[0-9]+/etat")) {
            Long id = Long.parseLong(chemin.substring("/api/budgets/".length(), chemin.lastIndexOf('/')));
            Budget budget = budgetDao.trouverParId(id)
                    .orElseThrow(() -> new ValidationException("Budget introuvable."));
            if (!budget.getUtilisateurId().equals(utilisateurId)) {
                throw new ValidationException("Le budget est introuvable.");
            }
            List<Depense> depenses = depenseDao.listerParUtilisateur(utilisateurId);
            BudgetService.EtatBudget etat = service.calculerEtat(budget, depenses);
            reponseJson(response, true, Map.of(
                    "budgetId", budget.getId(),
                    "categorieId", budget.getCategorieId(),
                    "montantConsomme", etat.montantConsomme(),
                    "pourcentage", etat.pourcentage(),
                    "reste", etat.reste(),
                    "joursRestants", etat.joursRestants()
            ), null, 200);
            return;
        }
        reponseJson(response, false, null, "Route non supportée.", 404);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        Map<String, Object> payload = lireCorpsJson(request);
        Budget budget = new Budget();
        budget.setUtilisateurId(utilisateurId);
        budget.setCategorieId(valeurLong(payload.get("categorieId")));
        budget.setPeriode(parserPeriode(payload.get("periode")));
        budget.setPlafondXof(valeurBigDecimal(payload.get("plafondXof")));
        budget.setDateDebut(valeurDate(payload.get("dateDebut")));
        verifierCategorie(budget.getCategorieId(), utilisateurId);
        reponseJson(response, true, service.creer(budget), null, 201);
    }

    private Budget.Periode parserPeriode(Object valeur) {
        try {
            return Budget.Periode.valueOf(String.valueOf(valeur));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("La période du budget doit être HEBDO ou MENSUEL.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/budgets/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/budgets/".length()));
        Map<String, Object> payload = lireCorpsJson(request);
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUtilisateurId(utilisateurId);
        budget.setCategorieId(valeurLong(payload.get("categorieId")));
        budget.setPeriode(parserPeriode(payload.get("periode")));
        budget.setPlafondXof(valeurBigDecimal(payload.get("plafondXof")));
        budget.setDateDebut(valeurDate(payload.get("dateDebut")));
        verifierCategorie(budget.getCategorieId(), utilisateurId);
        reponseJson(response, true, service.modifier(budget, utilisateurId), null, 200);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/budgets/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/budgets/".length()));
        service.supprimer(id, utilisateurId);
        reponseJson(response, true, null, null, 200);
    }
}
