package com.depenses.service;

import com.depenses.client.TauxChangeClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

class TauxChangeServiceTest {
    @Test
    void convertitUneDeviseVersXofAvecLeTauxActuel() {
        TauxChangeClient client = new TauxChangeClient() {
            @Override
            public Map<String, BigDecimal> chargerTaux(String deviseBase) {
                return Map.of("USD", new BigDecimal("650.00"));
            }
        };

        TauxChangeService service = new TauxChangeService(client);

        BigDecimal resultat = service.convertirEnXof("USD", new BigDecimal("2.00"));

        Assertions.assertEquals(new BigDecimal("1300.00"), resultat);
    }

    @Test
    void convertitDouzeEurosEnFrancsCfa() {
        TauxChangeClient client = deviseBase -> Map.of("EUR", new BigDecimal("655.957"));
        TauxChangeService service = new TauxChangeService(client);

        BigDecimal resultat = service.convertirEnXof("EUR", new BigDecimal("12.00"));

        Assertions.assertEquals(new BigDecimal("7871.48"), resultat);
    }
}
