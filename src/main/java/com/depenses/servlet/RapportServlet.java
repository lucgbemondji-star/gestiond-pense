package com.depenses.servlet;

import com.depenses.dao.JdbcDepenseDao;
import com.depenses.model.Depense;
import com.depenses.service.RapportService;
import com.depenses.util.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/api/rapports/*")
public class RapportServlet extends ApiServlet {
    private final RapportService rapportService = new RapportService();
    private final JdbcDepenseDao depenseDao = new JdbcDepenseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long utilisateurId = obtenirUtilisateurId(request);
        if (utilisateurId == null) {
            reponseJson(response, false, null, "Authentification requise.", 401);
            return;
        }

        String chemin = extrairePath(request);
        if (chemin.startsWith("/api/rapports/mensuel")) {
            String anneeParam = request.getParameter("annee");
            String moisParam = request.getParameter("mois");
            int annee;
            int mois;
            try {
                annee = anneeParam == null || anneeParam.isBlank() ? LocalDate.now().getYear() : Integer.parseInt(anneeParam);
                mois = moisParam == null || moisParam.isBlank() ? LocalDate.now().getMonthValue() : Integer.parseInt(moisParam);
            } catch (NumberFormatException e) {
                throw new ValidationException("L'année et le mois du rapport doivent être numériques.");
            }
            if (mois < 1 || mois > 12 || annee < 2000 || annee > 2100) {
                throw new ValidationException("La période du rapport est invalide.");
            }
            List<Depense> depenses = depenseDao.listerParUtilisateur(utilisateurId);
            Map<LocalDate, BigDecimal> rapport = rapportService.rapportMensuel(depenses, annee, mois);
            reponseJson(response, true, Map.of(
                    "annee", annee,
                    "mois", mois,
                    "donnees", rapport
            ), null, 200);
            return;
        }
        reponseJson(response, false, null, "Route non supportée.", 404);
    }
}
