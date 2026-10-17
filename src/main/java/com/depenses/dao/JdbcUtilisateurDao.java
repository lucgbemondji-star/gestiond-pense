package com.depenses.dao;

import com.depenses.model.Utilisateur;
import com.depenses.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUtilisateurDao implements UtilisateurDao {
    private static final String INSERT_SQL =
            "INSERT INTO utilisateur (nom, email, mot_de_passe_hash, role, date_creation) VALUES (?, ?, ?, ?, NOW())";
    private static final String SELECT_BY_ID =
            "SELECT id, nom, email, mot_de_passe_hash, role, date_creation FROM utilisateur WHERE id = ?";
    private static final String SELECT_BY_EMAIL =
            "SELECT id, nom, email, mot_de_passe_hash, role, date_creation FROM utilisateur WHERE email = ?";
    private static final String SELECT_ALL =
            "SELECT id, nom, email, mot_de_passe_hash, role, date_creation FROM utilisateur ORDER BY id";
    private static final String UPDATE_SQL =
            "UPDATE utilisateur SET nom = ?, email = ?, mot_de_passe_hash = ?, role = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM utilisateur WHERE id = ?";

    @Override
    public long creer(Utilisateur utilisateur) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, utilisateur.getNom());
            statement.setString(2, utilisateur.getEmail());
            statement.setString(3, utilisateur.getMotDePasseHash());
            statement.setString(4, utilisateur.getRole() != null ? utilisateur.getRole().name() : Utilisateur.RoleUtilisateur.USER.name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer l'utilisateur.", e);
        }
    }

    @Override
    public Optional<Utilisateur> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver l'utilisateur par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Utilisateur> trouverParEmail(String email) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver l'utilisateur par email.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Utilisateur> lister() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                utilisateurs.add(mapper(resultSet));
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les utilisateurs.", e);
        }
        return utilisateurs;
    }

    @Override
    public boolean modifier(Utilisateur utilisateur) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, utilisateur.getNom());
            statement.setString(2, utilisateur.getEmail());
            statement.setString(3, utilisateur.getMotDePasseHash());
            statement.setString(4, utilisateur.getRole().name());
            statement.setLong(5, utilisateur.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier l'utilisateur.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer l'utilisateur.", e);
        }
    }

    private Utilisateur mapper(ResultSet resultSet) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(resultSet.getLong("id"));
        utilisateur.setNom(resultSet.getString("nom"));
        utilisateur.setEmail(resultSet.getString("email"));
        utilisateur.setMotDePasseHash(resultSet.getString("mot_de_passe_hash"));
        String role = resultSet.getString("role");
        utilisateur.setRole(role == null ? Utilisateur.RoleUtilisateur.USER : Utilisateur.RoleUtilisateur.valueOf(role));
        Timestamp timestamp = resultSet.getTimestamp("date_creation");
        utilisateur.setDateCreation(timestamp != null ? timestamp.toLocalDateTime() : null);
        return utilisateur;
    }
}
