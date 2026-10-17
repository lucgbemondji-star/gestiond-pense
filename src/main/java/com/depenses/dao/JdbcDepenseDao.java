package com.depenses.dao;

import com.depenses.model.Depense;
import com.depenses.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDepenseDao implements DepenseDao {
    private static final String INSERT_SQL = "INSERT INTO depense (utilisateur_id, compte_id, categorie_id, montant, devise, montant_xof, date_depense, libelle, note, recurrente_id, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
    private static final String SELECT_BY_ID = "SELECT id, utilisateur_id, compte_id, categorie_id, montant, devise, montant_xof, date_depense, libelle, note, recurrente_id, date_creation FROM depense WHERE id = ?";
    private static final String SELECT_BY_UTILISATEUR = "SELECT id, utilisateur_id, compte_id, categorie_id, montant, devise, montant_xof, date_depense, libelle, note, recurrente_id, date_creation FROM depense WHERE utilisateur_id = ? ORDER BY date_depense DESC, id DESC";
    private static final String UPDATE_SQL = "UPDATE depense SET compte_id = ?, categorie_id = ?, montant = ?, devise = ?, montant_xof = ?, date_depense = ?, libelle = ?, note = ?, recurrente_id = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM depense WHERE id = ?";

    @Override
    public long creer(Depense depense) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, depense.getUtilisateurId());
            statement.setLong(2, depense.getCompteId());
            statement.setLong(3, depense.getCategorieId());
            statement.setBigDecimal(4, depense.getMontant());
            statement.setString(5, depense.getDevise());
            statement.setBigDecimal(6, depense.getMontantXof());
            statement.setDate(7, Date.valueOf(depense.getDateDepense()));
            statement.setString(8, depense.getLibelle());
            statement.setString(9, depense.getNote());
            if (depense.getRecurrenteId() != null) {
                statement.setLong(10, depense.getRecurrenteId());
            } else {
                statement.setNull(10, Types.BIGINT);
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return 0L;
        } catch (SQLException e) {
            throw new DaoException("Impossible de créer la dépense.", e);
        }
    }

    @Override
    public List<Depense> creerPlusieurs(List<Depense> depenses) {
        try (Connection connexion = DbUtil.getConnection()) {
            connexion.setAutoCommit(false);
            try {
                for (Depense depense : depenses) {
                    try (PreparedStatement statement = connexion.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setLong(1, depense.getUtilisateurId());
                        statement.setLong(2, depense.getCompteId());
                        statement.setLong(3, depense.getCategorieId());
                        statement.setBigDecimal(4, depense.getMontant());
                        statement.setString(5, depense.getDevise());
                        statement.setBigDecimal(6, depense.getMontantXof());
                        statement.setDate(7, Date.valueOf(depense.getDateDepense()));
                        statement.setString(8, depense.getLibelle());
                        statement.setString(9, depense.getNote());
                        if (depense.getRecurrenteId() != null) {
                            statement.setLong(10, depense.getRecurrenteId());
                        } else {
                            statement.setNull(10, Types.BIGINT);
                        }
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("La base n'a pas renvoyé l'identifiant de la dépense importée.");
                            }
                            depense.setId(keys.getLong(1));
                        }
                    }
                }
                connexion.commit();
                return depenses;
            } catch (SQLException | RuntimeException e) {
                try {
                    connexion.rollback();
                } catch (SQLException erreurAnnulation) {
                    e.addSuppressed(erreurAnnulation);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible d'importer les dépenses de manière atomique.", e);
        }
    }

    @Override
    public Optional<Depense> trouverParId(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de retrouver la dépense par id.", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Depense> listerParUtilisateur(Long utilisateurId) {
        List<Depense> depenses = new ArrayList<>();
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(SELECT_BY_UTILISATEUR)) {
            statement.setLong(1, utilisateurId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    depenses.add(mapper(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les dépenses.", e);
        }
        return depenses;
    }

    @Override
    public boolean modifier(Depense depense) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, depense.getCompteId());
            statement.setLong(2, depense.getCategorieId());
            statement.setBigDecimal(3, depense.getMontant());
            statement.setString(4, depense.getDevise());
            statement.setBigDecimal(5, depense.getMontantXof());
            statement.setDate(6, Date.valueOf(depense.getDateDepense()));
            statement.setString(7, depense.getLibelle());
            statement.setString(8, depense.getNote());
            if (depense.getRecurrenteId() != null) {
                statement.setLong(9, depense.getRecurrenteId());
            } else {
                statement.setNull(9, Types.BIGINT);
            }
            statement.setLong(10, depense.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la dépense.", e);
        }
    }

    @Override
    public boolean supprimer(Long id) {
        try (Connection connexion = DbUtil.getConnection();
             PreparedStatement statement = connexion.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la dépense.", e);
        }
    }

    private Depense mapper(ResultSet resultSet) throws SQLException {
        Depense depense = new Depense();
        depense.setId(resultSet.getLong("id"));
        depense.setUtilisateurId(resultSet.getLong("utilisateur_id"));
        depense.setCompteId(resultSet.getLong("compte_id"));
        depense.setCategorieId(resultSet.getLong("categorie_id"));
        depense.setMontant(resultSet.getBigDecimal("montant"));
        depense.setDevise(resultSet.getString("devise"));
        depense.setMontantXof(resultSet.getBigDecimal("montant_xof"));
        Date dateDepense = resultSet.getDate("date_depense");
        depense.setDateDepense(dateDepense != null ? dateDepense.toLocalDate() : null);
        depense.setLibelle(resultSet.getString("libelle"));
        depense.setNote(resultSet.getString("note"));
        long recurrenteId = resultSet.getLong("recurrente_id");
        depense.setRecurrenteId(resultSet.wasNull() ? null : recurrenteId);
        Timestamp timestamp = resultSet.getTimestamp("date_creation");
        depense.setDateCreation(timestamp != null ? timestamp.toLocalDateTime() : null);
        return depense;
    }
}
