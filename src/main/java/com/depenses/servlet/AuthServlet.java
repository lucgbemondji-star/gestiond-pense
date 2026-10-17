package com.depenses.servlet;

import com.depenses.model.Utilisateur;
import com.depenses.service.AuthService;
import com.depenses.util.JsonUtil;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/auth/*"})
public class AuthServlet extends ApiServlet {
    private final AuthService authService;

    public AuthServlet() {
        this(new AuthService());
    }

    public AuthServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String chemin = extrairePath(request);

        if (chemin.endsWith("/auth/inscription")) {
            traiterInscription(request, response);
            return;
        }
        if (chemin.endsWith("/auth/connexion")) {
            traiterConnexion(request, response);
            return;
        }
        if (chemin.endsWith("/auth/deconnexion")) {
            deconnecter(request, response);
            return;
        }

        reponseJson(response, false, null, "Route non supportée.", 404);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String chemin = extrairePath(request);
        if (chemin.endsWith("/auth/moi")) {
            utilisateurCourant(request, response);
            return;
        }
        reponseJson(response, false, null, "Route non supportée.", 404);
    }

    private void traiterInscription(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = lireJson(request);
        String nom = asString(payload.get("nom"));
        String email = asString(payload.get("email"));
        String motDePasse = asString(payload.get("motDePasse"));

        Utilisateur utilisateur = authService.inscrire(nom, email, motDePasse);
        HttpSession session = request.getSession(true);
        session.setAttribute("utilisateurId", utilisateur.getId());
        session.setAttribute("role", utilisateur.getRole().name());
        session.setAttribute("utilisateurNom", utilisateur.getNom());
        String csrf = authService.genererJetonCsrf();
        session.setAttribute("csrfToken", csrf);
        ajouterCookieCsrf(request, response, csrf);

        reponseJson(response, true, utilisateurSansMotDePasse(utilisateur), null, 201);
    }

    private void traiterConnexion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = lireJson(request);
        String email = asString(payload.get("email"));
        String motDePasse = asString(payload.get("motDePasse"));

        Utilisateur utilisateur = authService.connecter(email, motDePasse);
        HttpSession session = request.getSession(true);
        session.setAttribute("utilisateurId", utilisateur.getId());
        session.setAttribute("role", utilisateur.getRole().name());
        session.setAttribute("utilisateurNom", utilisateur.getNom());
        String csrf = authService.genererJetonCsrf();
        session.setAttribute("csrfToken", csrf);
        ajouterCookieCsrf(request, response, csrf);

        reponseJson(response, true, utilisateurSansMotDePasse(utilisateur), null, 200);
    }

    private void deconnecter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Cookie csrfCookie = new Cookie("csrf_token", "");
        csrfCookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        csrfCookie.setMaxAge(0);
        csrfCookie.setHttpOnly(false);
        csrfCookie.setSecure(request.isSecure());
        response.addCookie(csrfCookie);
        reponseJson(response, true, null, null, 200);
    }

    private void utilisateurCourant(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utilisateurId") == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        Long utilisateurId = Long.parseLong(String.valueOf(session.getAttribute("utilisateurId")));
        Utilisateur utilisateur = authService.trouverParId(utilisateurId)
                .orElseThrow(() -> new ValidationException("Utilisateur introuvable."));

        reponseJson(response, true, utilisateurSansMotDePasse(utilisateur), null, 200);
    }

    private Map<String, Object> lireJson(HttpServletRequest request) throws IOException {
        String corps = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> payload = JsonUtil.fromJson(corps, Map.class);
        if (payload == null) {
            throw new ValidationException("Le corps JSON est vide ou invalide.");
        }
        return payload;
    }

    private String asString(Object valeur) {
        return valeur == null ? null : String.valueOf(valeur).trim();
    }

    private Utilisateur utilisateurSansMotDePasse(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        utilisateur.setMotDePasseHash(null);
        return utilisateur;
    }

    private void ajouterCookieCsrf(HttpServletRequest request, HttpServletResponse response, String jeton) {
        Cookie cookie = new Cookie("csrf_token", jeton);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setHttpOnly(false);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(60 * 60 * 2);
        response.addCookie(cookie);
    }
}
