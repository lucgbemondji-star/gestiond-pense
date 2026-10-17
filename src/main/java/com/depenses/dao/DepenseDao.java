package com.depenses.dao;

import com.depenses.model.Depense;

import java.util.List;
import java.util.Optional;

public interface DepenseDao {
    long creer(Depense depense);

    default List<Depense> creerPlusieurs(List<Depense> depenses) {
        for (Depense depense : depenses) {
            depense.setId(creer(depense));
        }
        return depenses;
    }

    Optional<Depense> trouverParId(Long id);

    List<Depense> listerParUtilisateur(Long utilisateurId);

    boolean modifier(Depense depense);

    boolean supprimer(Long id);
}
