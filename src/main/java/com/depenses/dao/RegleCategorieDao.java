package com.depenses.dao;

import com.depenses.model.RegleCategorie;

import java.util.List;
import java.util.Optional;

public interface RegleCategorieDao {
    long creer(RegleCategorie regleCategorie);

    Optional<RegleCategorie> trouverParId(Long id);

    List<RegleCategorie> listerParUtilisateur(Long utilisateurId);

    boolean modifier(RegleCategorie regleCategorie);

    boolean supprimer(Long id);
}
