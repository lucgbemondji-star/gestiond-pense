package com.depenses.service;

import com.depenses.client.TauxChangeClient;
import com.depenses.util.ConfigUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TauxChangeService {
    private final TauxChangeClient client;
    private final Map<String, BigDecimal> cacheTaux = new ConcurrentHashMap<>();
    private volatile long tempsCacheExpire = 0L;

    public TauxChangeService(TauxChangeClient client) {
        this.client = client;
    }

    public BigDecimal convertirEnXof(String devise, BigDecimal montant) {
        if (montant == null) {
            throw new IllegalArgumentException("Le montant à convertir est obligatoire.");
        }
        if (devise == null || devise.isBlank() || "XOF".equalsIgnoreCase(devise)) {
            return montant.setScale(2, RoundingMode.HALF_UP);
        }
        if (montant.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal taux = obtenirTaux(devise);
        return montant.multiply(taux).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal obtenirTaux(String devise) {
        if (devise == null || devise.isBlank() || "XOF".equalsIgnoreCase(devise)) {
            return BigDecimal.ONE;
        }

        String code = devise.trim().toUpperCase();
        long maintenant = System.currentTimeMillis();
        if (maintenant < tempsCacheExpire && cacheTaux.containsKey(code)) {
            return cacheTaux.get(code);
        }

        Map<String, BigDecimal> taux = client.chargerTaux("XOF");
        if (taux == null || taux.isEmpty()) {
            BigDecimal tauxSecours = new BigDecimal(ConfigUtil.getString("api.tauxChange.fallbackXof", "1.00"));
            cacheTaux.put(code, tauxSecours);
            tempsCacheExpire = maintenant + 3600000L;
            return tauxSecours;
        }

        BigDecimal tauxValeur = taux.get(code);
        if (tauxValeur == null || tauxValeur.signum() <= 0) {
            BigDecimal tauxSecours = new BigDecimal(ConfigUtil.getString("api.tauxChange.fallbackXof", "1.00"));
            cacheTaux.put(code, tauxSecours);
            tempsCacheExpire = maintenant + 3600000L;
            return tauxSecours;
        }

        cacheTaux.put(code, tauxValeur);
        tempsCacheExpire = maintenant + 3600000L;
        return tauxValeur;
    }
}
