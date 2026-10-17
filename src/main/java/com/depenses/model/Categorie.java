package com.depenses.model;

public class Categorie {
    private Long id;
    private Long utilisateurId;
    private String nom;
    private Long parentId;

    public Categorie() {
    }

    public Categorie(Long id, Long utilisateurId, String nom, Long parentId) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.nom = nom;
        this.parentId = parentId;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
