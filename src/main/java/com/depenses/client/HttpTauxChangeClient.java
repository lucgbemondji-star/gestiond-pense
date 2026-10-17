package com.depenses.client;

import com.depenses.util.ConfigUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class HttpTauxChangeClient implements TauxChangeClient {
    private final HttpClient httpClient;

    public HttpTauxChangeClient() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
    }

    public HttpTauxChangeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Map<String, BigDecimal> chargerTaux(String deviseBase) {
        String url = ConfigUtil.getString("api.tauxChange.url", "https://open.er-api.com/v6/latest/XOF");
        int timeoutMs = ConfigUtil.getInt("api.tauxChange.timeoutMs", 5000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Erreur HTTP lors du chargement des taux de change : " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String deviseReponse = root.has("base_code") ? root.get("base_code").getAsString() : "";
            if (!"XOF".equalsIgnoreCase(deviseReponse)) {
                throw new IllegalStateException("L'API de change doit renvoyer des taux basés sur le XOF.");
            }
            JsonObject rates = root.getAsJsonObject("rates");
            if (rates == null) {
                throw new IllegalStateException("La réponse de l'API de change ne contient aucun taux.");
            }
            Map<String, BigDecimal> result = new HashMap<>();
            for (String key : rates.keySet()) {
                BigDecimal tauxDeviseParXof = new BigDecimal(rates.get(key).getAsString());
                if (tauxDeviseParXof.signum() <= 0) {
                    throw new IllegalStateException("L'API de change a renvoyé un taux invalide pour " + key + ".");
                }
                result.put(key, convertirEnXofParUnite(tauxDeviseParXof));
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Impossible de récupérer les taux de change depuis l'API externe.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de récupérer les taux de change depuis l'API externe.", e);
        }
    }

    static BigDecimal convertirEnXofParUnite(BigDecimal tauxDeviseParXof) {
        return BigDecimal.ONE.divide(tauxDeviseParXof, MathContext.DECIMAL128);
    }
}
