package com.depenses.servlet;

import com.depenses.service.ImportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.depenses.dao.JdbcCompteDao;
import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcDepenseDao;
import com.depenses.model.Compte;
import com.depenses.model.Categorie;
import com.depenses.model.Depense;
import com.depenses.service.DepenseService;
import com.depenses.service.TauxChangeService;
import com.depenses.client.HttpTauxChangeClient;
import com.depenses.util.ValidationException;

@WebServlet(urlPatterns = "/api/import/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class ImportServlet extends ApiServlet {
    private final ImportService importService = new ImportService();
    private final JdbcCompteDao compteDao = new JdbcCompteDao();
    private final JdbcCategorieDao categorieDao = new JdbcCategorieDao();
    private final DepenseService depenseService = new DepenseService(
            new JdbcDepenseDao(), compteDao, categorieDao, new TauxChangeService(new HttpTauxChangeClient()));

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (chemin.endsWith("/api/import/csv/apercu")) {
            var part = request.getPart("fichier");
            if (part == null || part.getSize() == 0) {
                reponseJson(response, false, null, "Aucun fichier CSV fourni.", 400);
                return;
            }
            Path fichier = Files.createTempFile("import-csv-", ".csv");
            try (var input = part.getInputStream()) {
                Files.copy(input, fichier, StandardCopyOption.REPLACE_EXISTING);
                List<String[]> lignes = importService.apercuCsv(fichier);
                reponseJson(response, true, Map.of("lignes", lignes, "total", Math.max(0, lignes.size() - 1)), null, 200);
            } finally {
                Files.deleteIfExists(fichier);
            }
            return;
        }
        if (chemin.endsWith("/api/import/csv/valider")) {
            Map<String, Object> payload = lireCorpsJson(request);
            Long compteId = valeurLong(payload.get("compteId"));
            Long categorieId = valeurLong(payload.get("categorieId"));
            Compte compte = compteDao.trouverParId(compteId)
                    .orElseThrow(() -> new ValidationException("Le compte sélectionné est introuvable."));
            if (!utilisateurId.equals(compte.getUtilisateurId())) {
                throw new ValidationException("Le compte sélectionné ne vous appartient pas.");
            }
            Categorie categorie = categorieDao.trouverParId(categorieId)
                    .orElseThrow(() -> new ValidationException("La catégorie sélectionnée est introuvable."));
            if (categorie.getUtilisateurId() != null && !utilisateurId.equals(categorie.getUtilisateurId())) {
                throw new ValidationException("La catégorie sélectionnée ne vous appartient pas.");
            }
            Object brut = payload.get("lignes");
            if (!(brut instanceof List<?> lignesBrutes)) {
                throw new ValidationException("Les lignes du fichier CSV sont absentes.");
            }
            if (lignesBrutes.size() > 5000) {
                throw new ValidationException("Le fichier ne peut pas dépasser 5 000 lignes.");
            }
            List<Depense> depenses = new ArrayList<>();
            int doublons = 0;
            List<Depense> existantes = new JdbcDepenseDao().listerParUtilisateur(utilisateurId);
            for (int index = 0; index < lignesBrutes.size(); index++) {
                Object element = lignesBrutes.get(index);
                if (!(element instanceof List<?> ligne) || ligne.size() < 4) {
                    throw new ValidationException("Une ligne CSV ne contient pas les quatre colonnes attendues.");
                }
                String premiereValeur = String.valueOf(ligne.get(0)).trim().replaceFirst("^\\uFEFF", "");
                if (index == 0 && "date".equalsIgnoreCase(premiereValeur)) continue;
                Depense depense = new Depense();
                depense.setUtilisateurId(utilisateurId);
                depense.setCompteId(compteId);
                depense.setCategorieId(categorieId);
                try {
                    depense.setDateDepense(LocalDate.parse(String.valueOf(ligne.get(0)).trim()));
                    depense.setLibelle(String.valueOf(ligne.get(1)).trim());
                    depense.setMontant(new BigDecimal(String.valueOf(ligne.get(2)).trim()));
                    depense.setDevise(String.valueOf(ligne.get(3)).trim().toUpperCase());
                } catch (RuntimeException e) {
                    throw new ValidationException("Une ligne CSV contient une date ou un montant invalide.");
                }
                if (depense.getLibelle().length() > 200) {
                    throw new ValidationException("Un libellé CSV dépasse la limite de 200 caractères.");
                }
                if (depense.getLibelle().isBlank()) {
                    throw new ValidationException("Un libellé CSV est vide.");
                }
                boolean doublon = existantes.stream().anyMatch(existante ->
                        depense.getDateDepense().equals(existante.getDateDepense())
                                && depense.getLibelle().equalsIgnoreCase(existante.getLibelle())
                                && depense.getMontant().compareTo(existante.getMontant()) == 0);
                if (doublon) doublons++;
                depenses.add(depense);
            }
            if (depenses.isEmpty()) {
                throw new ValidationException("Le fichier CSV ne contient aucune dépense à importer.");
            }
            List<Depense> depensesValidees = depenseService.creerPlusieurs(depenses);
            reponseJson(response, true, Map.of("depenses", depensesValidees, "total", depensesValidees.size(),
                    "doublonsPotentiels", doublons), null, 200);
            return;
        }
        reponseJson(response, false, null, "Route non supportée.", 404);
    }
}
