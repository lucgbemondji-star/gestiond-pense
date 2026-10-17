package com.depenses.service;

import com.depenses.dao.CategorieDao;
import com.depenses.dao.CompteDao;
import com.depenses.dao.DepenseDao;
import com.depenses.dao.DepenseRecurrenteDao;
import com.depenses.model.Categorie;
import com.depenses.model.Compte;
import com.depenses.model.Depense;
import com.depenses.model.DepenseRecurrente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepenseRecurrenteServiceTest {
    @Test
    void genereToutesLesEcheancesHebdomadairesManqueesAvecLeurDate() {
        DepenseRecurrente recurrente = new DepenseRecurrente(10L, 1L, 2L, 3L,
                new BigDecimal("1500"), "Loyer", DepenseRecurrente.Frequence.HEBDO,
                LocalDate.now().minusDays(8), true);
        List<Depense> depensesGenerees = new ArrayList<>();
        DepenseRecurrenteDao recurrenteDao = new DepenseRecurrenteDao() {
            @Override
            public long creer(DepenseRecurrente depenseRecurrente) {
                return 11;
            }

            @Override
            public Optional<DepenseRecurrente> trouverParId(Long id) {
                return Optional.of(recurrente);
            }

            @Override
            public List<DepenseRecurrente> listerParUtilisateur(Long utilisateurId) {
                return List.of(recurrente);
            }

            @Override
            public boolean modifier(DepenseRecurrente depenseRecurrente) {
                return true;
            }

            @Override
            public boolean supprimer(Long id) {
                return true;
            }
        };
        DepenseDao depenseDao = new DepenseDao() {
            @Override
            public long creer(Depense depense) {
                depensesGenerees.add(depense);
                return depensesGenerees.size();
            }

            @Override
            public Optional<Depense> trouverParId(Long id) {
                return Optional.empty();
            }

            @Override
            public List<Depense> listerParUtilisateur(Long utilisateurId) {
                return depensesGenerees;
            }

            @Override
            public boolean modifier(Depense depense) {
                return false;
            }

            @Override
            public boolean supprimer(Long id) {
                return false;
            }
        };
        CompteDao compteDao = new CompteDao() {
            @Override
            public long creer(Compte compte) {
                return 2;
            }

            @Override
            public Optional<Compte> trouverParId(Long id) {
                return Optional.empty();
            }

            @Override
            public List<Compte> listerParUtilisateur(Long utilisateurId) {
                return List.of();
            }

            @Override
            public boolean modifier(Compte compte) {
                return false;
            }

            @Override
            public boolean supprimer(Long id) {
                return false;
            }
        };
        CategorieDao categorieDao = new CategorieDao() {
            @Override
            public long creer(Categorie categorie) {
                return 3;
            }

            @Override
            public Optional<Categorie> trouverParId(Long id) {
                return Optional.empty();
            }

            @Override
            public List<Categorie> listerParUtilisateur(Long utilisateurId) {
                return List.of();
            }

            @Override
            public List<Categorie> listerParDefaut() {
                return List.of();
            }

            @Override
            public boolean modifier(Categorie categorie) {
                return false;
            }

            @Override
            public boolean supprimer(Long id) {
                return false;
            }
        };

        DepenseRecurrenteService service = new DepenseRecurrenteService(recurrenteDao, depenseDao, compteDao, categorieDao);
        service.traiterDepensesEchues();

        assertEquals(2, depensesGenerees.size());
        assertEquals(LocalDate.now().minusDays(8), depensesGenerees.get(0).getDateDepense());
        assertEquals(LocalDate.now().minusDays(1), depensesGenerees.get(1).getDateDepense());
        assertEquals(LocalDate.now().plusDays(6), recurrente.getProchaineDate());
    }
}
