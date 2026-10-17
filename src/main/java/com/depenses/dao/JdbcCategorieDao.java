package com.depenses.dao;

import com.depenses.model.Categorie;
import com.depenses.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategorieDao implements CategorieDao {
    private static final String INSERT_SQL = "INSERT INTO categorie (utilisateur_id, nom, parent_id) VALUES (?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, nom, parent_id FROM categorie WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, nom, parent_id FROM categorie WHERE utilisateur_id = ? ORDER BY nom";
    private static final String SELECT_BY_DEFAUT = "SELECT id, utilisateur_id, nom, parent_id FROM categorie WHERE utilisateur_id IS NULL ORDER BY nom";
    private static final String UPDATE_SQL = "UPDATE categorie SET nom = ?, parent_id = ? WHERE id = ? AND utilisateur_id = ?";
    private static final String DELETE_SQL = "DELETE FROM categorie WHERE id = ?";

    @Override
    public long creer(Categorie categorie) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            if (categorie.getUtilisateurId() != null) {
                statement.setLong(1, categorie.getUtilisateurId());
            } else {
                statement.setNull(1, java.sql.Types.BIGINT);
            }
            statement.setString(2, categorie.getNom());
            if (categorie.getParentId() != null) {
                statement.setLong(3, categorie.getParentId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer la catégorie.", e);
        }
    }

    @Override
    public Optional<Categorie> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver la catégorie par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Categorie> listerParUtilisateur(Long utilisateurId) {
        return lister(SELECT_BY_UTILISATEUR, utilisateurId);
    }

    @Override
    public List<Categorie> listerParDefaut() {
        return lister(SELECT_BY_DEFAUT, null);
    }

    @Override
    public boolean modifier(Categorie categorie) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, categorie.getNom());
            if (categorie.getParentId() != null) {
                statement.setLong(2, categorie.getParentId());
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.setLong(3, categorie.getId());
            statement.setLong(4, categorie.getUtilisateurId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la catégorie.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la catégorie.", e);
        }
    }

    private List<Categorie> lister(String sql, Long utilisateurId) {
        List<Categorie> categories = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(sql)) {
            if (utilisateurId != null) {
                statement.setLong(1, utilisateurId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les catégories.", e);
        }
        return categories;
    }

    private Categorie mapper(ResultSet resultSet) throws SQLException {
        Categorie categorie = new Categorie();
        categorie.setId(resultSet.getLong("id"));
        long utilisateurId = resultSet.getLong("utilisateur_id");
        categorie.setUtilisateurId(resultSet.wasNull() ? null : utilisateurId);
        categorie.setNom(resultSet.getString("nom"));
        long parentId = resultSet.getLong("parent_id");
        categorie.setParentId(resultSet.wasNull() ? null : parentId);
        return categorie;
    }
}
