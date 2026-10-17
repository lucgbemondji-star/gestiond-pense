package com.depenses.service;

import com.depenses.dao.CompteDao;
import com.depenses.dao.DepenseDao;
import com.depenses.client.HttpTauxChangeClient;
import com.depenses.model.Compte;
import com.depenses.model.Depense;
import com.depenses.util.ValidationException;

import java.math.BigDecimal;
import java.util.List;

public class CompteService {
    private final CompteDao compteDao;
    private final DepenseDao depenseDao;
    private final TauxChangeService tauxChangeService;

    public CompteService(CompteDao compteDao, DepenseDao depenseDao) {
        this(compteDao, depenseDao, new TauxChangeService(new HttpTauxChangeClient()));
    }

    public CompteService(CompteDao compteDao, DepenseDao depenseDao, TauxChangeService tauxChangeService) {
        this.compteDao = compteDao;
        this.depenseDao = depenseDao;
        this.tauxChangeService = tauxChangeService;
    }

    public Compte creer(Compte compte) {
        valider(compte);
        long id = compteDao.creer(compte);
        compte.setId(id);
        return compte;
    }

    public List<Compte> listerParUtilisateur(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new ValidationException("L'identifiant utilisateur est invalide.");
        }
        return compteDao.listerParUtilisateur(utilisateurId);
    }

    public Compte modifier(Compte compte, Long utilisateurId) {
        valider(compte);
        Compte existant = compteDao.trouverParId(compte.getId())
                .orElseThrow(() -> new ValidationException("Le compte est introuvable."));
        if (!existant.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à modifier ce compte.");
        }
        if (!compteDao.modifier(compte)) {
            throw new ValidationException("La modification du compte a échoué.");
        }
        return compte;
    }

    public BigDecimal calculerSoldeCourant(Long compteId) {
        if (compteId == null || compteId <= 0) {
            throw new ValidationException("L'identifiant du compte est invalide.");
        }

        Compte compte = compteDao.trouverParId(compteId)
                .orElseThrow(() -> new ValidationException("Le compte est introuvable."));

        BigDecimal totalDepenses = BigDecimal.ZERO;
        for (Depense depense : depenseDao.listerParUtilisateur(compte.getUtilisateurId())) {
            if (depense.getCompteId() != null && depense.getCompteId().equals(compteId) && depense.getMontantXof() != null) {
                totalDepenses = totalDepenses.add(depense.getMontantXof());
            }
        }

        BigDecimal soldeInitial = compte.getSoldeInitial() == null ? BigDecimal.ZERO : compte.getSoldeInitial();
        if (!"XOF".equalsIgnoreCase(compte.getDevise()) && soldeInitial.signum() != 0) {
            soldeInitial = tauxChangeService.convertirEnXof(compte.getDevise(), soldeInitial);
        }
        return soldeInitial.subtract(totalDepenses);
    }

    public void supprimer(Long compteId, Long utilisateurId) {
        if (compteId == null || compteId <= 0) {
            throw new ValidationException("L'identifiant du compte est invalide.");
        }

        Compte compte = compteDao.trouverParId(compteId)
                .orElseThrow(() -> new ValidationException("Le compte est introuvable."));
        if (!compte.getUtilisateurId().equals(utilisateurId)) {
            throw new ValidationException("Vous n'êtes pas autorisé à supprimer ce compte.");
        }
        if (!compteDao.supprimer(compteId)) {
            throw new ValidationException("La suppression du compte a échoué.");
        }
    }

    private void valider(Compte compte) {
        if (compte == null) {
            throw new ValidationException("Le compte ne peut pas être vide.");
        }
        if (compte.getUtilisateurId() == null || compte.getUtilisateurId() <= 0) {
            throw new ValidationException("Le propriétaire du compte est obligatoire.");
        }
        if (compte.getNom() == null || compte.getNom().isBlank()) {
            throw new ValidationException("Le nom du compte est obligatoire.");
        }
        if (compte.getType() == null) {
            throw new ValidationException("Le type de compte est obligatoire.");
        }
        if (compte.getDevise() == null || compte.getDevise().isBlank()) {
            throw new ValidationException("La devise du compte est obligatoire.");
        }
    }
}
