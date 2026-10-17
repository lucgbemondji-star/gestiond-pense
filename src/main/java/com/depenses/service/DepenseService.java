package com.depenses.service;

import com.depenses.dao.CompteDao;
import com.depenses.dao.CategorieDao;
import com.depenses.dao.DepenseDao;
import com.depenses.model.Categorie;
import com.depenses.model.Compte;
import com.depenses.model.Depense;
import com.depenses.util.ValidationException;

import java.util.List;

public class DepenseService {
    private final DepenseDao depenseDao;
    private final CompteDao compteDao;
    private final TauxChangeService tauxChangeService;
    private final CategorieDao categorieDao;

    public DepenseService(DepenseDao depenseDao, CompteDao compteDao, TauxChangeService tauxChangeService) {
        this(depenseDao, compteDao, null, tauxChangeService);
    }

    public DepenseService(DepenseDao depenseDao, CompteDao compteDao, CategorieDao categorieDao,
                          TauxChangeService tauxChangeService) {
        this.depenseDao = depenseDao;
        this.compteDao = compteDao;
        this.categorieDao = categorieDao;
        this.tauxChangeService = tauxChangeService;
    }

    public Depense creer(Depense depense) {
        preparer(depense);
        long id = depenseDao.creer(depense);
        depense.setId(id);
        return depense;
    }

    public List<Depense> creerPlusieurs(List<Depense> depenses) {
        if (depenses == null || depenses.isEmpty()) {
            throw new ValidationException("Aucune dépense à créer.");
        }
        for (Depense depense : depenses) {
            preparer(depense);
        }
        return depenseDao.creerPlusieurs(depenses);
    }

    private void preparer(Depense depense) {
        valider(depense);
        Compte compte = compteDao.trouverParId(depense.getCompteId())
                .orElseThrow(() -> new ValidationException("Le compte ciblé n'existe pas."));
        if (!compte.getUtilisateurId().equals(depense.getUtilisateurId())) {
            throw new ValidationException("La dépense ne correspond pas au propriétaire du compte.");
        }
        verifierCategorie(depense.getCategorieId(), depense.getUtilisateurId());

        depense.setMontantXof(tauxChangeService.convertirEnXof(depense.getDevise(), depense.getMontant()));
    }

    public List<Depense> listerParUtilisateur(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new ValidationException("L'identifiant utilisateur est invalide.");
        }
        return depenseDao.listerParUtilisateur(utilisateurId);
    }

    public Depense modifier(Depense depense) {
        valider(depense);
        Depense existante = depenseDao.trouverParId(depense.getId())
                .orElseThrow(() -> new ValidationException("La dépense à modifier est introuvable."));
        if (!existante.getUtilisateurId().equals(depense.getUtilisateurId())) {
            throw new ValidationException("Vous n'êtes pas autorisé à modifier cette dépense.");
        }
        Compte compte = compteDao.trouverParId(depense.getCompteId())
                .orElseThrow(() -> new ValidationException("Le compte ciblé n'existe pas."));
        if (!compte.getUtilisateurId().equals(depense.getUtilisateurId())) {
            throw new ValidationException("Le compte choisi ne vous appartient pas.");
        }
        verifierCategorie(depense.getCategorieId(), depense.getUtilisateurId());
        depense.setMontantXof(tauxChangeService.convertirEnXof(depense.getDevise(), depense.getMontant()));
        if (!depenseDao.modifier(depense)) {
            throw new ValidationException("La dépense à modifier est introuvable.");
        }
        return depense;
    }

    public void supprimer(Long depenseId, Long utilisateurId) {
        if (depenseId == null || depenseId <= 0) {
            throw new ValidationException("L'identifiant de la dépense est invalide.");
        }
        Depense depense = depenseDao.trouverParId(depenseId)
                .orElseThrow(() -> new ValidationException("La dépense est introuvable."));
        if (!depense.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à supprimer cette dépense.");
        }
        if (!depenseDao.supprimer(depenseId)) {
            throw new ValidationException("La suppression de la dépense a échoué.");
        }
    }

    private void valider(Depense depense) {
        if (depense == null) {
            throw new ValidationException("La dépense ne peut pas être vide.");
        }
        if (depense.getCompteId() == null || depense.getCompteId() <= 0) {
            throw new ValidationException("Le compte est obligatoire.");
        }
        if (depense.getCategorieId() == null || depense.getCategorieId() <= 0) {
            throw new ValidationException("La catégorie est obligatoire.");
        }
        if (depense.getMontant() == null || depense.getMontant().signum() <= 0) {
            throw new ValidationException("Le montant doit être strictement positif.");
        }
        if (depense.getDateDepense() == null) {
            throw new ValidationException("La date de dépense est obligatoire.");
        }
        if (depense.getLibelle() == null || depense.getLibelle().isBlank()) {
            throw new ValidationException("Le libellé est obligatoire.");
        }
        if (depense.getLibelle().length() > 200 || (depense.getNote() != null && depense.getNote().length() > 500)) {
            throw new ValidationException("Le libellé doit contenir au plus 200 caractères et la note au plus 500 caractères.");
        }
        if (depense.getDevise() == null || depense.getDevise().isBlank()) {
            throw new ValidationException("La devise est obligatoire.");
        }
    }

    private void verifierCategorie(Long categorieId, Long utilisateurId) {
        if (categorieDao == null) return;
        Categorie categorie = categorieDao.trouverParId(categorieId)
                .orElseThrow(() -> new ValidationException("La catégorie ciblée n'existe pas."));
        if (categorie.getUtilisateurId() != null && !categorie.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("La catégorie choisie ne vous appartient pas.");
        }
    }
}
