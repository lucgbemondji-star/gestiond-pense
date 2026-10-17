package com.depenses.dao;

import com.depenses.model.Budget;

import java.util.List;
import java.util.Optional;

public interface BudgetDao {
    long creer(Budget budget);

    Optional<Budget> trouverParId(Long id);

    List<Budget> listerParUtilisateur(Long utilisateurId);

    boolean modifier(Budget budget);

    boolean supprimer(Long id);
}
