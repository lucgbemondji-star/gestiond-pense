package com.depenses.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class ConfigUtil {
    private static final Properties PROPRIETES = charger();

    private ConfigUtil() {
    }

    public static String getString(String cle, String defaut) {
        return PROPRIETES.getProperty(cle, defaut);
    }

    public static int getInt(String cle, int defaut) {
        String valeur = PROPRIETES.getProperty(cle);
        if (valeur == null || valeur.isBlank()) {
            return defaut;
        }
        try {
            return Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valeur entière invalide pour la clé de configuration : " + cle, e);
        }
    }

    public static boolean getBoolean(String cle, boolean defaut) {
        String valeur = PROPRIETES.getProperty(cle);
        if (valeur == null || valeur.isBlank()) {
            return defaut;
        }
        return Boolean.parseBoolean(valeur.trim());
    }

    public static Properties getProperties() {
        Properties copie = new Properties();
        copie.putAll(PROPRIETES);
        return copie;
    }

    private static Properties charger() {
        Properties properties = new Properties();
        String cheminConfig = System.getenv("DEPENSES_CONFIG");
        if (cheminConfig != null && !cheminConfig.isBlank()) {
            chargerDepuisFichier(properties, Path.of(cheminConfig));
        } else {
            Path[] chemins = {
                    Path.of("config.properties"),
                    Path.of(System.getProperty("user.home"), "gestionnaire-depenses.properties")
            };

            for (Path chemin : chemins) {
                if (chargerDepuisFichier(properties, chemin)) {
                    break;
                }
            }
        }

        if (properties.isEmpty()) {
            try (InputStream stream = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (stream != null) {
                    properties.load(stream);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Impossible de lire la configuration depuis les ressources de l'application.", e);
            }
        }

        appliquerVariablesEnvironnement(properties, System.getenv());
        return properties;
    }

    static void appliquerVariablesEnvironnement(Properties properties, Map<String, String> environnement) {
        String url = premiereVariableEnvironnement(environnement, "DB_URL");
        String hote = premiereVariableEnvironnement(environnement, "MYSQLHOST", "DB_HOST");
        String base = premiereVariableEnvironnement(environnement, "MYSQLDATABASE", "DB_NAME");
        if (url != null) {
            properties.setProperty("db.url", url.trim());
        } else if (hote != null || base != null) {
            if (hote == null || base == null) {
                throw new IllegalStateException("Les variables MYSQLHOST et MYSQLDATABASE doivent être définies ensemble.");
            }
            hote = hote.trim();
            base = base.trim();
            if (!hote.matches("[A-Za-z0-9.-]+")) {
                throw new IllegalStateException("L'hôte MySQL fourni par l'environnement est invalide.");
            }
            String port = premiereVariableEnvironnement(environnement, "MYSQLPORT", "DB_PORT");
            if (port == null) {
                port = "3306";
            } else {
                port = port.trim();
            }
            if (!port.matches("[0-9]{1,5}") || Integer.parseInt(port) > 65535 || Integer.parseInt(port) < 1) {
                throw new IllegalStateException("La variable MYSQLPORT doit contenir un port valide.");
            }
            if (!base.matches("[A-Za-z0-9_$-]+")) {
                throw new IllegalStateException("Le nom de la base de données fourni par l'environnement est invalide.");
            }
            properties.setProperty("db.url", "jdbc:mysql://" + hote + ":" + port + "/" + base
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        }

        definirVariable(properties, environnement, "db.username", "MYSQLUSER", "DB_USERNAME", "DB_USER");
        definirVariable(properties, environnement, "db.password", "MYSQLPASSWORD", "DB_PASSWORD");
        definirVariable(properties, environnement, "db.initializeSchema", "DB_INITIALIZE_SCHEMA");
    }

    private static void definirVariable(Properties properties, Map<String, String> environnement,
                                        String cle, String... noms) {
        String valeur = premiereVariableEnvironnement(environnement, noms);
        if (valeur != null) {
            properties.setProperty(cle, "db.password".equals(cle) ? valeur : valeur.trim());
        }
    }

    private static String premiereVariableEnvironnement(Map<String, String> environnement, String... noms) {
        for (String nom : noms) {
            String valeur = variableEnvironnement(environnement, nom);
            if (valeur != null) {
                return valeur;
            }
        }
        return null;
    }

    private static String variableEnvironnement(Map<String, String> environnement, String nom) {
        String valeur = environnement.get(nom);
        return valeur == null || valeur.isBlank() ? null : valeur;
    }

    private static boolean chargerDepuisFichier(Properties properties, Path chemin) {
        if (chemin == null || !Files.exists(chemin)) {
            return false;
        }
        try (InputStream stream = Files.newInputStream(chemin)) {
            properties.load(stream);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier de configuration : " + chemin, e);
        }
    }
}
