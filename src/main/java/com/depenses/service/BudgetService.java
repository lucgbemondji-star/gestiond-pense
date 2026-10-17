package com.depenses.service;

import com.depenses.dao.BudgetDao;
import com.depenses.model.Budget;
import com.depenses.model.Depense;
import com.depenses.util.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BudgetService {
    private final BudgetDao budgetDao;

    public BudgetService() {
        this(null);
    }

    public BudgetService(BudgetDao budgetDao) {
        this.budgetDao = budgetDao;
    }

    public Budget creer(Budget budget) {
        valider(budget);
        if (budgetDao == null) {
            throw new ValidationException("Le DAO de budget n'est pas initialisé.");
        }
        long id = budgetDao.creer(budget);
        budget.setId(id);
        return budget;
    }

    public void supprimer(Long budgetId, Long utilisateurId) {
        if (budgetId == null || budgetId <= 0) {
            throw new ValidationException("L'identifiant du budget est invalide.");
        }
        if (budgetDao == null) {
            throw new ValidationException("Le DAO de budget n'est pas initialisé.");
        }
        Budget budget = budgetDao.trouverParId(budgetId)
                .orElseThrow(() -> new ValidationException("Le budget est introuvable."));
        if (!budget.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à supprimer ce budget.");
        }
        if (!budgetDao.supprimer(budgetId)) {
            throw new ValidationException("La suppression du budget a échoué.");
        }
    }

    public Budget modifier(Budget budget, Long utilisateurId) {
        valider(budget);
        if (budgetDao == null) {
            throw new ValidationException("Le DAO de budget n'est pas initialisé.");
        }
        Budget existant = budgetDao.trouverParId(budget.getId())
                .orElseThrow(() -> new ValidationException("Le budget est introuvable."));
        if (!existant.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à modifier ce budget.");
        }
        budget.setUtilisateurId(utilisateurId);
        if (!budgetDao.modifier(budget)) {
            throw new ValidationException("La modification du budget a échoué.");
        }
        return budget;
    }

    public EtatBudget calculerEtat(Budget budget, List<Depense> depenses) {
        if (budget == null) {
            throw new ValidationException("Le budget est obligatoire.");
        }
        if (budget.getPlafondXof() == null || budget.getPlafondXof().signum() <= 0) {
            throw new ValidationException("Le plafond du budget doit être strictement positif.");
        }
        if (budget.getDateDebut() == null) {
            throw new ValidationException("La date de début du budget est obligatoire.");
        }

        LocalDate maintenant = LocalDate.now();
        LocalDate debut = budget.getDateDebut();
        LocalDate fin = calculerFinPeriode(debut, budget.getPeriode());
        BigDecimal montantConsomme = BigDecimal.ZERO;

        for (Depense depense : depenses) {
            if (depense == null || depense.getCategorieId() == null || depense.getDateDepense() == null) {
                continue;
            }
            if (!depense.getCategorieId().equals(budget.getCategorieId())) {
                continue;
            }
            boolean dansPeriode = !depense.getDateDepense().isBefore(debut) && !depense.getDateDepense().isAfter(fin);
            if (dansPeriode && depense.getMontantXof() != null) {
                montantConsomme = montantConsomme.add(depense.getMontantXof());
            }
        }

        BigDecimal pourcentage = BigDecimal.ZERO;
        if (budget.getPlafondXof().compareTo(BigDecimal.ZERO) > 0) {
            pourcentage = montantConsomme.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getPlafondXof(), 2, RoundingMode.HALF_UP);
        }

        BigDecimal reste = budget.getPlafondXof().subtract(montantConsomme).max(BigDecimal.ZERO);
        long joursRestants = Math.max(0, ChronoUnit.DAYS.between(maintenant, fin));

        return new EtatBudget(montantConsomme, pourcentage, reste, joursRestants);
    }

    private void valider(Budget budget) {
        if (budget == null) {
            throw new ValidationException("Le budget ne peut pas être vide.");
        }
        if (budget.getUtilisateurId() == null || budget.getUtilisateurId() <= 0) {
            throw new ValidationException("Le propriétaire du budget est obligatoire.");
        }
        if (budget.getCategorieId() == null || budget.getCategorieId() <= 0) {
            throw new ValidationException("La catégorie du budget est obligatoire.");
        }
        if (budget.getPeriode() == null) {
            throw new ValidationException("La période du budget est obligatoire.");
        }
        if (budget.getPlafondXof() == null || budget.getPlafondXof().signum() <= 0) {
            throw new ValidationException("Le plafond du budget doit être strictement positif.");
        }
        if (budget.getDateDebut() == null) {
            throw new ValidationException("La date de début du budget est obligatoire.");
        }
    }

    private LocalDate calculerFinPeriode(LocalDate debut, Budget.Periode periode) {
        if (periode == Budget.Periode.HEBDO) {
            return debut.plusWeeks(1);
        }
        return debut.plusMonths(1);
    }

    public record EtatBudget(BigDecimal montantConsomme, BigDecimal pourcentage, BigDecimal reste, long joursRestants) {
    }
}
