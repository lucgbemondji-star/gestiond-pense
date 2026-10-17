package com.depenses.model;

public class RegleCategorie {
    private Long id;
    private Long utilisateurId;
    private String motCle;
    private Long categorieId;
    private Integer poids;

    public RegleCategorie() {
    }

    public RegleCategorie(Long id, Long utilisateurId, String motCle, Long categorieId, Integer poids) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.motCle = motCle;
        this.categorieId = categorieId;
        this.poids = poids;
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

    public String getMotCle() {
        return motCle;
    }

    public void setMotCle(String motCle) {
        this.motCle = motCle;
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }

    public Integer getPoids() {
        return poids;
    }

    public void setPoids(Integer poids) {
        this.poids = poids;
    }
}
