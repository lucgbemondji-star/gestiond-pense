package com.depenses.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Budget {
    private Long id;
    private Long utilisateurId;
    private Long categorieId;
    private Periode periode;
    private BigDecimal plafondXof;
    private LocalDate dateDebut;

    public enum Periode {
        HEBDO,
        MENSUEL
    }

    public Budget() {
    }

    public Budget(Long id, Long utilisateurId, Long categorieId, Periode periode, BigDecimal plafondXof, LocalDate dateDebut) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.categorieId = categorieId;
        this.periode = periode;
        this.plafondXof = plafondXof;
        this.dateDebut = dateDebut;
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

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public BigDecimal getPlafondXof() {
        return plafondXof;
    }

    public void setPlafondXof(BigDecimal plafondXof) {
        this.plafondXof = plafondXof;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }
}
