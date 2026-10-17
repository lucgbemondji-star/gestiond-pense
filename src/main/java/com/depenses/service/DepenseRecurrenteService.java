package com.depenses.service;

import com.depenses.dao.DepenseDao;
import com.depenses.dao.DepenseRecurrenteDao;
import com.depenses.dao.CompteDao;
import com.depenses.dao.CategorieDao;
import com.depenses.dao.JdbcCategorieDao;
import com.depenses.dao.JdbcCompteDao;
import com.depenses.model.Depense;
import com.depenses.model.DepenseRecurrente;
import com.depenses.util.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DepenseRecurrenteService {
    private final DepenseRecurrenteDao depenseRecurrenteDao;
    private final DepenseDao depenseDao;
    private final CompteDao compteDao;
    private final CategorieDao categorieDao;

    public DepenseRecurrenteService(DepenseRecurrenteDao depenseRecurrenteDao, DepenseDao depenseDao) {
        this(depenseRecurrenteDao, depenseDao, new JdbcCompteDao(), new JdbcCategorieDao());
    }

    public DepenseRecurrenteService(DepenseRecurrenteDao depenseRecurrenteDao, DepenseDao depenseDao,
                                   CompteDao compteDao, CategorieDao categorieDao) {
        this.depenseRecurrenteDao = depenseRecurrenteDao;
        this.depenseDao = depenseDao;
        this.compteDao = compteDao;
        this.categorieDao = categorieDao;
    }

    public DepenseRecurrente creer(DepenseRecurrente depenseRecurrente) {
        valider(depenseRecurrente);
        verifierLiens(depenseRecurrente);
        long id = depenseRecurrenteDao.creer(depenseRecurrente);
        depenseRecurrente.setId(id);
        return depenseRecurrente;
    }

    public List<DepenseRecurrente> listerParUtilisateur(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new ValidationException("L'identifiant utilisateur est invalide.");
        }
        return depenseRecurrenteDao.listerParUtilisateur(utilisateurId);
    }

    public DepenseRecurrente modifier(DepenseRecurrente depenseRecurrente, Long utilisateurId) {
        valider(depenseRecurrente);
        DepenseRecurrente existante = depenseRecurrenteDao.trouverParId(depenseRecurrente.getId())
                .orElseThrow(() -> new ValidationException("La dépense récurrente est introuvable."));
        if (!utilisateurId.equals(existante.getUtilisateurId())) {
            throw new ValidationException("Vous n'êtes pas autorisé à modifier cette dépense récurrente.");
        }
        verifierLiens(depenseRecurrente);
        depenseRecurrente.setUtilisateurId(utilisateurId);
        if (!depenseRecurrenteDao.modifier(depenseRecurrente)) {
            throw new ValidationException("La modification de la dépense récurrente a échoué.");
        }
        return depenseRecurrente;
    }

    public void supprimer(Long depenseRecurrenteId, Long utilisateurId) {
        DepenseRecurrente existante = depenseRecurrenteDao.trouverParId(depenseRecurrenteId)
                .orElseThrow(() -> new ValidationException("La dépense récurrente est introuvable."));
        if (!utilisateurId.equals(existante.getUtilisateurId())) {
            throw new ValidationException("Vous n'êtes pas autorisé à supprimer cette dépense récurrente.");
        }
        if (!depenseRecurrenteDao.supprimer(depenseRecurrenteId)) {
            throw new ValidationException("La suppression de la dépense récurrente a échoué.");
        }
    }

    public void traiterDepensesEchues() {
        List<DepenseRecurrente> depenses = depenseRecurrenteDao.listerParUtilisateur(null);
        for (DepenseRecurrente depense : depenses) {
            if (depense == null || !depense.isActive() || depense.getProchaineDate() == null) {
                continue;
            }
            LocalDate aujourdHui = LocalDate.now();
            while (!aujourdHui.isBefore(depense.getProchaineDate())) {
                Depense nouvelle = new Depense();
                nouvelle.setUtilisateurId(depense.getUtilisateurId());
                nouvelle.setCompteId(depense.getCompteId());
                nouvelle.setCategorieId(depense.getCategorieId());
                nouvelle.setMontant(depense.getMontant());
                nouvelle.setDevise("XOF");
                nouvelle.setMontantXof(depense.getMontant());
                nouvelle.setDateDepense(depense.getProchaineDate());
                nouvelle.setLibelle(depense.getLibelle());
                nouvelle.setRecurrenteId(depense.getId());
                depenseDao.creer(nouvelle);

                depense.setProchaineDate(calculerProchaineDate(depense.getProchaineDate(), depense.getFrequence()));
                if (!depenseRecurrenteDao.modifier(depense)) {
                    throw new IllegalStateException("Impossible d'avancer la prochaine échéance récurrente.");
                }
            }
        }
    }

    private LocalDate calculerProchaineDate(LocalDate date, DepenseRecurrente.Frequence frequence) {
        if (frequence == DepenseRecurrente.Frequence.HEBDO) {
            return date.plusWeeks(1);
        }
        return date.plusMonths(1);
    }

    private void valider(DepenseRecurrente depenseRecurrente) {
        if (depenseRecurrente == null) {
            throw new ValidationException("La dépense récurrente ne peut pas être vide.");
        }
        if (depenseRecurrente.getUtilisateurId() == null || depenseRecurrente.getUtilisateurId() <= 0) {
            throw new ValidationException("Le propriétaire de la dépense récurrente est obligatoire.");
        }
        if (depenseRecurrente.getCompteId() == null || depenseRecurrente.getCompteId() <= 0) {
            throw new ValidationException("Le compte est obligatoire.");
        }
        if (depenseRecurrente.getCategorieId() == null || depenseRecurrente.getCategorieId() <= 0) {
            throw new ValidationException("La catégorie de la dépense récurrente est obligatoire.");
        }
        if (depenseRecurrente.getMontant() == null || depenseRecurrente.getMontant().signum() <= 0) {
            throw new ValidationException("Le montant de la dépense récurrente doit être strictement positif.");
        }
        if (depenseRecurrente.getLibelle() == null || depenseRecurrente.getLibelle().isBlank()) {
            throw new ValidationException("Le libellé de la dépense récurrente est obligatoire.");
        }
        if (depenseRecurrente.getFrequence() == null) {
            throw new ValidationException("La fréquence de la dépense récurrente est obligatoire.");
        }
        if (depenseRecurrente.getProchaineDate() == null) {
            throw new ValidationException("La prochaine date d'échéance est obligatoire.");
        }
        if (depenseRecurrente.getLibelle().length() > 200) {
            throw new ValidationException("Le libellé doit contenir au plus 200 caractères.");
        }
    }

    private void verifierLiens(DepenseRecurrente depenseRecurrente) {
        var compte = compteDao.trouverParId(depenseRecurrente.getCompteId())
                .orElseThrow(() -> new ValidationException("Le compte ciblé n'existe pas."));
        if (!compte.getUtilisateurId().equals(depenseRecurrente.getUtilisateurId())) {
            throw new ValidationException("Le compte choisi ne vous appartient pas.");
        }
        var categorie = categorieDao.trouverParId(depenseRecurrente.getCategorieId())
                .orElseThrow(() -> new ValidationException("La catégorie ciblée n'existe pas."));
        if (categorie.getUtilisateurId() != null
                && !categorie.getUtilisateurId().equals(depenseRecurrente.getUtilisateurId())) {
            throw new ValidationException("La catégorie choisie ne vous appartient pas.");
        }
    }
}
