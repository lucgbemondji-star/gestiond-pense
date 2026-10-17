package com.depenses.dao;

import com.depenses.model.Utilisateur;

import java.util.List;
import java.util.Optional;

public interface UtilisateurDao {
    long creer(Utilisateur utilisateur);

    Optional<Utilisateur> trouverParId(Long id);

    Optional<Utilisateur> trouverParEmail(String email);

    List<Utilisateur> lister();

    boolean modifier(Utilisateur utilisateur);

    boolean supprimer(Long id);
}
