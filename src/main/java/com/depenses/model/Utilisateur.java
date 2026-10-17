package com.depenses.model;

import java.time.LocalDateTime;

public class Utilisateur {
    private Long id;
    private String nom;
    private String email;
    private String motDePasseHash;
    private RoleUtilisateur role = RoleUtilisateur.USER;
    private LocalDateTime dateCreation;

    public enum RoleUtilisateur {
        USER,
        ADMIN
    }

    public Utilisateur() {
    }

    public Utilisateur(Long id, String nom, String email, String motDePasseHash, RoleUtilisateur role, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.dateCreation = dateCreation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    public void setRole(RoleUtilisateur role) {
        this.role = role;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
