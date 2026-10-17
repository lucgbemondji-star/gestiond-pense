package com.depenses.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DbUtil {
    private static final Object LOCK = new Object();
    private static HikariDataSource dataSource;

    private DbUtil() {
    }

    public static HikariDataSource getDataSource() {
        synchronized (LOCK) {
            if (dataSource == null) {
                Properties properties = chargerProprietes();
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(properties.getProperty("db.url"));
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setUsername(properties.getProperty("db.username"));
                config.setPassword(properties.getProperty("db.password"));
                config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("db.pool.maximumPoolSize", "10")));
                config.setMinimumIdle(Integer.parseInt(properties.getProperty("db.pool.minimumIdle", "2")));
                config.setPoolName("depenses-pool");
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setAutoCommit(true);
                dataSource = new HikariDataSource(config);
            }
            return dataSource;
        }
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void fermer() {
        synchronized (LOCK) {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        }
    }

    public static Properties chargerProprietes() {
        Properties properties = ConfigUtil.getProperties();
        if (estVide(properties.getProperty("db.url"))) {
            throw new IllegalStateException("Aucune URL de base de données configurée. Définissez DB_URL ou les variables MySQL, ou créez config.properties.");
        }
        if (estVide(properties.getProperty("db.username")) || estVide(properties.getProperty("db.password"))) {
            throw new IllegalStateException("Les identifiants de la base de données sont absents. Définissez MYSQLUSER et MYSQLPASSWORD ou leurs équivalents DB_USERNAME et DB_PASSWORD.");
        }
        return properties;
    }

    private static boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
