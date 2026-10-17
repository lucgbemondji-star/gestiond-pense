package com.depenses.servlet;

import com.depenses.dao.JdbcCompteDao;
import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.model.Depense;
import com.depenses.service.DepenseService;
import com.depenses.service.TauxChangeService;
import com.depenses.client.HttpTauxChangeClient;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/depenses", "/api/depenses/*"})
public class DepenseServlet extends ApiServlet {
    private final DepenseService service;

    public DepenseServlet() {
        this(new DepenseService(new JdbcDepenseDao(), new JdbcCompteDao(), new JdbcCategorieDao(),
                new TauxChangeService(new HttpTauxChangeClient())));
    }

    public DepenseServlet(DepenseService service) {
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
        if (chemin.equals("/api/depenses") || chemin.equals("/api/depenses/")) {
            List<Depense> depenses = service.listerParUtilisateur(utilisateurId);
            reponseJson(response, true, filtrer(depenses, request), null, 200);
            return;
        }
        if (chemin.matches("/api/depenses/[0-9]+")) {
            Long id = Long.parseLong(chemin.substring("/api/depenses/".length()));
            Depense depense = service.listerParUtilisateur(utilisateurId).stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Dépense introuvable."));
            reponseJson(response, true, depense, null, 200);
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
        String chemin = extrairePath(request);
        if (!chemin.equals("/api/depenses") && !chemin.equals("/api/depenses/")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Map<String, Object> payload = lireCorpsJson(request);
        Depense depense = construireDepense(payload, utilisateurId, null);
        Depense creee = service.creer(depense);
        reponseJson(response, true, creee, null, 201);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/depenses/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/depenses/".length()));
        Map<String, Object> payload = lireCorpsJson(request);
        Depense depense = construireDepense(payload, utilisateurId, id);
        depense.setId(id);
        Depense miseAJour = service.modifier(depense);
        reponseJson(response, true, miseAJour, null, 200);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.matches("/api/depenses/[0-9]+")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Long id = Long.parseLong(chemin.substring("/api/depenses/".length()));
        service.supprimer(id, utilisateurId);
        reponseJson(response, true, null, null, 200);
    }

    private Depense construireDepense(Map<String, Object> payload, Long utilisateurId, Long id) {
        Depense depense = new Depense();
        depense.setId(id);
        depense.setUtilisateurId(utilisateurId);
        depense.setCompteId(valeurLong(payload.get("compteId")));
        depense.setCategorieId(valeurLong(payload.get("categorieId")));
        depense.setMontant(valeurBigDecimal(payload.get("montant")));
        depense.setDevise(valeurTexte(payload.get("devise")));
        depense.setDateDepense(valeurDate(payload.get("dateDepense")));
        depense.setLibelle(valeurTexte(payload.get("libelle")));
        depense.setNote(valeurTexte(payload.get("note")));
        depense.setRecurrenteId(valeurLong(payload.get("recurrenteId")));
        if (depense.getMontant() != null && depense.getDevise() == null) {
            depense.setDevise("XOF");
        }
        return depense;
    }

    private List<Depense> filtrer(List<Depense> depenses, HttpServletRequest request) {
        String categorie = request.getParameter("categorieId");
        String compte = request.getParameter("compteId");
        String min = request.getParameter("montantMin");
        String max = request.getParameter("montantMax");
        String recherche = request.getParameter("recherche");
        String dateDebut = request.getParameter("dateDebut");
        String dateFin = request.getParameter("dateFin");

        List<Depense> resultats = new ArrayList<>();
        for (Depense depense : depenses) {
            if (categorie != null && !categorie.isBlank() && !depense.getCategorieId().equals(Long.parseLong(categorie))) {
                continue;
            }
            if (compte != null && !compte.isBlank() && !depense.getCompteId().equals(Long.parseLong(compte))) {
                continue;
            }
            if (min != null && !min.isBlank() && depense.getMontantXof() != null && depense.getMontantXof().compareTo(new BigDecimal(min)) < 0) {
                continue;
            }
            if (max != null && !max.isBlank() && depense.getMontantXof() != null && depense.getMontantXof().compareTo(new BigDecimal(max)) > 0) {
                continue;
            }
            if (recherche != null && !recherche.isBlank() && depense.getLibelle() != null && !depense.getLibelle().toLowerCase().contains(recherche.toLowerCase())) {
                continue;
            }
            if (dateDebut != null && !dateDebut.isBlank() && depense.getDateDepense() != null && depense.getDateDepense().isBefore(LocalDate.parse(dateDebut))) {
                continue;
            }
            if (dateFin != null && !dateFin.isBlank() && depense.getDateDepense() != null && depense.getDateDepense().isAfter(LocalDate.parse(dateFin))) {
                continue;
            }
            resultats.add(depense);
        }
        return resultats;
    }
}
