package com.depenses.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Properties properties = new Properties();

        String cheminConfig = System.getenv("DEPENSES_CONFIG");
        if (cheminConfig != null && !cheminConfig.isBlank()) {
            chargerDepuisFichier(properties, Path.of(cheminConfig));
            return properties;
        }

        Path[] chemins = {
                Path.of("config.properties"),
                Path.of(System.getProperty("user.home"), "gestionnaire-depenses.properties")
        };

        for (Path chemin : chemins) {
            if (chargerDepuisFichier(properties, chemin)) {
                return properties;
            }
        }

        try (InputStream stream = DbUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Le fichier de configuration est introuvable. Copiez config.properties.example en config.properties.", e);
        }

        if (properties.isEmpty()) {
            throw new IllegalStateException("Aucune configuration détectée pour la base de données. Vérifiez la présence du fichier config.properties.");
        }

        return properties;
    }

    private static boolean chargerDepuisFichier(Properties properties, Path chemin) {
        if (chemin == null || !Files.exists(chemin)) {
            return false;
        }

        try (InputStream inputStream = Files.newInputStream(chemin)) {
            properties.load(inputStream);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier de configuration : " + chemin, e);
        }
    }
}
