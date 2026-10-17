package com.depenses.servlet;

import com.depenses.dao.JdbcUtilisateurDao;
import com.depenses.model.Utilisateur;
import com.depenses.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/api/admin/*")
public class AdminUtilisateurServlet extends ApiServlet {
    private final JdbcUtilisateurDao utilisateurDao = new JdbcUtilisateurDao();
    private final AuthService authService = new AuthService(utilisateurDao);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        Object role = request.getSession(false).getAttribute("role");
        if (role == null || !"ADMIN".equals(String.valueOf(role))) {
            reponseJson(response, false, null, "Accès refusé pour ce rôle.", 403);
            return;
        }

        List<Utilisateur> utilisateurs = utilisateurDao.lister();
        reponseJson(response, true, utilisateurs.stream().map(this::sansMotDePasse).toList(), null, 200);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }
        Object role = request.getSession(false).getAttribute("role");
        if (role == null || !"ADMIN".equals(String.valueOf(role))) {
            reponseJson(response, false, null, "Accès refusé pour ce rôle.", 403);
            return;
        }

        String chemin = extrairePath(request);
        if (!chemin.endsWith("/api/admin/utilisateurs")) {
            reponseJson(response, false, null, "Route non supportée.", 404);
            return;
        }

        Map<String, Object> payload = lireCorpsJson(request);
        Utilisateur utilisateur = authService.inscrire(
                valeurTexte(payload.get("nom")),
                valeurTexte(payload.get("email")),
                valeurTexte(payload.get("motDePasse"))
        );
        utilisateur.setRole(Utilisateur.RoleUtilisateur.valueOf(String.valueOf(payload.getOrDefault("role", "USER"))));
        utilisateurDao.modifier(utilisateur);
        reponseJson(response, true, sansMotDePasse(utilisateur), null, 201);
    }

    private Utilisateur sansMotDePasse(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        utilisateur.setMotDePasseHash(null);
        return utilisateur;
    }
}
