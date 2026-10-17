package com.depenses.service;

import com.depenses.client.TauxChangeClient;
import com.depenses.dao.CompteDao;
import com.depenses.dao.DepenseDao;
import com.depenses.model.Compte;
import com.depenses.model.Depense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompteServiceTest {
    @Test
    void calculeLeSoldeEnXofPourUnCompteEnDeviseEtrangere() {
        Compte compte = new Compte();
        compte.setId(4L);
        compte.setUtilisateurId(9L);
        compte.setDevise("USD");
        compte.setSoldeInitial(new BigDecimal("10.00"));

        Depense depense = new Depense();
        depense.setCompteId(4L);
        depense.setMontantXof(new BigDecimal("650.00"));

        CompteDao compteDao = new CompteDao() {
            @Override
            public long creer(Compte valeur) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Compte> trouverParId(Long id) {
                return Optional.of(compte);
            }

            @Override
            public List<Compte> listerParUtilisateur(Long utilisateurId) {
                return List.of(compte);
            }

            @Override
            public boolean modifier(Compte valeur) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean supprimer(Long id) {
                throw new UnsupportedOperationException();
            }
        };
        DepenseDao depenseDao = new DepenseDao() {
            @Override
            public long creer(Depense valeur) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Depense> trouverParId(Long id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Depense> listerParUtilisateur(Long utilisateurId) {
                return List.of(depense);
            }

            @Override
            public boolean modifier(Depense valeur) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean supprimer(Long id) {
                throw new UnsupportedOperationException();
            }
        };
        TauxChangeClient tauxClient = deviseBase -> Map.of("USD", new BigDecimal("650.00"));
        CompteService service = new CompteService(compteDao, depenseDao, new TauxChangeService(tauxClient));

        assertEquals(new BigDecimal("5850.00"), service.calculerSoldeCourant(4L));
    }
}
