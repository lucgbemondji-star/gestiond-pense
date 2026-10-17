package com.depenses.servlet;

import com.depenses.dao.JdbcCategorieDao;
import com.depenses.model.Categorie;
import com.depenses.service.CategorieService;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/categories", "/api/categories/*"})
public class CategorieServlet extends ApiServlet {
    private final CategorieService service;
    private final JdbcCategorieDao categorieDao;

    public CategorieServlet() {
        this(new CategorieService(new JdbcCategorieDao()), new JdbcCategorieDao());
    }

    public CategorieServlet(CategorieService service, JdbcCategorieDao categorieDao) {
        this.service = service;
        this.categorieDao = categorieDao;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (chemin.equals("/api/categories") || chemin.equals("/api/categories/")) {
            List<Categorie> categories = new ArrayList<>();
            categories.addAll(service.listerParDefaut());
            categories.addAll(service.listerParUtilisateur(utilisateurId));
            reponseJson(response, true, categories, null, 200);
            return;
        }
        if (chemin.matches("/api/categories/[0-9]+")) {
            Long id = Long.parseLong(chemin.substring("/api/categories/".length()));
            Categorie categorie = categorieDao.trouverParId(id)
                    .orElseThrow(() -> new ValidationException("Catégorie introuvable."));
            if (categorie.getUtilisateurId() != null && !categorie.getUtilisateurId().equals(utilisateurId)) {
                throw new ValidationException("La catégorie est introuvable.");
            }
            reponseJson(response, true, categorie, null, 200);
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
        Categorie categorie = new Categorie();
        categorie.setUtilisateurId(utilisateurId);
        categorie.setNom(valeurTexte(payload.get("nom")));
        Object objetParent = payload.get("parentId");
        categorie.setParentId(objetParent == null || String.valueOf(objetParent).isBlank() ? null : valeurLong(objetParent));
        service.validerParent(categorie.getParentId(), utilisateurId);
        reponseJson(response, true, service.creer(categorie), null, 201);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/categories/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/categories/".length()));
        Map<String, Object> payload = lireCorpsJson(request);
        Categorie categorie = new Categorie();
        categorie.setId(id);
        categorie.setUtilisateurId(utilisateurId);
        categorie.setNom(valeurTexte(payload.get("nom")));
        Object objetParent = payload.get("parentId");
        categorie.setParentId(objetParent == null || String.valueOf(objetParent).isBlank() ? null : valeurLong(objetParent));
        categorie = service.modifier(categorie, utilisateurId);
        reponseJson(response, true, categorie, null, 200);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/categories/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/categories/".length()));
        service.supprimer(id, utilisateurId);
        reponseJson(response, true, null, null, 200);
    }
}
