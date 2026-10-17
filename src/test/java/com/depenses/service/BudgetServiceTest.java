package com.depenses.service;

import com.depenses.model.Budget;
import com.depenses.model.Depense;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class BudgetServiceTest {
    private final BudgetService service = new BudgetService();

    @Test
    void calculeLEtatDuBudgetAvecMontantConsommeEtReste() {
        Budget budget = new Budget(1L, 10L, 20L, Budget.Periode.MENSUEL, new BigDecimal("5000.00"), LocalDate.now().withDayOfMonth(1));

        Depense depense1 = new Depense();
        depense1.setCategorieId(20L);
        depense1.setDateDepense(LocalDate.now());
        depense1.setMontantXof(new BigDecimal("1200.00"));

        Depense depense2 = new Depense();
        depense2.setCategorieId(20L);
        depense2.setDateDepense(LocalDate.now().plusDays(5));
        depense2.setMontantXof(new BigDecimal("800.00"));

        Depense depenseAutreCategorie = new Depense();
        depenseAutreCategorie.setCategorieId(99L);
        depenseAutreCategorie.setDateDepense(LocalDate.now());
        depenseAutreCategorie.setMontantXof(new BigDecimal("5000.00"));

        BudgetService.EtatBudget etat = service.calculerEtat(budget, List.of(depense1, depense2, depenseAutreCategorie));

        Assertions.assertEquals(new BigDecimal("2000.00"), etat.montantConsomme());
        Assertions.assertEquals(new BigDecimal("40.00"), etat.pourcentage());
        Assertions.assertEquals(new BigDecimal("3000.00"), etat.reste());
        Assertions.assertTrue(etat.joursRestants() >= 0);
    }
}
