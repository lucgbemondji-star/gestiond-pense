package com.depenses.service;

import com.depenses.model.Categorie;
import com.depenses.model.RegleCategorie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CategorisationServiceTest {
    private final CategorisationService service = new CategorisationService();

    @Test
    void suggereCategorieEnPriorisantLaRegleUtilisateur() {
        Categorie categorieAlimentation = new Categorie(1L, null, "Alimentation", null);
        Categorie categorieTransport = new Categorie(2L, null, "Transport", null);

        RegleCategorie regleUtilisateur = new RegleCategorie(10L, 42L, "supermarche", 1L, 5);
        RegleCategorie regleParDefaut = new RegleCategorie(11L, null, "transport", 2L, 2);

        CategorisationService.SuggestionCategorie suggestion = service.suggererCategorie(
                "Courses au supermarché de la semaine",
                42L,
                List.of(regleUtilisateur),
                List.of(regleParDefaut),
                List.of(categorieAlimentation, categorieTransport)
        );

        Assertions.assertEquals(1L, suggestion.categorieId());
        Assertions.assertTrue(suggestion.confiance() > 0.3d);
        Assertions.assertNotNull(suggestion.message());
    }

    @Test
    void enregistreOuRenforceUneRegleSuiteAUneCorrection() {
        RegleCategorie regleExistante = new RegleCategorie(20L, 7L, "carburant", 2L, 1);

        RegleCategorie regle = service.enregistrerOuRenforcerRegle(7L, "Carburant à la station", 2L, List.of(regleExistante));

        Assertions.assertTrue(regle.getMotCle().contains("carburant"));
        Assertions.assertEquals(2L, regle.getCategorieId());
        Assertions.assertEquals(2, regle.getPoids());
    }
}
