package com.depenses.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DepenseRecurrente {
    private Long id;
    private Long utilisateurId;
    private Long compteId;
    private Long categorieId;
    private BigDecimal montant;
    private String libelle;
    private Frequence frequence;
    private LocalDate prochaineDate;
    private boolean active;

    public enum Frequence {
        HEBDO,
        MENSUEL
    }

    public DepenseRecurrente() {
    }

    public DepenseRecurrente(Long id, Long utilisateurId, Long compteId, Long categorieId, BigDecimal montant,
                            String libelle, Frequence frequence, LocalDate prochaineDate, boolean active) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.compteId = compteId;
        this.categorieId = categorieId;
        this.montant = montant;
        this.libelle = libelle;
        this.frequence = frequence;
        this.prochaineDate = prochaineDate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Long getCompteId() {
        return compteId;
    }

    public void setCompteId(Long compteId) {
        this.compteId = compteId;
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Frequence getFrequence() {
        return frequence;
    }

    public void setFrequence(Frequence frequence) {
        this.frequence = frequence;
    }

    public LocalDate getProchaineDate() {
        return prochaineDate;
    }

    public void setProchaineDate(LocalDate prochaineDate) {
        this.prochaineDate = prochaineDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
