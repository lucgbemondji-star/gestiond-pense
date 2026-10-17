package com.depenses.service;

import com.depenses.dao.CategorieDao;
import com.depenses.model.Categorie;
import com.depenses.util.ValidationException;

import java.util.List;

public class CategorieService {
    private final CategorieDao categorieDao;

    public CategorieService(CategorieDao categorieDao) {
        this.categorieDao = categorieDao;
    }

    public Categorie creer(Categorie categorie) {
        valider(categorie);
        long id = categorieDao.creer(categorie);
        categorie.setId(id);
        return categorie;
    }

    public List<Categorie> listerParUtilisateur(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new ValidationException("L'identifiant utilisateur est invalide.");
        }
        return categorieDao.listerParUtilisateur(utilisateurId);
    }

    public List<Categorie> listerParDefaut() {
        return categorieDao.listerParDefaut();
    }

    public Categorie modifier(Categorie categorie, Long utilisateurId) {
        valider(categorie);
        Categorie existante = categorieDao.trouverParId(categorie.getId())
                .orElseThrow(() -> new ValidationException("La catégorie est introuvable."));
        if (existante.getUtilisateurId() == null || !existante.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à modifier cette catégorie.");
        }
        categorie.setUtilisateurId(utilisateurId);
        validerParent(categorie.getParentId(), utilisateurId);
        if (!categorieDao.modifier(categorie)) {
            throw new ValidationException("La modification de la catégorie a échoué.");
        }
        return categorie;
    }

    public void supprimer(Long categorieId, Long utilisateurId) {
        if (categorieId == null || categorieId <= 0) {
            throw new ValidationException("L'identifiant de la catégorie est invalide.");
        }
        Categorie categorie = categorieDao.trouverParId(categorieId)
                .orElseThrow(() -> new ValidationException("La catégorie est introuvable."));
        if (categorie.getUtilisateurId() == null || !categorie.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à supprimer cette catégorie.");
        }
        if (!categorieDao.supprimer(categorieId)) {
            throw new ValidationException("La suppression de la catégorie a échoué.");
        }
    }

    private void valider(Categorie categorie) {
        if (categorie == null) {
            throw new ValidationException("La catégorie ne peut pas être vide.");
        }
        if (categorie.getNom() == null || categorie.getNom().isBlank()) {
            throw new ValidationException("Le nom de la catégorie est obligatoire.");
        }
        if (categorie.getNom().length() > 120) {
            throw new ValidationException("Le nom de la catégorie ne doit pas dépasser 120 caractères.");
        }
    }

    public void validerParent(Long parentId, Long utilisateurId) {
        if (parentId == null) return;
        Categorie parent = categorieDao.trouverParId(parentId)
                .orElseThrow(() -> new ValidationException("La catégorie parente est introuvable."));
        if (parent.getUtilisateurId() != null && !parent.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("La catégorie parente ne vous appartient pas.");
        }
    }
}
