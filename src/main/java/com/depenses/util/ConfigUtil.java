package com.depenses.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return new Properties();
    }

    private static Properties charger() {
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

        try (InputStream stream = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire la configuration depuis les ressources de l'application.", e);
        }

        return properties;
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
