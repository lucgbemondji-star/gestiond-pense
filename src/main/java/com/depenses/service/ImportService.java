package com.depenses.service;

import com.depenses.model.Depense;
import com.depenses.util.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImportService {
    public List<String[]> apercuCsv(Path fichierCsv) {
        if (fichierCsv == null || !Files.exists(fichierCsv)) {
            throw new ValidationException("Le fichier CSV à prévisualiser est introuvable.");
        }

        List<String[]> lignes = new ArrayList<>();
        try {
            List<String> contenu = Files.readAllLines(fichierCsv);
            char separateur = contenu.stream().filter(ligne -> !ligne.isBlank()).findFirst()
                    .map(this::separateurCsv).orElse(',');
            for (String ligne : contenu) {
                if (!ligne.isBlank()) {
                    lignes.add(decouperCsv(ligne, separateur));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier CSV pour l'aperçu.", e);
        }
        return lignes;
    }

    private char separateurCsv(String ligne) {
        int virgules = 0;
        int pointsVirgules = 0;
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char caractere = ligne.charAt(i);
            if (caractere == '"') {
                if (entreGuillemets && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    i++;
                } else {
                    entreGuillemets = !entreGuillemets;
                }
            } else if (!entreGuillemets && caractere == ',') {
                virgules++;
            } else if (!entreGuillemets && caractere == ';') {
                pointsVirgules++;
            }
        }
        return pointsVirgules > virgules ? ';' : ',';
    }

    private String[] decouperCsv(String ligne, char separateur) {
        List<String> valeurs = new ArrayList<>();
        StringBuilder valeur = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char caractere = ligne.charAt(i);
            if (caractere == '"') {
                if (entreGuillemets && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    valeur.append('"');
                    i++;
                } else {
                    entreGuillemets = !entreGuillemets;
                }
            } else if (caractere == separateur && !entreGuillemets) {
                valeurs.add(valeur.toString());
                valeur.setLength(0);
            } else {
                valeur.append(caractere);
            }
        }
        if (entreGuillemets) {
            throw new ValidationException("Le fichier CSV contient des guillemets non fermés.");
        }
        valeurs.add(valeur.toString());
        return valeurs.toArray(String[]::new);
    }

    public List<Depense> validerCsv(List<String[]> lignesImport, Long utilisateurId) {
        if (lignesImport == null || lignesImport.isEmpty()) {
            throw new ValidationException("Aucune donnée CSV n'a été fournie pour validation.");
        }

        List<Depense> depenses = new ArrayList<>();
        for (String[] ligne : lignesImport) {
            if (ligne == null || ligne.length < 4) {
                continue;
            }
            Depense depense = new Depense();
            depense.setUtilisateurId(utilisateurId);
            depense.setDateDepense(java.time.LocalDate.parse(ligne[0].trim()));
            depense.setLibelle(ligne[1].trim());
            depense.setMontant(new BigDecimal(ligne[2].trim()));
            depense.setDevise(ligne[3].trim().toUpperCase());
            depenses.add(depense);
        }
        return depenses;
    }
}
