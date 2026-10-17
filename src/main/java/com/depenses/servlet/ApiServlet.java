package com.depenses.servlet;

import com.depenses.util.JsonUtil;
import com.depenses.util.ApiException;
import com.depenses.util.ReponseApi;
import com.depenses.util.ValidationException;
import com.depenses.dao.DaoException;
import java.sql.SQLException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

public abstract class ApiServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServlet.class);

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, jakarta.servlet.ServletException {
        try {
            super.service(request, response);
        } catch (ApiException e) {
            reponseJson(response, false, null, e.getMessage(), e.getCodeHttp());
        } catch (IllegalArgumentException e) {
            reponseJson(response, false, null, "Une valeur fournie est invalide.", HttpServletResponse.SC_BAD_REQUEST);
        } catch (DaoException e) {
            if (estConflitDeDonnees(e)) {
                LOGGER.warn("Conflit de données pendant le traitement de la route API {}.", request.getRequestURI(), e);
                reponseJson(response, false, null, "Cette opération est impossible car la donnée est déjà utilisée.",
                        HttpServletResponse.SC_CONFLICT);
            } else {
                LOGGER.error("Erreur de base de données pendant le traitement de la route API {}.", request.getRequestURI(), e);
                reponseJson(response, false, null, "Une erreur de base de données est survenue.",
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Erreur interne pendant le traitement de la route API {}.", request.getRequestURI(), e);
            reponseJson(response, false, null, "Une erreur interne est survenue.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean estConflitDeDonnees(Throwable erreur) {
        Throwable cause = erreur;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                String etatSql = sqlException.getSQLState();
                return etatSql != null && etatSql.startsWith("23");
            }
            cause = cause.getCause();
        }
        return false;
    }

    protected void ecrireJson(HttpServletResponse response, Object donnees, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JsonUtil.toJson(donnees));
    }

    protected void reponseJson(HttpServletResponse response, boolean succes, Object donnees, String erreur, int statusCode) throws IOException {
        ecrireJson(response, ReponseApi.reponse(succes, donnees, erreur), statusCode);
    }

    protected String extrairePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexte = request.getContextPath();
        if (contexte != null && !contexte.isBlank() && uri.startsWith(contexte)) {
            return uri.substring(contexte.length());
        }
        return uri;
    }

    protected Map<String, Object> lireCorpsJson(HttpServletRequest request) throws IOException {
        String corps = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> payload = JsonUtil.fromJson(corps, Map.class);
        if (payload == null) {
            throw new ValidationException("Le corps JSON est vide ou invalide.");
        }
        return payload;
    }

    protected Long obtenirUtilisateurId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("utilisateurId") == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(session.getAttribute("utilisateurId")));
    }

    protected String valeurTexte(Object valeur) {
        return valeur == null ? null : String.valueOf(valeur).trim();
    }

    protected Long valeurLong(Object valeur) {
        if (valeur == null || String.valueOf(valeur).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valeur).trim()).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ValidationException("La valeur numérique doit être un entier valide.");
        }
    }

    protected BigDecimal valeurBigDecimal(Object valeur) {
        if (valeur == null || String.valueOf(valeur).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valeur));
        } catch (NumberFormatException e) {
            throw new ValidationException("Le montant est invalide.");
        }
    }

    protected LocalDate valeurDate(Object valeur) {
        if (valeur == null || String.valueOf(valeur).isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(valeur));
        } catch (Exception e) {
            throw new ValidationException("La date est invalide.");
        }
    }

    protected String extraireDernierSegment(String chemin) {
        if (chemin == null || chemin.isBlank()) {
            return "";
        }
        String nettoye = chemin;
        if (nettoye.endsWith("/")) {
            nettoye = nettoye.substring(0, nettoye.length() - 1);
        }
        int index = nettoye.lastIndexOf('/');
        return index >= 0 ? nettoye.substring(index + 1) : nettoye;
    }
}
