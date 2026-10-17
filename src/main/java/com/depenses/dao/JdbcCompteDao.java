package com.depenses.dao;

import com.depenses.model.Compte;
import com.depenses.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCompteDao implements CompteDao {
    private static final String INSERT_SQL = "INSERT INTO compte (utilisateur_id, nom, type, devise, solde_initial) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, nom, type, devise, solde_initial FROM compte WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, nom, type, devise, solde_initial FROM compte WHERE utilisateur_id = ? ORDER BY id";
    private static final String UPDATE_SQL = "UPDATE compte SET nom = ?, type = ?, devise = ?, solde_initial = ? WHERE id = ? AND utilisateur_id = ?";
    private static final String DELETE_SQL = "DELETE FROM compte WHERE id = ?";

    @Override
    public long creer(Compte compte) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, compte.getUtilisateurId());
            statement.setString(2, compte.getNom());
            statement.setString(3, compte.getType().name());
            statement.setString(4, compte.getDevise());
            statement.setBigDecimal(5, compte.getSoldeInitial() == null ? BigDecimal.ZERO : compte.getSoldeInitial());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer le compte.", e);
        }
    }

    @Override
    public Optional<Compte> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver le compte par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Compte> listerParUtilisateur(Long utilisateurId) {
        List<Compte> comptes = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_UTILISATEUR)) {
            statement.setLong(1, utilisateurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    comptes.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les comptes.", e);
        }
        return comptes;
    }

    @Override
    public boolean modifier(Compte compte) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, compte.getNom());
            statement.setString(2, compte.getType().name());
            statement.setString(3, compte.getDevise());
            statement.setBigDecimal(4, compte.getSoldeInitial() == null ? BigDecimal.ZERO : compte.getSoldeInitial());
            statement.setLong(5, compte.getId());
            statement.setLong(6, compte.getUtilisateurId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier le compte.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer le compte.", e);
        }
    }

    private Compte mapper(ResultSet resultSet) throws SQLException {
        Compte compte = new Compte();
        compte.setId(resultSet.getLong("id"));
        compte.setUtilisateurId(resultSet.getLong("utilisateur_id"));
        compte.setNom(resultSet.getString("nom"));
        compte.setType(Compte.TypeCompte.valueOf(resultSet.getString("type")));
        compte.setDevise(resultSet.getString("devise"));
        compte.setSoldeInitial(resultSet.getBigDecimal("solde_initial"));
        return compte;
    }
}
