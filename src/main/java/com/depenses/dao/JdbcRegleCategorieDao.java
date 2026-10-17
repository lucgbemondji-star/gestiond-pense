package com.depenses.dao;

import com.depenses.model.RegleCategorie;
import com.depenses.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRegleCategorieDao implements RegleCategorieDao {
    private static final String INSERT_SQL = "INSERT INTO regle_categorie (utilisateur_id, mot_cle, categorie_id, poids) VALUES (?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, mot_cle, categorie_id, poids FROM regle_categorie WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, mot_cle, categorie_id, poids FROM regle_categorie WHERE utilisateur_id = ? ORDER BY poids DESC, mot_cle";
    private static final String UPDATE_SQL = "UPDATE regle_categorie SET mot_cle = ?, categorie_id = ?, poids = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM regle_categorie WHERE id = ?";

    @Override
    public long creer(RegleCategorie regleCategorie) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, regleCategorie.getUtilisateurId());
            statement.setString(2, regleCategorie.getMotCle());
            statement.setLong(3, regleCategorie.getCategorieId());
            statement.setInt(4, regleCategorie.getPoids() == null ? 1 : regleCategorie.getPoids());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer la règle de catégorie.", e);
        }
    }

    @Override
    public Optional<RegleCategorie> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver la règle de catégorie.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<RegleCategorie> listerParUtilisateur(Long utilisateurId) {
        List<RegleCategorie> regles = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_UTILISATEUR)) {
            statement.setLong(1, utilisateurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    regles.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les règles de catégorie.", e);
        }
        return regles;
    }

    @Override
    public boolean modifier(RegleCategorie regleCategorie) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, regleCategorie.getMotCle());
            statement.setLong(2, regleCategorie.getCategorieId());
            statement.setInt(3, regleCategorie.getPoids() == null ? 1 : regleCategorie.getPoids());
            statement.setLong(4, regleCategorie.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la règle de catégorie.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la règle de catégorie.", e);
        }
    }

    private RegleCategorie mapper(ResultSet resultSet) throws SQLException {
        RegleCategorie regle = new RegleCategorie();
        regle.setId(resultSet.getLong("id"));
        regle.setUtilisateurId(resultSet.getLong("utilisateur_id"));
        regle.setMotCle(resultSet.getString("mot_cle"));
        regle.setCategorieId(resultSet.getLong("categorie_id"));
        regle.setPoids(resultSet.getInt("poids"));
        return regle;
    }
}
