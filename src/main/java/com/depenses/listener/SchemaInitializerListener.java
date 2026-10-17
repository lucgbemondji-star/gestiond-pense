package com.depenses.listener;

import com.depenses.util.ConfigUtil;
import com.depenses.util.DbUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaInitializerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent evenement) {
        if (!ConfigUtil.getBoolean("db.initializeSchema", false)) {
            return;
        }

        String schema = lireSchema();
        try (Connection connexion = DbUtil.getConnection()) {
            for (String commande : schema.split(";")) {
                String sql = commande.trim();
                if (sql.isEmpty() || sql.regionMatches(true, 0, "CREATE DATABASE", 0, "CREATE DATABASE".length())
                        || sql.matches("(?is)^USE\\s+.+$")) {
                    continue;
                }
                try (Statement statement = connexion.createStatement()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("L'initialisation du schéma de la base de données a échoué.", e);
        }
    }

    private String lireSchema() {
        try (InputStream flux = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (flux == null) {
                throw new IllegalStateException("Le fichier schema.sql est absent de l'application.");
            }
            try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8))) {
                return lecteur.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le schéma de la base de données.", e);
        }
    }
}
