package com.depenses.servlet;

import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcCompteDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.dao.JdbcDepenseRecurrenteDao;
import com.depenses.model.DepenseRecurrente;
import com.depenses.service.DepenseRecurrenteService;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/depenses-recurrentes", "/api/depenses-recurrentes/*"})
public class DepenseRecurrenteServlet extends ApiServlet {
    private final DepenseRecurrenteService service;

    public DepenseRecurrenteServlet() {
        this(new DepenseRecurrenteService(new JdbcDepenseRecurrenteDao(), new JdbcDepenseDao(),
                new JdbcCompteDao(), new JdbcCategorieDao()));
    }

    public DepenseRecurrenteServlet(DepenseRecurrenteService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        String chemin = extrairePath(request);
        if (chemin.equals("/api/depenses-recurrentes") || chemin.equals("/api/depenses-recurrentes/")) {
            reponseJson(response, true, service.listerParUtilisateur(utilisateurId), null, 200);
            return;
        }
        throw new ValidationException("Route non supportée.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        if (!estCollection(request)) {
            throw new ValidationException("Route non supportée.");
        }
        DepenseRecurrente depense = construire(lireCorpsJson(request), utilisateurId, null);
        reponseJson(response, true, service.creer(depense), null, 201);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        Long id = extraireId(request);
        DepenseRecurrente depense = construire(lireCorpsJson(request), utilisateurId, id);
        reponseJson(response, true, service.modifier(depense, utilisateurId), null, 200);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        service.supprimer(extraireId(request), utilisateurId);
        reponseJson(response, true, null, null, 200);
    }

    private boolean estCollection(HttpServletRequest request) {
        String chemin = extrairePath(request);
        return chemin.equals("/api/depenses-recurrentes") || chemin.equals("/api/depenses-recurrentes/");
    }

    private Long extraireId(HttpServletRequest request) {
        String chemin = extrairePath(request);
        if (!chemin.matches("/api/depenses-recurrentes/[0-9]+")) {
            throw new ValidationException("L'identifiant de dépense récurrente est invalide.");
        }
        return Long.parseLong(chemin.substring("/api/depenses-recurrentes/".length()));
    }

    private DepenseRecurrente construire(Map<String, Object> payload, Long utilisateurId, Long id) {
        DepenseRecurrente depense = new DepenseRecurrente();
        depense.setId(id);
        depense.setUtilisateurId(utilisateurId);
        depense.setCompteId(valeurLong(payload.get("compteId")));
        depense.setCategorieId(valeurLong(payload.get("categorieId")));
        depense.setMontant(valeurBigDecimal(payload.get("montant")));
        depense.setLibelle(valeurTexte(payload.get("libelle")));
        try {
            depense.setFrequence(DepenseRecurrente.Frequence.valueOf(valeurTexte(payload.get("frequence"))));
        } catch (RuntimeException e) {
            throw new ValidationException("La fréquence doit être HEBDO ou MENSUEL.");
        }
        depense.setProchaineDate(valeurDate(payload.get("prochaineDate")));
        Object active = payload.get("active");
        depense.setActive(active == null || Boolean.parseBoolean(String.valueOf(active)));
        return depense;
    }
}
