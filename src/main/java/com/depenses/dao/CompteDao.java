package com.depenses.dao;

import com.depenses.model.Compte;

import java.util.List;
import java.util.Optional;

public interface CompteDao {
    long creer(Compte compte);

    Optional<Compte> trouverParId(Long id);

    List<Compte> listerParUtilisateur(Long utilisateurId);

    boolean modifier(Compte compte);

    boolean supprimer(Long id);
}
