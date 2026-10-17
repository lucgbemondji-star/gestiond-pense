package com.depenses.dao;

import com.depenses.model.Categorie;

import java.util.List;
import java.util.Optional;

public interface CategorieDao {
    long creer(Categorie categorie);

    Optional<Categorie> trouverParId(Long id);

    List<Categorie> listerParUtilisateur(Long utilisateurId);

    List<Categorie> listerParDefaut();

    boolean modifier(Categorie categorie);

    boolean supprimer(Long id);
}
