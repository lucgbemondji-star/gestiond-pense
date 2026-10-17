package com.depenses.dao;

import com.depenses.model.DepenseRecurrente;
import com.depenses.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDepenseRecurrenteDao implements DepenseRecurrenteDao {
    private static final String INSERT_SQL = "INSERT INTO depense_recurrente (utilisateur_id, compte_id, categorie_id, montant, libelle, frequence, prochaine_date, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, compte_id, categorie_id, montant, libelle, frequence, prochaine_date, active FROM depense_recurrente WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, compte_id, categorie_id, montant, libelle, frequence, prochaine_date, active FROM depense_recurrente WHERE utilisateur_id = ? ORDER BY prochaine_date";
    private static final String SELECT_ALL = "SELECT id, utilisateur_id, compte_id, categorie_id, montant, libelle, frequence, prochaine_date, active FROM depense_recurrente WHERE active = TRUE ORDER BY prochaine_date";
    private static final String UPDATE_SQL = "UPDATE depense_recurrente SET compte_id = ?, categorie_id = ?, montant = ?, libelle = ?, frequence = ?, prochaine_date = ?, active = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM depense_recurrente WHERE id = ?";

    @Override
    public long creer(DepenseRecurrente depenseRecurrente) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, depenseRecurrente.getUtilisateurId());
            statement.setLong(2, depenseRecurrente.getCompteId());
            statement.setLong(3, depenseRecurrente.getCategorieId());
            statement.setBigDecimal(4, depenseRecurrente.getMontant());
            statement.setString(5, depenseRecurrente.getLibelle());
            statement.setString(6, depenseRecurrente.getFrequence().name());
            statement.setDate(7, Date.valueOf(depenseRecurrente.getProchaineDate()));
            statement.setBoolean(8, depenseRecurrente.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer la dépense récurrente.", e);
        }
    }

    @Override
    public Optional<DepenseRecurrente> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver la dépense récurrente par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<DepenseRecurrente> listerParUtilisateur(Long utilisateurId) {
        List<DepenseRecurrente> depenses = new ArrayList<>();
        String sql = utilisateurId == null ? SELECT_ALL : SELECT_BY_UTILISATEUR;
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(sql)) {
            if (utilisateurId != null) {
                statement.setLong(1, utilisateurId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    depenses.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les dépenses récurrentes.", e);
        }
        return depenses;
    }

    @Override
    public boolean modifier(DepenseRecurrente depenseRecurrente) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, depenseRecurrente.getCompteId());
            statement.setLong(2, depenseRecurrente.getCategorieId());
            statement.setBigDecimal(3, depenseRecurrente.getMontant() == null ? BigDecimal.ZERO : depenseRecurrente.getMontant());
            statement.setString(4, depenseRecurrente.getLibelle());
            statement.setString(5, depenseRecurrente.getFrequence().name());
            statement.setDate(6, Date.valueOf(depenseRecurrente.getProchaineDate()));
            statement.setBoolean(7, depenseRecurrente.isActive());
            statement.setLong(8, depenseRecurrente.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la dépense récurrente.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la dépense récurrente.", e);
        }
    }

    private DepenseRecurrente mapper(ResultSet resultSet) throws SQLException {
        DepenseRecurrente depense = new DepenseRecurrente();
        depense.setId(resultSet.getLong("id"));
        depense.setUtilisateurId(resultSet.getLong("utilisateur_id"));
        depense.setCompteId(resultSet.getLong("compte_id"));
        depense.setCategorieId(resultSet.getLong("categorie_id"));
        depense.setMontant(resultSet.getBigDecimal("montant"));
        depense.setLibelle(resultSet.getString("libelle"));
        depense.setFrequence(DepenseRecurrente.Frequence.valueOf(resultSet.getString("frequence")));
        LocalDate prochaineDate = resultSet.getDate("prochaine_date") != null ? resultSet.getDate("prochaine_date").toLocalDate() : null;
        depense.setProchaineDate(prochaineDate);
        depense.setActive(resultSet.getBoolean("active"));
        return depense;
    }
}
