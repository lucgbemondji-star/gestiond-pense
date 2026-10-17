package com.depenses.model;

import java.math.BigDecimal;

public class Compte {
    private Long id;
    private Long utilisateurId;
    private String nom;
    private TypeCompte type;
    private String devise;
    private BigDecimal soldeInitial;
    private BigDecimal soldeCourant;

    public enum TypeCompte {
        ESPECES,
        MOBILE_MONEY,
        BANQUE
    }

    public Compte() {
    }

    public Compte(Long id, Long utilisateurId, String nom, TypeCompte type, String devise, BigDecimal soldeInitial) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.nom = nom;
        this.type = type;
        this.devise = devise;
        this.soldeInitial = soldeInitial;
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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypeCompte getType() {
        return type;
    }

    public void setType(TypeCompte type) {
        this.type = type;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public BigDecimal getSoldeInitial() {
        return soldeInitial;
    }

    public void setSoldeInitial(BigDecimal soldeInitial) {
        this.soldeInitial = soldeInitial;
    }

    public BigDecimal getSoldeCourant() {
        return soldeCourant;
    }

    public void setSoldeCourant(BigDecimal soldeCourant) {
        this.soldeCourant = soldeCourant;
    }
}
