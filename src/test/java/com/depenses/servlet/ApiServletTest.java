package com.depenses.servlet;

import com.depenses.util.JsonUtil;
import com.depenses.util.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiServletTest {
    private final ServletTestHelper servlet = new ServletTestHelper();

    @Test
    void accepteUnIdentifiantJsonDecodeCommeNombreDecimalEntier() {
        Map<String, Object> payload = JsonUtil.fromJson("{\"categorieId\":5}", Map.class);

        assertEquals(5L, servlet.lireIdentifiant(payload.get("categorieId")));
    }

    @Test
    void rejetteUnIdentifiantFractionnaire() {
        assertThrows(ValidationException.class, () -> servlet.lireIdentifiant(5.5));
    }

    @Test
    void rejetteUnIdentifiantHorsCapacite() {
        assertThrows(ValidationException.class, () -> servlet.lireIdentifiant("9223372036854775808"));
    }

    @Test
    void conserveUnIdentifiantAbsentCommeValeurNulle() {
        assertNull(servlet.lireIdentifiant(null));
    }

    private static final class ServletTestHelper extends ApiServlet {
        private Long lireIdentifiant(Object valeur) {
            return valeurLong(valeur);
        }
    }
}
