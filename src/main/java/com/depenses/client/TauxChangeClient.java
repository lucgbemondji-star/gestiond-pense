package com.depenses.client;

import java.math.BigDecimal;
import java.util.Map;

public interface TauxChangeClient {
    Map<String, BigDecimal> chargerTaux(String deviseBase);
}
