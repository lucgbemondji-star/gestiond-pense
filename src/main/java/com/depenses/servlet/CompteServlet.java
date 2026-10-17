package com.depenses.servlet;

import com.depenses.dao.JdbcCompteDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.model.Compte;
import com.depenses.service.CompteService;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/comptes", "/api/comptes/*"})
public class CompteServlet extends ApiServlet {
    private final CompteService service;

    public CompteServlet() {
        this(new CompteService(new JdbcCompteDao(), new JdbcDepenseDao()));
    }

    public CompteServlet(CompteService service) {
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
        if (chemin.equals("/api/comptes") || chemin.equals("/api/comptes/")) {
            List<Compte> comptes = service.listerParUtilisateur(utilisateurId);
            for (Compte compte : comptes) {
                compte.setSoldeCourant(service.calculerSoldeCourant(compte.getId()));
            }
            reponseJson(response, true, comptes, null, 200);
            return;
        }
        if (chemin.matches("/api/comptes/[0-9]+")) {
            Long id = Long.parseLong(chemin.substring("/api/comptes/".length()));
            Compte compte = service.listerParUtilisateur(utilisateurId).stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Compte introuvable."));
            compte.setSoldeCourant(service.calculerSoldeCourant(id));
            reponseJson(response, true, compte, null, 200);
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
        Compte compte = new Compte();
        compte.setUtilisateurId(utilisateurId);
        compte.setNom(valeurTexte(payload.get("nom")));
        compte.setType(parserType(payload.get("type")));
        compte.setDevise(valeurTexte(payload.get("devise")));
        compte.setSoldeInitial(valeurBigDecimal(payload.get("soldeInitial")));
        if (compte.getSoldeInitial() == null) {
            compte.setSoldeInitial(BigDecimal.ZERO);
        }
        reponseJson(response, true, service.creer(compte), null, 201);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/comptes/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/comptes/".length()));
        Map<String, Object> payload = lireCorpsJson(request);
        Compte compte = new Compte();
        compte.setId(id);
        compte.setUtilisateurId(utilisateurId);
        compte.setNom(valeurTexte(payload.get("nom")));
        compte.setType(parserType(payload.get("type")));
        compte.setDevise(valeurTexte(payload.get("devise")));
        compte.setSoldeInitial(valeurBigDecimal(payload.get("soldeInitial")));
        if (compte.getSoldeInitial() == null) {
            compte.setSoldeInitial(BigDecimal.ZERO);
        }

        Compte modifie = service.modifier(compte, utilisateurId);
        reponseJson(response, true, modifie, null, 200);
    }

    private Compte.TypeCompte parserType(Object valeur) {
        try {
            return Compte.TypeCompte.valueOf(String.valueOf(valeur));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Le type de compte doit être ESPECES, MOBILE_MONEY ou BANQUE.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/comptes/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/comptes/".length()));
        service.supprimer(id, utilisateurId);
        reponseJson(response, true, null, null, 200);
    }
}
