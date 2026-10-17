import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyConverter {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Pattern RESULT_PATTERN = Pattern.compile("\"result\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BASE_PATTERN = Pattern.compile("\"base_code\"\\s*:\\s*\"([^\"]+)\"");

    public static double convertToLocalCurrency(double amount, String fromCurrency) {
        String base = fromCurrency.toUpperCase(Locale.ROOT);
        if (base.equals("XOF")) return amount;

        if (!Expense.isValidCurrency(base)) {
            throw new IllegalArgumentException("Devise non prise en charge : " + base);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://open.er-api.com/v6/latest/" + base))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ConversionException("L'API de change a répondu avec le statut HTTP " + response.statusCode() + ".");
            }

            String body = response.body();
            if (!"success".equals(extract(RESULT_PATTERN, body, "résultat"))) {
                throw new ConversionException("L'API de change a refusé la demande.");
            }
            if (!base.equals(extract(BASE_PATTERN, body, "devise de base"))) {
                throw new ConversionException("L'API de change a renvoyé une devise de base inattendue.");
            }

            double rate = extractRate(body, "XOF");
            double converted = amount * rate;
            if (!Double.isFinite(converted)) {
                throw new ConversionException("Le montant converti est hors limites.");
            }
            return converted;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConversionException("La demande de conversion a été interrompue.", e);
        } catch (IOException e) {
            throw new ConversionException("Impossible de joindre l'API de change : " + e.getMessage(), e);
        }
    }

    private static String extract(Pattern pattern, String json, String field) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new ConversionException("Réponse invalide de l'API de change : champ " + field + " absent.");
        }
        return matcher.group(1);
    }

    private static double extractRate(String json, String currency) {
        Pattern ratePattern = Pattern.compile("\"rates\"\\s*:\\s*\\{[^}]*\"" + Pattern.quote(currency)
                + "\"\\s*:\\s*(-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)");
        Matcher matcher = ratePattern.matcher(json);
        if (!matcher.find()) {
            throw new ConversionException("L'API de change ne fournit pas de taux pour " + currency + ".");
        }
        double rate = Double.parseDouble(matcher.group(1));
        if (!Double.isFinite(rate) || rate <= 0) {
            throw new ConversionException("L'API de change a renvoyé un taux invalide pour " + currency + ".");
        }
        return rate;
    }

    public static final class ConversionException extends RuntimeException {
        public ConversionException(String message) {
            super(message);
        }

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}