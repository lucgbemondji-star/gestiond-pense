package com.depenses.dao;

import com.depenses.model.DepenseRecurrente;

import java.util.List;
import java.util.Optional;

public interface DepenseRecurrenteDao {
    long creer(DepenseRecurrente depenseRecurrente);

    Optional<DepenseRecurrente> trouverParId(Long id);

    List<DepenseRecurrente> listerParUtilisateur(Long utilisateurId);

    boolean modifier(DepenseRecurrente depenseRecurrente);

    boolean supprimer(Long id);
}
