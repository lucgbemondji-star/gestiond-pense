package com.depenses.filter;

import com.depenses.util.JsonUtil;
import com.depenses.util.ReponseApi;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = "/api/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        // Le filtre est activé par le conteneur de servlet.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String chemin = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (estRessourcePublique(chemin)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("utilisateurId") == null) {
            repondreErreur(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise.");
            return;
        }

        if (!"GET".equalsIgnoreCase(httpRequest.getMethod()) && !"OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            Object jetonSession = session.getAttribute("csrfToken");
            String jetonRecu = httpRequest.getHeader("X-CSRF-Token");
            if (jetonSession == null || jetonRecu == null || !jetonSession.toString().equals(jetonRecu)) {
                repondreErreur(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide ou absent.");
                return;
            }
        }

        if (chemin.contains("/api/admin/")) {
            Object role = session.getAttribute("role");
            if (role == null || !"ADMIN".equals(String.valueOf(role))) {
                repondreErreur(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Accès refusé pour ce rôle.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean estRessourcePublique(String uri) {
        return uri == null || uri.endsWith("/index.html") || uri.endsWith("/login.html") || uri.endsWith("/register.html")
                || uri.contains("/api/auth/connexion") || uri.contains("/api/auth/inscription")
                || uri.contains("/api/auth/moi")
                || uri.startsWith("/assets/") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/images/") || uri.endsWith(".css") || uri.endsWith(".js")
                || uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".ico");
    }

    private void repondreErreur(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(JsonUtil.toJson(ReponseApi.erreur(message)));
    }

    @Override
    public void destroy() {
        // Rien à libérer explicitement.
    }
}
