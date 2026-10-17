package com.depenses.service;

import com.depenses.util.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportServiceTest {
    @TempDir
    Path dossierTemporaire;

    private final ImportService service = new ImportService();

    @Test
    void litUnCsvDelimiteParPointVirguleAvecTexteEntreGuillemets() throws Exception {
        Path fichier = dossierTemporaire.resolve("depenses.csv");
        Files.writeString(fichier,
                "date;libellé;montant;devise\n2025-03-01;\"Café; marché\";1250;XOF",
                StandardCharsets.UTF_8);

        List<String[]> lignes = service.apercuCsv(fichier);

        assertEquals(2, lignes.size());
        assertArrayEquals(new String[]{"2025-03-01", "Café; marché", "1250", "XOF"}, lignes.get(1));
    }

    @Test
    void refuseLesGuillemetsNonFermes() throws Exception {
        Path fichier = dossierTemporaire.resolve("invalide.csv");
        Files.writeString(fichier, "date,libellé,montant,devise\n2025-03-01,\"repas,1250,XOF",
                StandardCharsets.UTF_8);

        assertThrows(ValidationException.class, () -> service.apercuCsv(fichier));
    }
}
