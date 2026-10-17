package com.depenses.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigUtilTest {
    @Test
    void construitUrlMysqlDepuisVariablesRailway() {
        Properties proprietes = new Properties();
        Map<String, String> environnement = Map.of(
                "MYSQLHOST", "mysql.railway.internal",
                "MYSQLPORT", "3307",
                "MYSQLDATABASE", "gestion_depenses",
                "MYSQLUSER", "demo",
                "MYSQLPASSWORD", "secret-test"
        );

        ConfigUtil.appliquerVariablesEnvironnement(proprietes, environnement);

        assertEquals("jdbc:mysql://mysql.railway.internal:3307/gestion_depenses"
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                proprietes.getProperty("db.url"));
        assertEquals("demo", proprietes.getProperty("db.username"));
        assertEquals("secret-test", proprietes.getProperty("db.password"));
    }

    @Test
    void variablesDenvironnementRemplacentLesValeursDuFichier() {
        Properties proprietes = new Properties();
        proprietes.setProperty("db.url", "jdbc:mysql://localhost/locale");
        proprietes.setProperty("db.username", "ancien");
        Map<String, String> environnement = Map.of(
                "DB_URL", "jdbc:mysql://mysql.railway.internal/demo",
                "DB_USER", "heberge",
                "DB_PASSWORD", "secret-heberge"
        );

        ConfigUtil.appliquerVariablesEnvironnement(proprietes, environnement);

        assertEquals("jdbc:mysql://mysql.railway.internal/demo", proprietes.getProperty("db.url"));
        assertEquals("heberge", proprietes.getProperty("db.username"));
        assertEquals("secret-heberge", proprietes.getProperty("db.password"));
    }

    @Test
    void refuseUneConfigurationMySqlPartielle() {
        assertThrows(IllegalStateException.class, () -> ConfigUtil.appliquerVariablesEnvironnement(
                new Properties(), Map.of("MYSQLHOST", "mysql.railway.internal")));
    }

    @Test
    void refuseUnPortMySqlInvalide() {
        Map<String, String> environnement = new HashMap<>();
        environnement.put("MYSQLHOST", "mysql.railway.internal");
        environnement.put("MYSQLDATABASE", "gestion_depenses");
        environnement.put("MYSQLPORT", "70000");

        assertThrows(IllegalStateException.class,
                () -> ConfigUtil.appliquerVariablesEnvironnement(new Properties(), environnement));
    }
}
