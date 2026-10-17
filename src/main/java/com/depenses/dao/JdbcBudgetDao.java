package com.depenses.dao;

import com.depenses.model.Budget;
import com.depenses.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBudgetDao implements BudgetDao {
    private static final String INSERT_SQL = "INSERT INTO budget (utilisateur_id, categorie_id, periode, plafond_xof, date_debut) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, categorie_id, periode, plafond_xof, date_debut FROM budget WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, categorie_id, periode, plafond_xof, date_debut FROM budget WHERE utilisateur_id = ? ORDER BY date_debut DESC";
    private static final String UPDATE_SQL = "UPDATE budget SET categorie_id = ?, periode = ?, plafond_xof = ?, date_debut = ? WHERE id = ? AND utilisateur_id = ?";
    private static final String DELETE_SQL = "DELETE FROM budget WHERE id = ?";

    @Override
    public long creer(Budget budget) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, budget.getUtilisateurId());
            statement.setLong(2, budget.getCategorieId());
            statement.setString(3, budget.getPeriode().name());
            statement.setBigDecimal(4, budget.getPlafondXof() == null ? BigDecimal.ZERO : budget.getPlafondXof());
            statement.setDate(5, Date.valueOf(budget.getDateDebut()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer le budget.", e);
        }
    }

    @Override
    public Optional<Budget> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver le budget par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> listerParUtilisateur(Long utilisateurId) {
        List<Budget> budgets = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_UTILISATEUR)) {
            statement.setLong(1, utilisateurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    budgets.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les budgets.", e);
        }
        return budgets;
    }

    @Override
    public boolean modifier(Budget budget) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, budget.getCategorieId());
            statement.setString(2, budget.getPeriode().name());
            statement.setBigDecimal(3, budget.getPlafondXof() == null ? BigDecimal.ZERO : budget.getPlafondXof());
            statement.setDate(4, Date.valueOf(budget.getDateDebut()));
            statement.setLong(5, budget.getId());
            statement.setLong(6, budget.getUtilisateurId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier le budget.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer le budget.", e);
        }
    }

    private Budget mapper(ResultSet resultSet) throws SQLException {
        Budget budget = new Budget();
        budget.setId(resultSet.getLong("id"));
        budget.setUtilisateurId(resultSet.getLong("utilisateur_id"));
        budget.setCategorieId(resultSet.getLong("categorie_id"));
        budget.setPeriode(Budget.Periode.valueOf(resultSet.getString("periode")));
        budget.setPlafondXof(resultSet.getBigDecimal("plafond_xof"));
        LocalDate dateDebut = resultSet.getDate("date_debut") != null ? resultSet.getDate("date_debut").toLocalDate() : null;
        budget.setDateDebut(dateDebut);
        return budget;
    }
}
