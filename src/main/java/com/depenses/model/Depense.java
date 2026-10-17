package com.depenses.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Depense {
    private Long id;
    private Long utilisateurId;
    private Long compteId;
    private Long categorieId;
    private BigDecimal montant;
    private String devise;
    private BigDecimal montantXof;
    private LocalDate dateDepense;
    private String libelle;
    private String note;
    private Long recurrenteId;
    private LocalDateTime dateCreation;

    public Depense() {
    }

    public Depense(Long id, Long utilisateurId, Long compteId, Long categorieId, BigDecimal montant, String devise,
                   BigDecimal montantXof, LocalDate dateDepense, String libelle, String note, Long recurrenteId,
                   LocalDateTime dateCreation) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.compteId = compteId;
        this.categorieId = categorieId;
        this.montant = montant;
        this.devise = devise;
        this.montantXof = montantXof;
        this.dateDepense = dateDepense;
        this.libelle = libelle;
        this.note = note;
        this.recurrenteId = recurrenteId;
        this.dateCreation = dateCreation;
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

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public BigDecimal getMontantXof() {
        return montantXof;
    }

    public void setMontantXof(BigDecimal montantXof) {
        this.montantXof = montantXof;
    }

    public LocalDate getDateDepense() {
        return dateDepense;
    }

    public void setDateDepense(LocalDate dateDepense) {
        this.dateDepense = dateDepense;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getRecurrenteId() {
        return recurrenteId;
    }

    public void setRecurrenteId(Long recurrenteId) {
        this.recurrenteId = recurrenteId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
