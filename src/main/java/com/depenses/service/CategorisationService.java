package com.depenses.service;

import com.depenses.model.Categorie;
import com.depenses.model.RegleCategorie;
import com.depenses.util.ValidationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CategorisationService {
    public SuggestionCategorie suggererCategorie(String libelle, Long utilisateurId,
                                               List<RegleCategorie> reglesUtilisateur,
                                               List<RegleCategorie> reglesParDefaut,
                                               List<Categorie> categories) {
        if (libelle == null || libelle.isBlank()) {
            throw new ValidationException("Le libellé est obligatoire pour la suggestion de catégorie.");
        }

        String texteNormalise = normaliser(libelle);
        List<ScoreCategorie> scores = new ArrayList<>();

        List<RegleCategorie> regles = new ArrayList<>();
        if (reglesUtilisateur != null) {
            regles.addAll(reglesUtilisateur);
        }
        if (reglesParDefaut != null) {
            regles.addAll(reglesParDefaut);
        }

        for (RegleCategorie regle : regles) {
            if (regle == null || regle.getMotCle() == null || regle.getCategorieId() == null) {
                continue;
            }
            if (texteNormalise.contains(normaliser(regle.getMotCle()))) {
                int poids = regle.getPoids() == null ? 1 : regle.getPoids();
                scores.add(new ScoreCategorie(regle.getCategorieId(), poids));
            }
        }

        if (scores.isEmpty()) {
            long idAutres = categories == null || categories.isEmpty() ? 8L : categories.stream()
                    .filter(c -> "Autres".equalsIgnoreCase(c.getNom()))
                    .map(Categorie::getId)
                    .findFirst()
                    .orElse(8L);
            return new SuggestionCategorie(idAutres, 0.15d, "Aucune règle évidente détectée, catégorie par défaut proposée.");
        }

        ScoreCategorie gagnant = scores.stream()
                .collect(Collectors.groupingBy(ScoreCategorie::categorieId, Collectors.summingInt(ScoreCategorie::score)))
                .entrySet()
                .stream()
                .map(entry -> new ScoreCategorie(entry.getKey(), entry.getValue()))
                .max(Comparator.comparingInt(ScoreCategorie::score))
                .orElseThrow();

        double confiance = Math.min(0.95d, 0.25d + (gagnant.score() * 0.075d));
        return new SuggestionCategorie(gagnant.categorieId(), confiance, "Règle détectée avec score de confiance calculé.");
    }

    public RegleCategorie enregistrerOuRenforcerRegle(Long utilisateurId, String libelle, Long categorieId, List<RegleCategorie> reglesExistantes) {
        if (utilisateurId == null || categorieId == null || libelle == null || libelle.isBlank()) {
            throw new ValidationException("Les informations pour enregistrer une règle sont incomplètes.");
        }

        String motCleBrut = normaliser(libelle).replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (motCleBrut.isEmpty()) {
            throw new ValidationException("Le mot-clé à retenir est vide après normalisation.");
        }

        String[] mots = motCleBrut.split("\\s+");
        String motCle = mots.length > 1 ? mots[0] : motCleBrut;

        if (reglesExistantes != null) {
            for (RegleCategorie regle : reglesExistantes) {
                if (regle != null && regle.getUtilisateurId() != null && regle.getUtilisateurId().equals(utilisateurId)
                        && regle.getMotCle() != null && normaliser(regle.getMotCle()).equals(motCle)) {
                    regle.setCategorieId(categorieId);
                    regle.setPoids((regle.getPoids() == null ? 1 : regle.getPoids()) + 1);
                    return regle;
                }
            }
        }

        RegleCategorie nouvelleRegle = new RegleCategorie();
        nouvelleRegle.setUtilisateurId(utilisateurId);
        nouvelleRegle.setMotCle(motCle);
        nouvelleRegle.setCategorieId(categorieId);
        nouvelleRegle.setPoids(1);
        return nouvelleRegle;
    }

    private String normaliser(String valeur) {
        return valeur.toLowerCase(Locale.FRENCH)
                .replaceAll("[àâäáã]", "a")
                .replaceAll("[ç]", "c")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[îïìí]", "i")
                .replaceAll("[ôöòóõ]", "o")
                .replaceAll("[ùûüú]", "u")
                .replaceAll("[yÿ]", "y")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record SuggestionCategorie(Long categorieId, double confiance, String message) {
    }

    private record ScoreCategorie(Long categorieId, int score) {
    }
}
