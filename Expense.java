import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class Expense {
    private final String id;
    private String title;
    private double amount;
    private String currency; // ex: EUR, USD
    private double convertedAmount; // en XOF / Franc CFA par exemple
    private String date;
    private final String createdAt;

    public Expense(String id, String title, double amount, String currency, double convertedAmount, String date, String createdAt) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.convertedAmount = convertedAmount;
        this.date = date;
        this.createdAt = createdAt != null ? createdAt : Instant.now().toString();
    }

    public static boolean isValidCurrency(String currency) {
        return currency != null && List.of("EUR", "USD", "XOF", "GBP").contains(currency.toUpperCase(Locale.ROOT));
    }

    public String toJson() {
        return String.format(Locale.ROOT,
            "{\"id\":%s,\"title\":%s,\"amount\":%.2f,\"currency\":%s,\"convertedAmount\":%.2f,\"date\":%s,\"createdAt\":%s}",
            json(id), json(title), amount, json(currency), convertedAmount, json(date), json(createdAt)
        );
    }

    public String toStorageLine() {
        return String.join("\t", encode(id), encode(title), encode(String.valueOf(amount)),
                encode(currency), encode(String.valueOf(convertedAmount)), encode(date), encode(createdAt));
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String json(String value) {
        if (value == null) return "null";
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    // Getters et Setters nécessaires pour les modifications (PUT)
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public double getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(double convertedAmount) { this.convertedAmount = convertedAmount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}