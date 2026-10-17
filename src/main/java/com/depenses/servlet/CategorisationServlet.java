package com.depenses.servlet;

import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcRegleCategorieDao;
import com.depenses.model.Categorie;
import com.depenses.model.RegleCategorie;
import com.depenses.service.CategorisationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/api/categorisation/*")
public class CategorisationServlet extends ApiServlet {
    private final CategorisationService service = new CategorisationService();
    private final JdbcRegleCategorieDao regleDao = new JdbcRegleCategorieDao();
    private final JdbcCategorieDao categorieDao = new JdbcCategorieDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.endsWith("/api/categorisation/suggestion")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Map<String, Object> payload = lireCorpsJson(request);
        String libelle = valeurTexte(payload.get("libelle"));
        Long categorieChoisie = valeurLong(payload.get("categorieId"));

        List<RegleCategorie> reglesUtilisateur = regleDao.listerParUtilisateur(utilisateurId);
        List<RegleCategorie> reglesParDefaut = null;
        List<Categorie> categories = categorieDao.listerParDefaut();
        categories.addAll(categorieDao.listerParUtilisateur(utilisateurId));

        CategorisationService.SuggestionCategorie suggestion = service.suggererCategorie(libelle, utilisateurId, reglesUtilisateur, reglesParDefaut, categories);
        if (categorieChoisie != null && categorieChoisie > 0 && libelle != null && !libelle.isBlank()) {
            RegleCategorie regle = service.enregistrerOuRenforcerRegle(utilisateurId, libelle, categorieChoisie, reglesUtilisateur);
            reglesUtilisateur.add(regle);
            if (regle.getId() == null || regle.getId() <= 0) {
                regleDao.creer(regle);
            } else {
                regleDao.modifier(regle);
            }
        }

        reponseJson(response, true, Map.of(
                "categorieId", suggestion.categorieId(),
                "confiance", suggestion.confiance(),
                "message", suggestion.message()
        ), null, 200);
    }
}
