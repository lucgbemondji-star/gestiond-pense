package com.depenses.service;

import com.depenses.dao.JdbcUtilisateurDao;
import com.depenses.dao.UtilisateurDao;
import com.depenses.model.Utilisateur;
import com.depenses.util.ApiException;
import com.depenses.util.ValidationException;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private final UtilisateurDao utilisateurDao;

    public AuthService() {
        this(new JdbcUtilisateurDao());
    }

    public AuthService(UtilisateurDao utilisateurDao) {
        this.utilisateurDao = utilisateurDao;
    }

    public Utilisateur inscrire(String nom, String email, String motDePasse) {
        String nomValide = normaliserNom(nom);
        String emailValide = normaliserEmail(email);
        String motDePasseValide = validerMotDePasse(motDePasse);

        if (utilisateurDao.trouverParEmail(emailValide).isPresent()) {
            throw new ValidationException("Un compte existe déjà pour cette adresse e-mail.");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nomValide);
        utilisateur.setEmail(emailValide);
        utilisateur.setMotDePasseHash(BCrypt.hashpw(motDePasseValide, BCrypt.gensalt(12)));
        utilisateur.setRole(Utilisateur.RoleUtilisateur.USER);

        long id = utilisateurDao.creer(utilisateur);
        utilisateur.setId(id);
        return utilisateur;
    }

    public Utilisateur connecter(String email, String motDePasse) {
        String emailValide = normaliserEmail(email);
        String motDePasseValide = validerMotDePasse(motDePasse);

        Optional<Utilisateur> utilisateurOptional = utilisateurDao.trouverParEmail(emailValide);
        if (utilisateurOptional.isEmpty()) {
            throw new ApiException(401, "Identifiants invalides.");
        }

        Utilisateur utilisateur = utilisateurOptional.get();
        if (!BCrypt.checkpw(motDePasseValide, utilisateur.getMotDePasseHash())) {
            throw new ApiException(401, "Identifiants invalides.");
        }

        return utilisateur;
    }

    public Optional<Utilisateur> trouverParId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return utilisateurDao.trouverParId(id);
    }

    public String genererJetonCsrf() {
        return UUID.randomUUID().toString();
    }

    private String normaliserNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new ValidationException("Le nom est obligatoire.");
        }
        String nomValide = nom.trim();
        if (nomValide.length() < 2 || nomValide.length() > 120) {
            throw new ValidationException("Le nom doit contenir entre 2 et 120 caractères.");
        }
        return nomValide;
    }

    private String normaliserEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("L'adresse e-mail est obligatoire.");
        }
        String emailValide = email.trim().toLowerCase(Locale.FRANCE);
        if (!emailValide.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("L'adresse e-mail n'est pas valide.");
        }
        if (emailValide.length() > 180) {
            throw new ValidationException("L'adresse e-mail est trop longue.");
        }
        return emailValide;
    }

    private String validerMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new ValidationException("Le mot de passe est obligatoire.");
        }
        if (motDePasse.length() < 8 || motDePasse.length() > 128) {
            throw new ValidationException("Le mot de passe doit contenir entre 8 et 128 caractères.");
        }
        return motDePasse;
    }
}
