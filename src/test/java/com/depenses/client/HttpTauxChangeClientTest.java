package com.depenses.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class HttpTauxChangeClientTest {
    @Test
    void inverseLesTauxDeLaReponseBaseeSurLeXof() {
        BigDecimal tauxEuroParXof = new BigDecimal("0.0015");
        BigDecimal tauxXofParEuro = HttpTauxChangeClient.convertirEnXofParUnite(tauxEuroParXof);

        Assertions.assertEquals(new BigDecimal("666.6666666666666666666666666666667"), tauxXofParEuro);
    }
}
